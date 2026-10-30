package com.sosuisha.domain.service;

import java.nio.file.Path;

import com.sosuisha.domain.exception.FolderOpenException;

/**
 * Opens folders in the file manager of the OS.
 */
@FunctionalInterface
public interface FolderOpener {
    /**
     * Opens the given folder.
     *
     * @param folder folder to open
     * @throws NullPointerException if folder is null
     * @throws FolderOpenException  if the folder cannot be opened
     */
    void open(Path folder) throws FolderOpenException;
}
