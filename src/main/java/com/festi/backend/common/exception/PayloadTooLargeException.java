package com.festi.backend.common.exception;

public class PayloadTooLargeException extends FestiException {

    public PayloadTooLargeException(String message) {
        super(ErrorCode.PAYLOAD_TOO_LARGE, message);
    }
}
