package com.festi.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @Test
    void mapsMultipartLimitViolationToPayloadTooLarge() {
        ResponseEntity<Object> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(5 * 1024 * 1024), null, HttpStatus.CONTENT_TOO_LARGE, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(((ErrorResponse) response.getBody()).code()).isEqualTo("PAYLOAD_TOO_LARGE");
    }
}
