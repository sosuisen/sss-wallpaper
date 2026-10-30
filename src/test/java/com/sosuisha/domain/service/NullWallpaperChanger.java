package com.sosuisha.domain.service;

import module java.base;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.WallpaperChange;

/**
 * A wallpaper changer that changes nothing. Tests override the methods they need.
 */
public class NullWallpaperChanger implements WallpaperChanger {
    @Override
    public Path change(Monitor monitor, Path source, Alignment alignment, FillColor fillColor) {
        return source;
    }

    @Override
    public List<WallpaperChange> randomizeAll() {
        return List.of();
    }
}
