package com.exception;

public class IsoException extends Exception {

    public IsoException(String message) {
        super(message);
    }

    public IsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
