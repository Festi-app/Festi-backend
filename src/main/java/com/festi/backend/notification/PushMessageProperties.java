package com.festi.backend.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festi.push.messages")
public record PushMessageProperties(
        PushMessageTemplate called
) {

    public record PushMessageTemplate(
            String title,
            String body,
            String icon
    ) {
    }
}
