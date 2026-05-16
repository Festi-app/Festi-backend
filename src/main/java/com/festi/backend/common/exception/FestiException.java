package com.festi.backend.common.exception;

import lombok.Getter;

@Getter
public class FestiException extends RuntimeException {

    private final ErrorCode errorCode;

    public FestiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FestiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
