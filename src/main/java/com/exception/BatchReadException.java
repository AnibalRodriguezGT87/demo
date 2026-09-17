package com.exception;

public class BatchReadException extends Exception {

    public BatchReadException(String message) {
        super(message);
    }

    public BatchReadException(String message, Throwable cause) {
        super(message, cause);
    }

}
