package com.exception;

/**
 * BatchReadException is a custom exception class that represents errors occurring during batch read operations.
 * It extends the standard Exception class and provides constructors for creating instances with a message and an optional cause.
 */
public class BatchReadException extends Exception {

    /**
     * Constructs a new BatchReadException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public BatchReadException(String message, Throwable cause) {
        super(message, cause);
    }

}
