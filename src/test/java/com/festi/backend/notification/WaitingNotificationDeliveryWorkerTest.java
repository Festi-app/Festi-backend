package com.festi.backend.notification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitingNotificationDeliveryWorkerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-05-25T00:00:00Z");

    @Mock
    private WaitingNotificationOutboxCoordinator coordinator;

    @Mock
    private WebPushSender webPushSender;

    private WaitingNotificationDeliveryWorker worker;

    @BeforeEach
    void setUp() {
        PushDeliveryWorkerProperties properties = new PushDeliveryWorkerProperties(
                true, 1000, 20, 2, 30, 300);
        Clock clock = Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
        worker = new WaitingNotificationDeliveryWorker(coordinator, webPushSender, properties, clock);
    }

    @Test
    void sendsClaimedEventAndCompletesItWhenDeliverySucceeds() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        PushSubscription subscription = subscription();
        when(coordinator.claimNextAvailable()).thenReturn(Optional.of(task(eventId, 1, subscription)));
        when(coordinator.startDeliveryAttempt(eventId, subscription)).thenReturn(deliveryId);
        when(webPushSender.send(subscription, payload())).thenReturn(WebPushSender.SendResult.sent(201));

        worker.processNext();

        verify(coordinator).markDeliverySent(deliveryId, 201, NOW);
        verify(coordinator).markCompleted(eventId, NOW);
        verify(coordinator, never()).scheduleRetry(eventId, NOW.plusSeconds(30), null);
    }

    @Test
    void schedulesRetryInsteadOfImmediatelyRetryingTransientFailure() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        PushSubscription subscription = subscription();
        when(coordinator.claimNextAvailable()).thenReturn(Optional.of(task(eventId, 1, subscription)));
        when(coordinator.startDeliveryAttempt(eventId, subscription)).thenReturn(deliveryId);
        when(webPushSender.send(subscription, payload()))
                .thenReturn(WebPushSender.SendResult.failed(503, "temporary", true));

        worker.processNext();

        verify(webPushSender).send(subscription, payload());
        verify(coordinator).markDeliveryFailed(deliveryId, 503, "temporary", true, NOW);
        verify(coordinator).scheduleRetry(eventId, NOW.plusSeconds(30), "temporary");
        verify(coordinator, never()).markCompleted(eventId, NOW);
    }

    @Test
    void marksEventFailedWhenTransientFailureExhaustsMaximumAttempts() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        PushSubscription subscription = subscription();
        when(coordinator.claimNextAvailable()).thenReturn(Optional.of(task(eventId, 2, subscription)));
        when(coordinator.startDeliveryAttempt(eventId, subscription)).thenReturn(deliveryId);
        when(webPushSender.send(subscription, payload())).thenThrow(new IOException("connection reset"));

        worker.processNext();

        verify(coordinator).markDeliveryFailed(
                deliveryId, null, "Web Push transport failed: IOException.", true, NOW);
        verify(coordinator).markFailed(eventId, NOW, "Web Push transport failed: IOException.");
        verify(coordinator, never()).scheduleRetry(eventId, NOW.plusSeconds(30), "Web Push transport failed: IOException.");
    }

    @Test
    void completesEventWithoutSendingWhenNoCurrentSubscriptionNeedsDelivery() {
        UUID eventId = UUID.randomUUID();
        when(coordinator.claimNextAvailable()).thenReturn(Optional.of(task(eventId, 1)));

        worker.processNext();

        verify(coordinator).markCompleted(eventId, NOW);
        verifyNoInteractions(webPushSender);
    }

    private WaitingNotificationOutboxCoordinator.DispatchTask task(
            UUID eventId, int attemptCount, PushSubscription... subscriptions) {
        return new WaitingNotificationOutboxCoordinator.DispatchTask(
                eventId, attemptCount, payload(), List.of(subscriptions));
    }

    private PushSubscription subscription() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        User user = new User(festival, "alice123", "hashed", "Alice", "01012345678");
        return new PushSubscription(user, "https://push.example.com/subscription/1", "p256dh", "auth");
    }

    private PushNotificationPayload payload() {
        return new PushNotificationPayload("입장 안내", "부스로 방문해 주세요.", "/icons/called.png", "/waitings");
    }
}
