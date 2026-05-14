package com.festi.backend.common.exception;

public class ConflictException extends FestiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
