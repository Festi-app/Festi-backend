package com.festi.backend.notification;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import tools.jackson.databind.ObjectMapper;

public class VapidWebPushSender implements WebPushSender {

    private final PushService pushService;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;

    public VapidWebPushSender(PushService pushService, ObjectMapper objectMapper, int ttlSeconds) {
        this.pushService = pushService;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public SendResult send(PushSubscription subscription, PushNotificationPayload payload) throws Exception {
        Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dhKey(),
                subscription.getAuthKey(),
                objectMapper.writeValueAsBytes(payload),
                ttlSeconds);
        HttpResponse response = pushService.send(notification);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return SendResult.sent(statusCode);
        }
        boolean retryable = statusCode == 429 || statusCode >= 500;
        return SendResult.failed(
                statusCode, "Push endpoint responded with HTTP " + statusCode + ".", retryable);
    }
}
