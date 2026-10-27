package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a wallpaper image cannot be composed from a source image.
 */
public class ImageComposeException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @param cause   cause of the error, or null if there is none
     * @throws NullPointerException     if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public ImageComposeException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
