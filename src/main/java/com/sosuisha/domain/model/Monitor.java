package com.sosuisha.domain.model;

import java.util.Comparator;
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
     * Order of the monitors from left to right on the virtual desktop. The
     * monitors with the same left edge are ordered from top to bottom.
     */
    public static final Comparator<Monitor> LEFT_TO_RIGHT =
        Comparator.comparingInt(Monitor::x).thenComparingInt(Monitor::y);

    /**
     * Creates a monitor.
     *
     * @throws NullPointerException if id is null
     */
    public Monitor {
        Objects.requireNonNull(id, "id must not be null");
        // The size is not checked. Windows gives no rectangle to a monitor
        // that cannot show a wallpaper, and the service skips such a monitor,
        // so no case is known where the size is zero or negative.
    }
}
