package com.festi.backend.notification;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingNotificationDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WaitingNotificationDeliveryWorker.class);

    private final WaitingNotificationOutboxCoordinator coordinator;
    private final WebPushSender webPushSender;
    private final PushDeliveryWorkerProperties properties;
    private final Clock clock;

    public boolean processNext() {
        Optional<WaitingNotificationOutboxCoordinator.DispatchTask> optionalTask = coordinator.claimNextAvailable();
        if (optionalTask.isEmpty()) {
            return false;
        }

        WaitingNotificationOutboxCoordinator.DispatchTask task = optionalTask.orElseThrow();
        if (task.subscriptions().isEmpty()) {
            coordinator.markCompleted(task.eventId(), now());
            return true;
        }

        String retryableFailure = null;
        for (PushSubscription subscription : task.subscriptions()) {
            UUID deliveryId = coordinator.startDeliveryAttempt(task.eventId(), subscription);
            try {
                WebPushSender.SendResult result = webPushSender.send(subscription, task.payload());
                if (result.successful()) {
                    coordinator.markDeliverySent(deliveryId, result.responseStatus(), now());
                } else {
                    log.warn("Push delivery failed for event {}: {}", task.eventId(), result.failureReason());
                    coordinator.markDeliveryFailed(
                            deliveryId, result.responseStatus(), result.failureReason(), result.retryable(), now());
                    if (result.retryable()) {
                        retryableFailure = result.failureReason();
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                String failureReason = failureReason(exception);
                log.warn("Push delivery interrupted for event {}: {}", task.eventId(), failureReason);
                coordinator.markDeliveryFailed(deliveryId, null, failureReason, false, now());
                coordinator.markFailed(task.eventId(), now(), failureReason);
                return true;
            } catch (Exception exception) {
                retryableFailure = failureReason(exception);
                log.error("Push delivery transport error for event {}", task.eventId(), exception);
                coordinator.markDeliveryFailed(deliveryId, null, retryableFailure, true, now());
            }
        }

        if (retryableFailure == null) {
            coordinator.markCompleted(task.eventId(), now());
        } else if (task.attemptCount() < properties.maxAttempts()) {
            coordinator.scheduleRetry(
                    task.eventId(), now().plusSeconds(properties.retryDelaySeconds()), retryableFailure);
        } else {
            coordinator.markFailed(task.eventId(), now(), retryableFailure);
        }
        return true;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private String failureReason(Exception exception) {
        return "Web Push transport failed: " + exception.getClass().getSimpleName() + ".";
    }
}
