package com.exception;

/**
 * IsoException is a custom exception class that represents errors related to ISO operations.
 * It extends the standard Exception class and provides constructors for creating instances with a message and an optional cause.
 */
public class IsoException extends Exception {


    /**
     * Constructs a new IsoException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public IsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
