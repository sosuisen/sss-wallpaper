package com.sosuisha.domain.model;

import java.util.Objects;

/**
 * A monitor connected to the PC, placed on the virtual desktop.
 *
 * @param id     device path that identifies the monitor
 * @param x      x coordinate of the left edge in pixels
 * @param y      y coordinate of the top edge in pixels
 * @param width  width in pixels
 * @param height height in pixels
 */
public record Monitor(String id, int x, int y, int width, int height) {
    /**
     * Creates a monitor.
     *
     * @throws NullPointerException     if id is null
     * @throws IllegalArgumentException if width or height is zero or negative
     */
    public Monitor {
        Objects.requireNonNull(id, "id must not be null");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "width and height must be positive: " + width + "x" + height
            );
        }
    }
}
