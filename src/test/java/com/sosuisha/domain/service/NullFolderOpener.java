package com.sosuisha.domain.service;

import module java.base;

/**
 * A folder opener that opens nothing.
 */
public class NullFolderOpener implements FolderOpener {
    @Override
    public void open(Path folder) {}
}
