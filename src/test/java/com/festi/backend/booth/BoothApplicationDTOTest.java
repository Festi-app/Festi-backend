package com.festi.backend.booth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class BoothApplicationDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidCreateRequestWithOptionalFieldsOmitted() {
        BoothApplicationDTO.CreateRequest request = new BoothApplicationDTO.CreateRequest(
                "manager1",
                "Password1!",
                "Manager",
                "01012345678",
                "Night Booth",
                BoothType.NIGHT,
                null,
                null
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidCreateRequestFields() {
        BoothApplicationDTO.CreateRequest request = new BoothApplicationDTO.CreateRequest(
                " ",
                "password",
                " ",
                " ",
                " ",
                null,
                null,
                null
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void acceptsBlankRejectMemoBecauseItIsOptional() {
        BoothApplicationDTO.RejectRequest request = new BoothApplicationDTO.RejectRequest(" ");

        assertThat(validator.validate(request)).isEmpty();
    }
}
