package com.festi.backend.notification;

import com.festi.backend.common.exception.NotFoundException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitingNotificationOutboxCoordinator {

    private final WaitingNotificationEventRepository eventRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final PushNotificationDeliveryRepository deliveryRepository;
    private final PushDeliveryWorkerProperties properties;
    private final Clock clock;

    @Transactional
    public Optional<DispatchTask> claimNextAvailable() {
        OffsetDateTime now = now();
        eventRepository.requeueExpiredProcessing(now.minusSeconds(properties.processingTimeoutSeconds()), now);
        return eventRepository.findNextAvailableForUpdate(now).map(event -> {
            event.markProcessing(now);
            List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndFestivalId(
                            event.getWaiting().getUser().getId(), event.getWaiting().getUser().getFestivalId()).stream()
                    .filter(subscription -> shouldAttempt(event.getId(), subscription.getEndpoint()))
                    .toList();
            PushNotificationPayload payload = new PushNotificationPayload(
                    event.getTitle(), event.getBody(), event.getIcon(), event.getUrl());
            return new DispatchTask(event.getId(), event.getAttemptCount(), payload, subscriptions);
        });
    }

    @Transactional
    public UUID startDeliveryAttempt(UUID eventId, PushSubscription subscription) {
        WaitingNotificationEvent event = findEvent(eventId);
        PushNotificationDelivery delivery = deliveryRepository.save(new PushNotificationDelivery(event, subscription));
        return delivery.getId();
    }

    @Transactional
    public void markDeliverySent(UUID deliveryId, int responseStatus, OffsetDateTime attemptedAt) {
        findDelivery(deliveryId).markSent(responseStatus, attemptedAt);
    }

    @Transactional
    public void markDeliveryFailed(UUID deliveryId, Integer responseStatus, String failureReason,
                                   boolean retryable, OffsetDateTime attemptedAt) {
        findDelivery(deliveryId).markFailed(responseStatus, failureReason, retryable, attemptedAt);
    }

    @Transactional
    public void markCompleted(UUID eventId, OffsetDateTime processedAt) {
        findEvent(eventId).markCompleted(processedAt);
    }

    @Transactional
    public void scheduleRetry(UUID eventId, OffsetDateTime availableAt, String failureReason) {
        findEvent(eventId).scheduleRetry(availableAt, failureReason);
    }

    @Transactional
    public void markFailed(UUID eventId, OffsetDateTime processedAt, String failureReason) {
        findEvent(eventId).markFailed(processedAt, failureReason);
    }

    private boolean shouldAttempt(UUID eventId, String endpoint) {
        return deliveryRepository.findFirstByEventIdAndEndpointOrderByCreatedAtDesc(eventId, endpoint)
                .map(delivery -> delivery.getStatus() == PushNotificationDeliveryStatus.PENDING
                        || delivery.getStatus() == PushNotificationDeliveryStatus.FAILED && delivery.isRetryable())
                .orElse(true);
    }

    private WaitingNotificationEvent findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Notification event not found."));
    }

    private PushNotificationDelivery findDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Notification delivery not found."));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    public record DispatchTask(
            UUID eventId,
            int attemptCount,
            PushNotificationPayload payload,
            List<PushSubscription> subscriptions
    ) {
    }
}
