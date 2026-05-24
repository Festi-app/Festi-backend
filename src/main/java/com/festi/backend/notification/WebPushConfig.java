package com.festi.backend.notification;

import java.security.GeneralSecurityException;
import java.security.Security;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {

    @Bean
    WebPushSender webPushSender(WebPushProperties properties, ObjectMapper objectMapper)
            throws GeneralSecurityException {
        if (!properties.enabled()) {
            return new DisabledWebPushSender();
        }
        validateEnabledProperties(properties);
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        PushService pushService = new PushService(
                properties.publicKey(), properties.privateKey(), properties.subject());
        return new VapidWebPushSender(pushService, objectMapper, properties.ttlSeconds());
    }

    private void validateEnabledProperties(WebPushProperties properties) {
        if (isBlank(properties.publicKey()) || isBlank(properties.privateKey()) || isBlank(properties.subject())) {
            throw new IllegalStateException("VAPID public key, private key, and subject are required when Web Push is enabled.");
        }
        if (properties.ttlSeconds() <= 0) {
            throw new IllegalStateException("Web Push TTL must be greater than zero.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
