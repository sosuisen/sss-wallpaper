package com.sosuisha.domain.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.sosuisha.domain.model.Monitor;

/**
 * Reads the monitors of the PC and sets the wallpaper of each monitor.
 */
public interface WallpaperService {
    /**
     * Returns the monitors that are attached to the desktop now.
     *
     * @return monitors in no particular order
     */
    List<Monitor> listMonitors();

    /**
     * Sets the wallpaper of one monitor.
     *
     * @param monitorId id of the monitor
     * @param image     image file to use as the wallpaper
     * @throws NullPointerException if monitorId or image is null
     */
    void setWallpaper(String monitorId, Path image);

    /**
     * Returns the current wallpaper of one monitor.
     *
     * @param monitorId id of the monitor
     * @return image file of the wallpaper, or empty if the monitor has none
     * @throws NullPointerException if monitorId is null
     */
    Optional<Path> getWallpaper(String monitorId);
}
