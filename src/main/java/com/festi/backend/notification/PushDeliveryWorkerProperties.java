package com.festi.backend.notification;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "festi.push.delivery.worker")
public record PushDeliveryWorkerProperties(
        boolean enabled,
        @Min(1) long pollDelayMillis,
        @Min(1) int batchSize,
        @Min(1) int maxAttempts,
        @Min(1) long retryDelaySeconds,
        @Min(1) long processingTimeoutSeconds
) {
}
