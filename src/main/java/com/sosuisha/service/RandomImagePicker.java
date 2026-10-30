package com.sosuisha.service;

import module java.base;

import com.sosuisha.domain.exception.FolderReadException;
import com.sosuisha.domain.model.ImageFiles;
import com.sosuisha.domain.service.ImagePicker;

/**
 * Image picker that picks one image file of a folder at random.
 */
public class RandomImagePicker implements ImagePicker {
    private final RandomGenerator random;

    /**
     * Creates the picker.
     *
     * @param random random number generator that chooses the image
     * @throws NullPointerException if random is null
     */
    public RandomImagePicker(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public Optional<Path> pick(Path folder) throws FolderReadException {
        Objects.requireNonNull(folder, "folder must not be null");
        // Sorted, because the order of Files.list is not defined and the
        // same random number should pick the same file.
        try (var files = Files.list(folder)) {
            var images = files
                .filter(Files::isRegularFile)
                .filter(ImageFiles::hasImageExtension)
                .sorted(Comparator.naturalOrder())
                .toList();
            if (images.isEmpty()) { return Optional.empty(); }
            return Optional.of(images.get(random.nextInt(images.size())));
        } catch (IOException e) {
            throw new FolderReadException("Cannot read the folder: " + folder, e);
        }
    }
}
