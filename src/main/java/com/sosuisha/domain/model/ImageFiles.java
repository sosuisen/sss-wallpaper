package com.sosuisha.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Tells which files the app treats as images.
 */
public final class ImageFiles {
    private static final List<String> IMAGE_EXTENSIONS =
        List.of(".jpg", ".jpeg", ".png", ".bmp");

    private ImageFiles() {}

    /**
     * Tells whether a file has the extension of an image. The file itself is
     * not read.
     *
     * @param file file to check
     * @return true if the extension is jpg, jpeg, png, or bmp in any case
     * @throws NullPointerException if file is null
     */
    public static boolean hasImageExtension(Path file) {
        Objects.requireNonNull(file, "file must not be null");
        var name = file.toString().toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
