package com.sosuisha.service;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.imageio.ImageIO;

import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.service.ImageComposer;

/**
 * Image composer that fits the source image in the monitor with its aspect
 * ratio kept, and fills the rest with a color.
 */
public class FitImageComposer implements ImageComposer {
    private final Path outputFolder;

    /**
     * Creates the composer.
     *
     * @param outputFolder folder to save the composed images in. It is created if missing.
     * @throws NullPointerException if outputFolder is null
     */
    public FitImageComposer(Path outputFolder) {
        this.outputFolder = Objects.requireNonNull(outputFolder, "outputFolder must not be null");
    }

    @Override
    public Path compose(Path source, Monitor monitor, Alignment alignment, FillColor fillColor) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(monitor, "monitor must not be null");
        Objects.requireNonNull(alignment, "alignment must not be null");
        Objects.requireNonNull(fillColor, "fillColor must not be null");
        var fitted = fit(read(source), monitor.width(), monitor.height(), alignment, fillColor);
        return save(fitted, monitor);
    }

    /**
     * Fits an image in a canvas of the given size with its aspect ratio kept.
     *
     * @param source    image to fit
     * @param width     width of the canvas
     * @param height    height of the canvas
     * @param alignment horizontal position of the image in the canvas
     * @param fillColor color of the canvas that the image does not cover
     * @return new image of the canvas size
     */
    static BufferedImage fit(
        BufferedImage source, int width, int height, Alignment alignment, FillColor fillColor) {
        double scale = Math.min(
            (double) width / source.getWidth(), (double) height / source.getHeight()
        );
        int fittedWidth = (int) Math.round(source.getWidth() * scale);
        int fittedHeight = (int) Math.round(source.getHeight() * scale);
        int x = switch (alignment) {
            case LEFT -> 0;
            case RIGHT -> width - fittedWidth;
        };
        int y = (height - fittedHeight) / 2;

        var canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = canvas.createGraphics();
        try {
            graphics.setColor(
                switch (fillColor) {
                    case WHITE -> Color.WHITE;
                    case BLACK -> Color.BLACK;
                }
            );
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(source, x, y, fittedWidth, fittedHeight, null);
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    private static BufferedImage read(Path source) {
        try {
            var image = ImageIO.read(source.toFile());
            if (image == null) {
                throw new ImageComposeException("Not a readable image: " + source, null);
            }
            return image;
        } catch (IOException e) {
            throw new ImageComposeException("Cannot read the image: " + source, e);
        }
    }

    private Path save(BufferedImage image, Monitor monitor) {
        // Windows may keep showing a cached copy when a file of the same name
        // is set again, so every composed image gets a new name.
        var prefix = "monitor-" + Integer.toHexString(monitor.id().hashCode()) + "-";
        var file = outputFolder.resolve(prefix + System.nanoTime() + ".png");
        try {
            Files.createDirectories(outputFolder);
            deleteOldImages(prefix);
            ImageIO.write(image, "png", file.toFile());
            return file;
        } catch (IOException e) {
            throw new ImageComposeException("Cannot save the composed image: " + file, e);
        }
    }

    private void deleteOldImages(String prefix) throws IOException {
        try (var files = Files.list(outputFolder)) {
            for (var old : files.filter(f -> String.valueOf(f.getFileName()).startsWith(prefix))
                .toList()) {
                Files.delete(old);
            }
        }
    }
}
