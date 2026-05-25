package com.festi.backend.notification;

public interface WebPushSender {

    SendResult send(PushSubscription subscription, PushNotificationPayload payload) throws Exception;

    record SendResult(
            Integer responseStatus,
            boolean successful,
            String failureReason,
            boolean retryable
    ) {

        public static SendResult sent(int responseStatus) {
            return new SendResult(responseStatus, true, null, false);
        }

        public static SendResult failed(Integer responseStatus, String failureReason, boolean retryable) {
            return new SendResult(responseStatus, false, failureReason, retryable);
        }
    }
}
