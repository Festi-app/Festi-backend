package com.festi.backend.common.exception;

public class NotFoundException extends FestiException {

    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
