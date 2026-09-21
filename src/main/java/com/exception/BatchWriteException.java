package com.exception;

/**
 * BatchWriteException is a custom exception class that represents errors occurring during batch write operations.
 * It extends the standard Exception class and provides constructors for creating instances with a message and an optional cause.
 */
public class BatchWriteException extends Exception {

    /**
     * Constructs a new BatchWriteException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public BatchWriteException(String message, Throwable cause) {
        super(message, cause);
    }

}
