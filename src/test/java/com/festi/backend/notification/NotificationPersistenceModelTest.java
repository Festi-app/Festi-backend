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

        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting, "CALLED", "입장 안내", "부스로 방문해 주세요.", "/icons/called.png", "/waitings");

        assertThat(event.getWaiting()).isEqualTo(waiting);
        assertThat(event.getEventType()).isEqualTo("CALLED");
        assertThat(event.getTitle()).isEqualTo("입장 안내");
        assertThat(event.getUrl()).isEqualTo("/waitings");
    }

    @Test
    void deliveryStoresOneAttemptResultAndEndpointSnapshot() {
        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting(), "CALLED", "title", "body", null, "/waitings");
        PushSubscription subscription = new PushSubscription(user(), "endpoint-1", "key", "auth");
        PushNotificationDelivery delivery = new PushNotificationDelivery(event, subscription);
        OffsetDateTime attemptedAt = OffsetDateTime.of(2026, 5, 25, 1, 0, 0, 0, ZoneOffset.UTC);

        delivery.markFailed(410, "Subscription expired.", attemptedAt);

        assertThat(delivery.getStatus()).isEqualTo(PushNotificationDeliveryStatus.FAILED);
        assertThat(delivery.getEndpoint()).isEqualTo("endpoint-1");
        assertThat(delivery.getResponseStatus()).isEqualTo(410);
        assertThat(delivery.getFailureReason()).isEqualTo("Subscription expired.");
        assertThat(delivery.getAttemptedAt()).isEqualTo(attemptedAt);
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
