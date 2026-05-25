package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PushSubscriptionDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsWebPushSubscriptionShape() {
        PushSubscriptionDTO.Request request = new PushSubscriptionDTO.Request(
                "https://push.example.com/subscription/1",
                new PushSubscriptionDTO.Keys("p256dh-value", "auth-value"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingEndpointOrKeys() {
        assertThat(validator.validate(new PushSubscriptionDTO.Request(" ", null))).isNotEmpty();
        assertThat(validator.validate(new PushSubscriptionDTO.Request(
                "https://push.example.com/subscription/1",
                new PushSubscriptionDTO.Keys(" ", " "))))
                .isNotEmpty();
    }
}
