package com.festi.backend.notification;

import com.festi.backend.waiting.Waiting;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingNotificationService {

    private static final int MAX_DELIVERY_ATTEMPTS = 2;
    private static final String CALLED_EVENT_TYPE = "CALLED";
    private static final String WAITINGS_URL = "/waitings";

    private final WaitingNotificationEventRepository eventRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final PushNotificationDeliveryRepository deliveryRepository;
    private final WebPushSender webPushSender;
    private final PushMessageProperties pushMessageProperties;
    private final Clock clock;

    public void notifyCalled(Waiting waiting) {
        PushMessageProperties.PushMessageTemplate template = pushMessageProperties.called();
        PushNotificationPayload payload = new PushNotificationPayload(
                template.title(), template.body(), template.icon(), WAITINGS_URL);
        WaitingNotificationEvent event = eventRepository.save(new WaitingNotificationEvent(
                waiting, CALLED_EVENT_TYPE, payload.title(), payload.body(), payload.icon(), payload.url()));
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId());
        subscriptions.forEach(subscription -> deliver(event, subscription, payload));
    }

    private void deliver(WaitingNotificationEvent event, PushSubscription subscription, PushNotificationPayload payload) {
        for (int attempt = 0; attempt < MAX_DELIVERY_ATTEMPTS; attempt++) {
            PushNotificationDelivery delivery = new PushNotificationDelivery(event, subscription);
            try {
                WebPushSender.SendResult result = webPushSender.send(subscription, payload);
                if (result.successful()) {
                    delivery.markSent(result.responseStatus(), attemptedAt());
                } else {
                    delivery.markFailed(result.responseStatus(), result.failureReason(), attemptedAt());
                }
                deliveryRepository.save(delivery);
                if (result.successful() || !result.retryable()) {
                    return;
                }
            } catch (Exception exception) {
                boolean interrupted = exception instanceof InterruptedException;
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                delivery.markFailed(null, failureReason(exception), attemptedAt());
                deliveryRepository.save(delivery);
                if (interrupted) {
                    return;
                }
            }
        }
    }

    private OffsetDateTime attemptedAt() {
        return OffsetDateTime.now(clock);
    }

    private String failureReason(Exception exception) {
        return "Web Push transport failed: " + exception.getClass().getSimpleName() + ".";
    }
}
