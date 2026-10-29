package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when the files in an image folder cannot be listed.
 */
public class FolderReadException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @param cause   cause of the error, or null if there is none
     * @throws NullPointerException if message is null
     */
    public FolderReadException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
