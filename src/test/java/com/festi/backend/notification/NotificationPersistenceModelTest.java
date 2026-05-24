package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.waiting.Waiting;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationPersistenceModelTest {

    @Test
    void eventPreservesNotificationPayloadSnapshotForWaiting() {
        Waiting waiting = waiting();
        OffsetDateTime availableAt = OffsetDateTime.of(2026, 5, 25, 0, 0, 0, 0, ZoneOffset.UTC);

        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting, "CALLED", "입장 안내", "부스로 방문해 주세요.", "/icons/called.png", "/waitings",
                availableAt);

        assertThat(event.getWaiting()).isEqualTo(waiting);
        assertThat(event.getEventType()).isEqualTo("CALLED");
        assertThat(event.getTitle()).isEqualTo("입장 안내");
        assertThat(event.getUrl()).isEqualTo("/waitings");
        assertThat(event.getStatus()).isEqualTo(WaitingNotificationEventStatus.PENDING);
        assertThat(event.getAvailableAt()).isEqualTo(availableAt);
        assertThat(event.getAttemptCount()).isZero();
    }

    @Test
    void deliveryStoresOneAttemptResultAndEndpointSnapshot() {
        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting(), "CALLED", "title", "body", null, "/waitings", OffsetDateTime.now());
        PushSubscription subscription = new PushSubscription(user(), "endpoint-1", "key", "auth");
        PushNotificationDelivery delivery = new PushNotificationDelivery(event, subscription);
        OffsetDateTime attemptedAt = OffsetDateTime.of(2026, 5, 25, 1, 0, 0, 0, ZoneOffset.UTC);

        delivery.markFailed(410, "Subscription expired.", false, attemptedAt);

        assertThat(delivery.getStatus()).isEqualTo(PushNotificationDeliveryStatus.FAILED);
        assertThat(delivery.getEndpoint()).isEqualTo("endpoint-1");
        assertThat(delivery.getResponseStatus()).isEqualTo(410);
        assertThat(delivery.getFailureReason()).isEqualTo("Subscription expired.");
        assertThat(delivery.isRetryable()).isFalse();
        assertThat(delivery.getAttemptedAt()).isEqualTo(attemptedAt);
    }

    @Test
    void eventTransitionsFromClaimToRetryAndCompletion() {
        OffsetDateTime initialAt = OffsetDateTime.of(2026, 5, 25, 0, 0, 0, 0, ZoneOffset.UTC);
        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting(), "CALLED", "title", "body", null, "/waitings", initialAt);
        OffsetDateTime claimedAt = initialAt.plusSeconds(1);
        OffsetDateTime retryAt = initialAt.plusSeconds(31);
        OffsetDateTime completedAt = initialAt.plusSeconds(32);

        event.markProcessing(claimedAt);
        assertThat(event.getStatus()).isEqualTo(WaitingNotificationEventStatus.PROCESSING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getProcessingStartedAt()).isEqualTo(claimedAt);

        event.scheduleRetry(retryAt, "Temporary push failure.");
        assertThat(event.getStatus()).isEqualTo(WaitingNotificationEventStatus.RETRY_WAIT);
        assertThat(event.getAvailableAt()).isEqualTo(retryAt);

        event.markProcessing(retryAt);
        event.markCompleted(completedAt);
        assertThat(event.getStatus()).isEqualTo(WaitingNotificationEventStatus.COMPLETED);
        assertThat(event.getProcessedAt()).isEqualTo(completedAt);
        assertThat(event.getAttemptCount()).isEqualTo(2);
    }

    private Waiting waiting() {
        Booth booth = new Booth("Night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        Waiting waiting = new Waiting(booth, user(), (short) 2);
        ReflectionTestUtils.setField(waiting, "id", UUID.randomUUID());
        return waiting;
    }

    private User user() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        return new User(festival, "alice123", "hashed", "Alice", "01012345678");
    }
}
