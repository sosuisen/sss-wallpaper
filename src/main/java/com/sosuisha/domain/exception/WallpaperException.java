package com.sosuisha.domain.exception;

/**
 * Thrown when Windows fails to read the monitors or to set a wallpaper.
 */
public class WallpaperException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message message for the user
     * @throws NullPointerException if message is null
     */
    public WallpaperException(String message) {
        super(message, null);
    }
}
