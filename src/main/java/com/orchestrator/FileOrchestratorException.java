package com.orchestrator;

/**
 * FileOrchestratorException is a custom exception class that represents errors occurring during SFTP operations.
 * It extends the standard Exception class and provides constructors for creating instances with a message and an optional cause.
 */
public class FileOrchestratorException extends Exception {

    /**
     * Constructs a new FileOrchestratorException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public FileOrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

}
