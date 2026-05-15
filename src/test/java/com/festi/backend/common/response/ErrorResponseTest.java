package com.festi.backend.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void createsResponseFromErrorCode() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, "User not found.");

        assertThat(response.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.message()).isEqualTo("User not found.");
        assertThat(response.details()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }
}
