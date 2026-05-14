package com.festi.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesFestiExceptionWithMappedStatusAndMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleFestiException(
                new NotFoundException("Booth not found."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Booth not found.");
    }
}
