package com.festi.backend.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "festi.push.delivery.worker", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class PushDeliverySchedulingConfig {
}
