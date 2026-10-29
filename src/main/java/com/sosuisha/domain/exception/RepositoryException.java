package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when the saved settings cannot be read or written.
 */
public class RepositoryException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @param cause   cause of the error, or null if there is none
     * @throws NullPointerException if message is null
     */
    public RepositoryException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
