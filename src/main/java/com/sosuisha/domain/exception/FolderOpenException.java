package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a folder cannot be opened in the file manager of the OS.
 */
public class FolderOpenException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @param cause   cause of the error, or null if there is none
     * @throws NullPointerException if message is null
     */
    public FolderOpenException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
