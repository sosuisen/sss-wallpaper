package com.sosuisha.presentation.screens.monitors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.ImageFiles;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.repository.MonitorSettingsRepository;
import com.sosuisha.domain.service.FolderOpener;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.WallpaperChanger;
import com.sosuisha.domain.service.WallpaperService;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * View model of the monitors screen.
 */
public class MonitorsViewModel {
    /** Total width of all the frames that the view can show without scrolling. */
    static final double MAX_FRAMES_WIDTH = 960;
    // Frames larger than this look out of proportion to the window.
    private static final double MAX_SCALE = 1.0 / 8;

    private final WallpaperService wallpaperService;
    private final WallpaperChanger wallpaperChanger;
    private final ImagePicker imagePicker;
    private final MonitorSettingsRepository settingsRepository;
    private final FolderOpener folderOpener;
    private final ObservableList<Monitor> monitors = FXCollections.observableArrayList();
    private final ObservableList<Monitor> readOnlyMonitors =
        FXCollections.unmodifiableObservableList(monitors);
    // The size of a monitor is needed to remake its wallpaper, so every
    // monitor that this view model has seen is kept by id.
    private final Map<String, Monitor> knownMonitors = new HashMap<>();
    private final Map<String, ObjectProperty<Alignment>> alignments = new HashMap<>();
    private final Map<String, ObjectProperty<FillColor>> fillColors = new HashMap<>();
    private final Map<String, ReadOnlyObjectWrapper<Optional<Path>>> folders = new HashMap<>();
    private final Map<String, ReadOnlyObjectWrapper<Optional<Path>>> wallpapers =
        new HashMap<>();
    private final Map<String, ObjectProperty<Optional<Path>>> sourceImages = new HashMap<>();
    private double frameScale = MAX_SCALE;
    // While the saved settings are restored, a changed setting must not
    // remake the wallpaper or save the settings that are only half restored.
    private boolean restoring = false;

    /**
     * Creates the view model.
     *
     * @param wallpaperService   service that reads the monitors and their current wallpapers
     * @param wallpaperChanger   changer that makes and sets the wallpapers
     * @param imagePicker        picker that chooses an image from a dropped folder
     * @param settingsRepository repository of the settings of each monitor
     * @param folderOpener       opener that shows the location of a wallpaper source
     * @throws NullPointerException if any argument is null
     */
    public MonitorsViewModel(
        WallpaperService wallpaperService, WallpaperChanger wallpaperChanger,
        ImagePicker imagePicker,
        MonitorSettingsRepository settingsRepository, FolderOpener folderOpener) {
        this.wallpaperService =
            Objects.requireNonNull(wallpaperService, "wallpaperService must not be null");
        this.wallpaperChanger =
            Objects.requireNonNull(wallpaperChanger, "wallpaperChanger must not be null");
        this.imagePicker = Objects.requireNonNull(imagePicker, "imagePicker must not be null");
        this.settingsRepository =
            Objects.requireNonNull(settingsRepository, "settingsRepository must not be null");
        this.folderOpener = Objects.requireNonNull(folderOpener, "folderOpener must not be null");
    }

    /**
     * Returns the monitors from left to right.
     *
     * @return read-only list of the monitors
     */
    public ObservableList<Monitor> getMonitors() {
        return readOnlyMonitors;
    }

    /**
     * Returns the alignment of the wallpaper of a monitor. The default is
     * {@link Alignment#LEFT}. Changing it saves the settings and remakes the
     * wallpaper from the source image of the current wallpaper.
     *
     * @param monitor monitor of the alignment
     * @return alignment property of the monitor
     * @throws NullPointerException if monitor is null
     */
    public ObjectProperty<Alignment> alignmentProperty(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        knownMonitors.put(monitor.id(), monitor);
        return alignment(monitor.id());
    }

    /**
     * Returns the color that fills the part of the monitor that the wallpaper
     * image does not cover. The default is {@link FillColor#WHITE}. Changing
     * it saves the settings and remakes the wallpaper from the source image of
     * the current wallpaper.
     *
     * @param monitor monitor of the fill color
     * @return fill color property of the monitor
     * @throws NullPointerException if monitor is null
     */
    public ObjectProperty<FillColor> fillColorProperty(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        knownMonitors.put(monitor.id(), monitor);
        return fillColor(monitor.id());
    }

    /**
     * Returns the name that tells where the wallpaper of a monitor comes from.
     * It is the name of the registered folder. Without a folder, it is the
     * file name of the source image. It is empty if the monitor has neither.
     *
     * @param monitor monitor of the source
     * @return source name of the monitor
     * @throws NullPointerException if monitor is null
     */
    public ObservableValue<String> sourceNameProperty(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        var folder = folder(monitor.id());
        var image = sourceImage(monitor.id());
        return Bindings.createStringBinding(
            () -> folder.get().or(image::get).map(MonitorsViewModel::fileName).orElse(""),
            folder, image
        );
    }

    /**
     * Returns whether the location of the wallpaper source of a monitor can be
     * opened. It is true when the monitor has a registered folder or a source image.
     *
     * @param monitor monitor of the location
     * @return whether the location can be opened
     * @throws NullPointerException if monitor is null
     */
    public ObservableBooleanValue canOpenLocationProperty(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        var id = monitor.id();
        return Bindings.createBooleanBinding(
            () -> location(id).isPresent(), folder(id), sourceImage(id)
        );
    }

    /**
     * Opens the location of the wallpaper source of a monitor in the file
     * manager. It is the registered folder, or the folder of the source image
     * when no folder is registered. Nothing is opened if the monitor has neither.
     *
     * @param monitor monitor of the location
     * @throws NullPointerException if monitor is null
     * @throws com.sosuisha.domain.exception.FolderOpenException if the folder cannot be opened
     */
    public void openLocation(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        location(monitor.id()).ifPresent(folderOpener::open);
    }

    /**
     * Returns the current wallpaper of a monitor.
     *
     * @param monitor monitor of the wallpaper
     * @return wallpaper property of the monitor. It is empty if the monitor has no wallpaper.
     * @throws NullPointerException if monitor is null
     */
    public ReadOnlyObjectProperty<Optional<Path>> wallpaperProperty(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        return wallpaper(monitor.id()).getReadOnlyProperty();
    }

    /**
     * Returns the size of the frame that shows a monitor. All the frames use
     * the same scale, so they keep the size ratio and the direction of the
     * monitors. The scale is updated when the monitors are loaded.
     *
     * @param monitor monitor of the frame
     * @return size of the frame
     * @throws NullPointerException if monitor is null
     */
    public FrameSize frameSize(Monitor monitor) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        return new FrameSize(monitor.width() * frameScale, monitor.height() * frameScale);
    }

    /**
     * Loads the monitors from the service and sorts them from left to right.
     * The current wallpaper and the saved settings of each monitor are loaded
     * too. Loading does not change any wallpaper. If the saved settings
     * cannot be read, the monitors are still loaded with the default settings.
     *
     * @throws com.sosuisha.domain.exception.RepositoryException if the saved settings cannot be
     *                                                           read
     */
    public void loadMonitors() {
        var loaded = wallpaperService.listMonitors().stream()
            .sorted(Monitor.LEFT_TO_RIGHT)
            .toList();
        for (var monitor : loaded) {
            knownMonitors.put(monitor.id(), monitor);
            wallpaper(monitor.id()).set(wallpaperService.getWallpaper(monitor.id()));
        }
        try {
            restoreSettings(settingsRepository.load());
        } finally {
            // The view builds the frames when the list changes, so the scale and
            // the states of the frames must be ready before it. A broken settings
            // file must not leave the screen without frames.
            frameScale = scaleToFit(loaded);
            monitors.setAll(loaded);
        }
    }

    /**
     * Handles the files dropped on the frame of a monitor. The first folder or
     * image file decides what happens. A folder is registered as the image
     * folder of the monitor, and an image picked from it becomes the
     * wallpaper. An image file is fitted in the monitor and set as its
     * wallpaper, and the folder of the monitor is unregistered. Nothing
     * happens if neither is dropped. The settings are saved after a change.
     *
     * @param monitor monitor whose frame received the files
     * @param files   dropped files
     * @throws NullPointerException if monitor or files is null
     */
    public void dropFiles(Monitor monitor, List<Path> files) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        Objects.requireNonNull(files, "files must not be null");
        knownMonitors.put(monitor.id(), monitor);
        for (var file : files) {
            // The steps that can fail come first, so a failure leaves the
            // settings as they were.
            if (Files.isDirectory(file)) {
                var picked = imagePicker.pick(file);
                picked.ifPresent(image -> applyWallpaper(monitor, image));
                folder(monitor.id()).set(Optional.of(file));
                picked.ifPresent(image -> sourceImage(monitor.id()).set(Optional.of(image)));
                saveSettings();
                return;
            }
            if (ImageFiles.hasImageExtension(file)) {
                applyWallpaper(monitor, file);
                // A folder left registered would replace the dropped image on the next randomize.
                folder(monitor.id()).set(Optional.empty());
                sourceImage(monitor.id()).set(Optional.of(file));
                saveSettings();
                return;
            }
        }
    }

    /**
     * Changes the wallpaper of every connected monitor that has a registered
     * folder. Each monitor gets an image picked at random from its own folder.
     * A monitor without a folder, or with a folder that has no image, keeps
     * its wallpaper. The picked images become the source images.
     */
    public void randomizeAll() {
        for (var change : wallpaperChanger.randomizeAll()) {
            sourceImage(change.monitorId()).set(Optional.of(change.source()));
            wallpaper(change.monitorId()).set(Optional.of(change.wallpaper()));
        }
    }

    private static double scaleToFit(List<Monitor> monitors) {
        int totalWidth = monitors.stream().mapToInt(Monitor::width).sum();
        // Without monitors, the division gives Infinity and MAX_SCALE is used.
        return Math.min(MAX_SCALE, MAX_FRAMES_WIDTH / totalWidth);
    }

    private void restoreSettings(Map<String, MonitorSettings> saved) {
        restoring = true;
        try {
            saved.forEach((monitorId, settings) -> {
                folder(monitorId).set(settings.folder());
                sourceImage(monitorId).set(settings.image());
                alignment(monitorId).set(settings.alignment());
                fillColor(monitorId).set(settings.fillColor());
            });
        } finally {
            restoring = false;
        }
    }

    private void saveSettings() {
        var ids = new HashSet<String>();
        ids.addAll(alignments.keySet());
        ids.addAll(fillColors.keySet());
        ids.addAll(folders.keySet());
        ids.addAll(sourceImages.keySet());
        var settings = new HashMap<String, MonitorSettings>();
        for (var id : ids) {
            settings.put(
                id, new MonitorSettings(
                    folder(id).get(), sourceImage(id).get(),
                    alignment(id).get(),
                    fillColor(id).get()
                )
            );
        }
        settingsRepository.save(Map.copyOf(settings));
    }

    private ObjectProperty<Alignment> alignment(String monitorId) {
        return setting(alignments, monitorId, Alignment.LEFT);
    }

    private ObjectProperty<FillColor> fillColor(String monitorId) {
        return setting(fillColors, monitorId, FillColor.WHITE);
    }

    private <T> ObjectProperty<T> setting(
        Map<String, ObjectProperty<T>> settings, String monitorId, T initialValue) {
        return settings.computeIfAbsent(monitorId, _ -> {
            var setting = new SimpleObjectProperty<>(initialValue);
            setting.subscribe((_, _) -> {
                if (restoring) { return; }
                saveSettings();
                // Only the methods that take a Monitor change a setting, and
                // they register the monitor first.
                var monitor = Objects.requireNonNull(knownMonitors.get(monitorId));
                sourceImage(monitorId).get().ifPresent(image -> applyWallpaper(monitor, image));
            });
            return setting;
        });
    }

    private ReadOnlyObjectWrapper<Optional<Path>> folder(String monitorId) {
        return folders
            .computeIfAbsent(monitorId, _ -> new ReadOnlyObjectWrapper<>(Optional.empty()));
    }

    private ObjectProperty<Optional<Path>> sourceImage(String monitorId) {
        return sourceImages
            .computeIfAbsent(monitorId, _ -> new SimpleObjectProperty<>(Optional.empty()));
    }

    private Optional<Path> location(String monitorId) {
        return folder(monitorId).get()
            .or(
                () -> sourceImage(monitorId).get()
                    .flatMap(image -> Optional.ofNullable(image.getParent()))
            );
    }

    private static String fileName(Path path) {
        var name = path.getFileName();
        // A drive root such as C:\ has no file name.
        return name == null ? path.toString() : name.toString();
    }

    private ReadOnlyObjectWrapper<Optional<Path>> wallpaper(String monitorId) {
        return wallpapers
            .computeIfAbsent(monitorId, _ -> new ReadOnlyObjectWrapper<>(Optional.empty()));
    }

    private void applyWallpaper(Monitor monitor, Path image) {
        var changed = wallpaperChanger.change(
            monitor, image, alignment(monitor.id()).get(), fillColor(monitor.id()).get()
        );
        wallpaper(monitor.id()).set(Optional.of(changed));
    }
}
