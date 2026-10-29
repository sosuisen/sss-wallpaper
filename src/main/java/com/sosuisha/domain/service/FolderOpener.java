package com.sosuisha.domain.service;

import java.nio.file.Path;

/**
 * Opens folders in the file manager of the OS.
 */
@FunctionalInterface
public interface FolderOpener {
    /**
     * Opens the given folder.
     *
     * @param folder folder to open
     * @throws com.sosuisha.domain.exception.FolderOpenException if the folder cannot be opened
     */
    void open(Path folder);
}
