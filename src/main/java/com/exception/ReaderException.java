package com.exception;

public class ReaderException extends RuntimeException {

    /**
     * @param message the String that contains a detailed message.
     */
    public ReaderException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with a message and nested exception.
     * @param msg the exception message.
     * @param nested the cause of the exception.
     *
     */
    public ReaderException(String msg, Throwable nested) {
        super(msg, nested);
    }

    /**
     * Constructs a new instance with a nested exception and empty message.
     * @param nested the cause of the exception.
     */
    public ReaderException(Throwable nested) {
        super(nested);
    }
}
