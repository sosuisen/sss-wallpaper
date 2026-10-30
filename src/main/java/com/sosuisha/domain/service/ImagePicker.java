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
     *
     * @param folder folder to pick from
     * @return picked image file, or empty if the folder has no image file
     * @throws NullPointerException if folder is null
     * @throws FolderReadException if the folder cannot be read
     */
    Optional<Path> pick(Path folder) throws FolderReadException;
}
