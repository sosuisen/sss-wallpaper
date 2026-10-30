package com.sosuisha.domain.service;

import java.nio.file.Path;

import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;

/**
 * Makes a wallpaper image of the exact size of a monitor from a source image.
 */
@FunctionalInterface
public interface ImageComposer {
    /**
     * Fits the source image in the monitor and saves the result as a file.
     *
     * @param source    image file to fit
     * @param monitor   monitor that shows the result
     * @param alignment horizontal position of the fitted image
     * @param fillColor color of the part that the fitted image does not cover
     * @return file of the composed image
     * @throws NullPointerException if any argument is null
     * @throws ImageComposeException if the source
     *         cannot be read or the result cannot be saved
     */
    Path compose(Path source, Monitor monitor, Alignment alignment, FillColor fillColor)
        throws ImageComposeException;
}
