package com.sosuisha.domain.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.sosuisha.domain.model.Monitor;

/**
 * A wallpaper service that does nothing. Tests override the methods they need.
 */
public class NullWallpaperService implements WallpaperService {
    @Override
    public List<Monitor> listMonitors() {
        return List.of();
    }

    @Override
    public void setWallpaper(String monitorId, Path image) {}

    @Override
    public Optional<Path> getWallpaper(String monitorId) {
        return Optional.empty();
    }
}
