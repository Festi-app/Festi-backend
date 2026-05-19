package com.festi.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AuthDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidSignupRequest() {
        AuthDTO.SignupRequest request = new AuthDTO.SignupRequest(
                "alice123", "Password1!", "nickname", "01012345678");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsPasswordsThatDoNotMatchPolicy() {
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "Pass1!", "nickname", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "password1!", "nickname", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "PASSWORD1!", "nickname", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "Password!", "nickname", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "Password1", "nickname", "01012345678"))).isNotEmpty();
    }

    @Test
    void rejectsBlankRequiredSignupFields() {
        assertThat(validator.validate(new AuthDTO.SignupRequest(" ", "Password1!", "nickname", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "Password1!", " ", "01012345678"))).isNotEmpty();
        assertThat(validator.validate(new AuthDTO.SignupRequest("alice123", "Password1!", "nickname", " "))).isNotEmpty();
    }

    @Test
    void rejectsIdExceedingMaxLength() {
        String longId = "a".repeat(31);
        assertThat(validator.validate(new AuthDTO.SignupRequest(longId, "Password1!", "nickname", "01012345678"))).isNotEmpty();
    }
}
