package com.exception;

public class BatchWriteException extends Exception {

    public BatchWriteException(String message) {
        super(message);
    }

    public BatchWriteException(String message, Throwable cause) {
        super(message, cause);
    }

}
