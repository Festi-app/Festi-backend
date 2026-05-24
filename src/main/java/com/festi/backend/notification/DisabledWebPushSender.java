package com.festi.backend.notification;

public class DisabledWebPushSender implements WebPushSender {

    @Override
    public SendResult send(PushSubscription subscription, PushNotificationPayload payload) {
        return SendResult.failed(null, "Web Push delivery is disabled.", false);
    }
}
