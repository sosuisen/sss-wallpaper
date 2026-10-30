package com.sosuisha.domain.service;

import java.nio.file.Path;
import java.util.List;

import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.domain.exception.WallpaperException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.WallpaperChange;

/**
 * Changes the wallpapers of the monitors.
 */
public interface WallpaperChanger {
    /**
     * Fits the source image in the monitor and sets the result as its wallpaper.
     *
     * @param monitor   monitor whose wallpaper is changed
     * @param source    image file to fit
     * @param alignment horizontal position of the fitted image
     * @param fillColor color of the part that the fitted image does not cover
     * @return image file that was set as the wallpaper
     * @throws NullPointerException if any argument is null
     * @throws ImageComposeException if the image cannot be composed
     * @throws WallpaperException if the wallpaper cannot be set
     */
    Path change(Monitor monitor, Path source, Alignment alignment, FillColor fillColor)
        throws ImageComposeException, WallpaperException;

    /**
     * Changes the wallpaper of every connected monitor that has a registered
     * folder. Each monitor gets an image picked at random from its own folder.
     * A monitor without a folder, or with a folder that has no image, keeps
     * its wallpaper. The picked images are saved as the source images before
     * the wallpapers are changed from left to right.
     *
     * @return changes from left to right, or an empty list if nothing was picked
     * @throws UnrecoverableException if the settings, the folders or the wallpapers cannot
     *                                be read or written
     */
    List<WallpaperChange> randomizeAll() throws UnrecoverableException;
}
