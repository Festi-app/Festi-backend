package com.festi.backend.waiting;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class WaitingDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsPositivePartySize() {
        assertThat(validator.validate(new WaitingDTO.Request((short) 1))).isEmpty();
    }

    @Test
    void rejectsNonPositivePartySize() {
        assertThat(validator.validate(new WaitingDTO.Request((short) 0))).isNotEmpty();
        assertThat(validator.validate(new WaitingDTO.Request((short) -1))).isNotEmpty();
    }

    @Test
    void requiresManagerOperationRequestValues() {
        assertThat(validator.validate(new WaitingDTO.StatusRequest(null))).isNotEmpty();
        assertThat(validator.validate(new WaitingDTO.OpenStatusRequest(null))).isNotEmpty();
    }
}
