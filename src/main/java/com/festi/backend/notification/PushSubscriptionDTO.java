package com.festi.backend.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class PushSubscriptionDTO {

    private PushSubscriptionDTO() {
    }

    public record Request(
            @NotBlank @Size(max = 2048) String endpoint,
            @NotNull @Valid Keys keys
    ) {
    }

    public record Keys(
            @NotBlank @Size(max = 255) String p256dh,
            @NotBlank @Size(max = 255) String auth
    ) {
    }

    public record Response(UUID id, String endpoint) {
        public static Response from(PushSubscription subscription) {
            return new Response(subscription.getId(), subscription.getEndpoint());
        }
    }
}
