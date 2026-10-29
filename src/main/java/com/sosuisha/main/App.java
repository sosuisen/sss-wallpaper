package com.sosuisha.main;

import java.nio.file.Path;
import java.util.Objects;
import java.util.random.RandomGenerator;

import com.sosuisha.domain.repository.MonitorSettingsRepository;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.WallpaperChanger;
import com.sosuisha.domain.service.WallpaperService;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.monitors.MonitorsView;
import com.sosuisha.presentation.screens.monitors.MonitorsViewModel;
import com.sosuisha.repository.PropertiesMonitorSettingsRepository;
import com.sosuisha.service.ComposingWallpaperChanger;
import com.sosuisha.service.DesktopWallpaperService;
import com.sosuisha.service.ExplorerFolderOpener;
import com.sosuisha.service.FitImageComposer;
import com.sosuisha.service.RandomImagePicker;

import atlantafx.base.theme.PrimerLight;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX application of SSS Wallpaper.
 */
public class App extends Application {
    /** Change this constant during development to open another window first. */
    static final Class<? extends View> FIRST_VIEW = MonitorsView.class;
    /** System property that overrides the path of the settings file. */
    static final String SETTINGS_FILE_PROPERTY = "sss.settings.file";
    static final String ICON_RESOURCE = "sss-wallpaper.png";
    private static final Path APP_FOLDER =
        Path.of(System.getProperty("user.home"), ".sss-wallpaper");

    /**
     * Composition root that wires the dependencies and shows the first window.
     *
     * @param stage the primary stage for this application
     * @throws NullPointerException if stage is null
     */
    @Override
    public void start(Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.getIcons().add(new Image(App.class.getResource(ICON_RESOURCE).toExternalForm()));
        var windowManager = new WindowManager();
        var monitorsViewModel = createMonitorsViewModel();
        windowManager.registerView(new MonitorsView(monitorsViewModel));
        windowManager.showWindow(FIRST_VIEW, stage);
        monitorsViewModel.loadMonitors();
    }

    /**
     * Creates the view model of the monitors screen with the real services.
     *
     * @return view model whose monitors are not loaded yet
     */
    static MonitorsViewModel createMonitorsViewModel() {
        var wallpaperService = new DesktopWallpaperService();
        var imagePicker = new RandomImagePicker(RandomGenerator.getDefault());
        var settingsRepository = new PropertiesMonitorSettingsRepository(settingsFile());
        return new MonitorsViewModel(
            wallpaperService,
            createWallpaperChanger(wallpaperService, imagePicker, settingsRepository), imagePicker,
            settingsRepository, new ExplorerFolderOpener()
        );
    }

    /**
     * Creates the wallpaper changer with the real services. The launcher uses
     * it when it starts without a window.
     *
     * @return wallpaper changer
     */
    static WallpaperChanger createWallpaperChanger() {
        return createWallpaperChanger(
            new DesktopWallpaperService(), new RandomImagePicker(RandomGenerator.getDefault()),
            new PropertiesMonitorSettingsRepository(settingsFile())
        );
    }

    private static WallpaperChanger createWallpaperChanger(
        WallpaperService wallpaperService, ImagePicker imagePicker,
        MonitorSettingsRepository settingsRepository) {
        return new ComposingWallpaperChanger(
            wallpaperService, new FitImageComposer(APP_FOLDER.resolve("wallpapers")), imagePicker,
            settingsRepository
        );
    }

    private static Path settingsFile() {
        return Path.of(
            System.getProperty(
                SETTINGS_FILE_PROPERTY, APP_FOLDER.resolve("settings.properties").toString()
            )
        );
    }
}
