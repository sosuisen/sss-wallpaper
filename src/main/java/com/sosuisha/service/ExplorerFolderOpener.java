package com.sosuisha.service;

import module java.base;

import com.sosuisha.domain.exception.FolderOpenException;
import com.sosuisha.domain.service.FolderOpener;

/**
 * Folder opener that opens folders in Windows Explorer.
 */
public class ExplorerFolderOpener implements FolderOpener {
    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if folder is null
     * @throws FolderOpenException  if Explorer cannot be started
     */
    @Override
    public void open(Path folder) throws FolderOpenException {
        Objects.requireNonNull(folder, "folder must not be null");
        try {
            // Not waited for, because Explorer keeps running after the window opens.
            new ProcessBuilder("explorer.exe", folder.toAbsolutePath().toString()).start();
        } catch (IOException e) {
            throw new FolderOpenException("Cannot open the folder: " + folder, e);
        }
    }
}
