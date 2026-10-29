package com.sosuisha.domain.service;

import java.nio.file.Path;

/**
 * A folder opener that opens nothing.
 */
public class NullFolderOpener implements FolderOpener {
    @Override
    public void open(Path folder) {}
}
