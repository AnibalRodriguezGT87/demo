package com.exception;

/**
 * ReaderException is a custom exception class that represents errors occurring during reading operations.
 * It extends the standard RuntimeException class and provides constructors for creating instances with a message and an optional cause.
 */
public class ReaderException extends RuntimeException {


    /**
     * Constructs a new instance with a message and nested exception.
     * @param msg the exception message.
     * @param nested the cause of the exception.
     *
     */
    public ReaderException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
