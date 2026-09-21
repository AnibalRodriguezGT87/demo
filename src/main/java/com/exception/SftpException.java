package com.exception;

/**
 * SftpException is a custom exception class that represents errors occurring during SFTP operations.
 * It extends the standard Exception class and provides constructors for creating instances with a message and an optional cause.
 */
public class SftpException extends Exception {

    /**
     * Constructs a new SftpException with the specified detail message.
     *
     * @param message the detail message
     */
    public SftpException(String message) {
        super(message);
    }

    /**
     * Constructs a new SftpException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public SftpException(String message, Throwable cause) {
        super(message, cause);
    }

}
