package com.festi.backend.common.exception;

public class BadRequestException extends FestiException {

    public BadRequestException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
