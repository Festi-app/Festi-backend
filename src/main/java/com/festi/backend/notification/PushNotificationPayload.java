package com.festi.backend.notification;

public record PushNotificationPayload(
        String title,
        String body,
        String icon,
        String url
) {
}
