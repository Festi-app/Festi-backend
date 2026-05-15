package com.festi.backend.common.response;

import com.festi.backend.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> details,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(errorCode.name(), message, details, Instant.now());
    }

    public static ErrorResponse fromBindingResult(ErrorCode errorCode, BindingResult bindingResult) {
        return of(errorCode, errorCode.getMessage(), bindingResult.getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .toList());
    }

    public record FieldErrorDetail(
            String field,
            String message
    ) {

        private static FieldErrorDetail from(FieldError error) {
            return new FieldErrorDetail(error.getField(), error.getDefaultMessage());
        }
    }
}
