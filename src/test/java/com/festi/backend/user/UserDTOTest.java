package com.festi.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UserDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsPartialProfileUpdate() {
        UserDTO.UpdateRequest request = new UserDTO.UpdateRequest("new-name", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankProfileFieldsWhenTheyArePresent() {
        assertThat(validator.validate(new UserDTO.UpdateRequest(" ", null))).isNotEmpty();
        assertThat(validator.validate(new UserDTO.UpdateRequest(null, " "))).isNotEmpty();
    }

    @Test
    void rejectsEmptyProfilePatch() {
        UserDTO.UpdateRequest request = new UserDTO.UpdateRequest(null, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
