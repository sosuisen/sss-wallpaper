package com.sosuisha.domain.model;

import module java.base;

/**
 * Settings of one monitor that the app keeps between runs.
 *
 * @param folder    image folder registered for the monitor
 * @param image     source image of the current wallpaper, before it is fitted
 * @param alignment horizontal position of the fitted image
 * @param fillColor color of the part that the fitted image does not cover
 */
public record MonitorSettings(
    Optional<Path> folder, Optional<Path> image, Alignment alignment, FillColor fillColor) {
    /** Settings of a monitor that the user has not touched yet. */
    public static final MonitorSettings DEFAULT =
        new MonitorSettings(Optional.empty(), Optional.empty(), Alignment.LEFT, FillColor.WHITE);

    /**
     * Creates the settings.
     *
     * @throws NullPointerException if any component is null
     */
    public MonitorSettings {
        Objects.requireNonNull(folder, "folder must not be null");
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(alignment, "alignment must not be null");
        Objects.requireNonNull(fillColor, "fillColor must not be null");
    }
}
