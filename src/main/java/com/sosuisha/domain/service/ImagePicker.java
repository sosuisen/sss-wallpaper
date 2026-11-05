package com.sosuisha.domain.service;

import java.nio.file.Path;
import java.util.Optional;

import com.sosuisha.domain.exception.FolderReadException;

/**
 * Picks one image file from a folder.
 */
@FunctionalInterface
public interface ImagePicker {
    /**
     * Picks one image file directly in the folder. Subfolders are not searched.
     * Every image file in the folder can be picked.
     *
     * @param folder folder to pick from
     * @return picked image file, or empty if the folder has no image file
     * @throws NullPointerException if folder is null
     * @throws FolderReadException if the folder cannot be read
     */
    default Optional<Path> pick(Path folder) throws FolderReadException {
        return pick(folder, Optional.empty());
    }

    /**
     * Picks one image file directly in the folder, and avoids the given image
     * if the folder has another one. Subfolders are not searched.
     *
     * @param folder folder to pick from
     * @param avoid  image file not to pick, unless it is the only image file.
     *               It does not have to be in the folder.
     * @return picked image file, or empty if the folder has no image file
     * @throws NullPointerException if folder or avoid is null
     * @throws FolderReadException if the folder cannot be read
     */
    Optional<Path> pick(Path folder, Optional<Path> avoid) throws FolderReadException;
}
