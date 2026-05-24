package com.festi.backend.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festi.push.delivery")
public record WebPushProperties(
        boolean enabled,
        String publicKey,
        String privateKey,
        String subject,
        int ttlSeconds
) {
}
