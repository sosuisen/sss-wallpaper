package com.sosuisha.service;

import module java.base;

import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.domain.exception.WallpaperException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.model.WallpaperChange;
import com.sosuisha.domain.repository.MonitorSettingsRepository;
import com.sosuisha.domain.service.ImageComposer;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.WallpaperChanger;
import com.sosuisha.domain.service.WallpaperService;

/**
 * Wallpaper changer that composes an image of the monitor size and sets it as the wallpaper.
 */
public class ComposingWallpaperChanger implements WallpaperChanger {
    private final WallpaperService wallpaperService;
    private final ImageComposer imageComposer;
    private final ImagePicker imagePicker;
    private final MonitorSettingsRepository settingsRepository;

    /**
     * Creates the changer.
     *
     * @param wallpaperService   service that reads the monitors and sets the wallpapers
     * @param imageComposer      composer that fits an image in a monitor
     * @param imagePicker        picker that chooses an image from a registered folder
     * @param settingsRepository repository of the settings of each monitor
     * @throws NullPointerException if any argument is null
     */
    public ComposingWallpaperChanger(
        WallpaperService wallpaperService, ImageComposer imageComposer, ImagePicker imagePicker,
        MonitorSettingsRepository settingsRepository) {
        this.wallpaperService =
            Objects.requireNonNull(wallpaperService, "wallpaperService must not be null");
        this.imageComposer =
            Objects.requireNonNull(imageComposer, "imageComposer must not be null");
        this.imagePicker = Objects.requireNonNull(imagePicker, "imagePicker must not be null");
        this.settingsRepository =
            Objects.requireNonNull(settingsRepository, "settingsRepository must not be null");
    }

    @Override
    public Path change(Monitor monitor, Path source, Alignment alignment, FillColor fillColor)
        throws ImageComposeException, WallpaperException {
        Objects.requireNonNull(monitor, "monitor must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(alignment, "alignment must not be null");
        Objects.requireNonNull(fillColor, "fillColor must not be null");
        var composed = imageComposer.compose(source, monitor, alignment, fillColor);
        wallpaperService.setWallpaper(monitor.id(), composed);
        return composed;
    }

    @Override
    public List<WallpaperChange> randomizeAll() throws UnrecoverableException {
        var settings = settingsRepository.load();
        var picks = pickImages(leftToRight(wallpaperService.listMonitors()), settings);
        if (picks.isEmpty()) { return List.of(); }
        settingsRepository.save(withSourceImages(settings, picks));
        return picks.stream()
            .map(
                pick -> new WallpaperChange(
                    pick.monitor().id(), pick.image(),
                    change(
                        pick.monitor(), pick.image(), pick.settings().alignment(),
                        pick.settings().fillColor()
                    )
                )
            )
            .toList();
    }

    private record Pick(Monitor monitor, Path image, MonitorSettings settings) {
    }

    private static List<Monitor> leftToRight(List<Monitor> monitors) {
        return monitors.stream().sorted(Monitor.LEFT_TO_RIGHT).toList();
    }

    private List<Pick> pickImages(List<Monitor> monitors, Map<String, MonitorSettings> settings) {
        return monitors.stream()
            .flatMap(
                monitor -> Optional.ofNullable(settings.get(monitor.id()))
                    .flatMap(
                        setting -> setting.folder()
                            .flatMap(imagePicker::pick)
                            .map(image -> new Pick(monitor, image, setting))
                    )
                    .stream()
            )
            .toList();
    }

    private static Map<String, MonitorSettings> withSourceImages(
        Map<String, MonitorSettings> settings, List<Pick> picks) {
        var updated = new HashMap<>(settings);
        for (var pick : picks) {
            var setting = pick.settings();
            updated.put(
                pick.monitor().id(), new MonitorSettings(
                    setting.folder(), Optional.of(pick.image()), setting.alignment(),
                    setting.fillColor()
                )
            );
        }
        return Map.copyOf(updated);
    }
}
