package com.sosuisha.domain.service;

import module java.base;

import com.sosuisha.domain.exception.WallpaperException;
import com.sosuisha.domain.model.Monitor;

/**
 * Reads the monitors of the PC and sets the wallpaper of each monitor.
 */
public interface WallpaperService {
    /**
     * Returns the monitors that are attached to the desktop now.
     *
     * @return monitors in no particular order
     * @throws WallpaperException if Windows fails to list them
     */
    List<Monitor> listMonitors() throws WallpaperException;

    /**
     * Sets the wallpaper of one monitor.
     *
     * @param monitorId id of the monitor
     * @param image     image file to use as the wallpaper
     * @throws NullPointerException if monitorId or image is null
     * @throws WallpaperException   if Windows fails to set it
     */
    void setWallpaper(String monitorId, Path image) throws WallpaperException;

    /**
     * Returns the current wallpaper of one monitor.
     *
     * @param monitorId id of the monitor
     * @return image file of the wallpaper, or empty if the monitor has none
     * @throws NullPointerException if monitorId is null
     * @throws WallpaperException   if Windows fails to read it
     */
    Optional<Path> getWallpaper(String monitorId) throws WallpaperException;
}
