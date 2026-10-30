package com.sosuisha.domain.exception;

import module java.base;

import org.jspecify.annotations.Nullable;

/**
 * Base class of the errors that the app cannot recover from. The app shows
 * the message to the user.
 */
public abstract class UnrecoverableException extends RuntimeException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @param cause   cause of the error, or null if there is none
     * @throws NullPointerException     if message is null
     * @throws IllegalArgumentException if message is blank
     */
    protected UnrecoverableException(String message, @Nullable Throwable cause) {
        super(requireNotBlank(message), cause);
    }

    private static String requireNotBlank(String message) {
        Objects.requireNonNull(message, "message must not be null");
        if (message.isBlank()) { throw new IllegalArgumentException("message must not be blank"); }
        return message;
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
