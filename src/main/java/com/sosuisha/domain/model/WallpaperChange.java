package com.sosuisha.domain.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A wallpaper that was changed on a monitor.
 *
 * @param monitorId id of the monitor
 * @param source    source image that the wallpaper was made from
 * @param wallpaper image file that was set as the wallpaper
 */
public record WallpaperChange(String monitorId, Path source, Path wallpaper) {
    /**
     * Creates a wallpaper change.
     *
     * @throws NullPointerException if any argument is null
     */
    public WallpaperChange {
        Objects.requireNonNull(monitorId, "monitorId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(wallpaper, "wallpaper must not be null");
    }
}
