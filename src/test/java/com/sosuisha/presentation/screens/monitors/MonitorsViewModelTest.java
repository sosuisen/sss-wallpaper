package com.sosuisha.presentation.screens.monitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.exception.FolderReadException;
import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.model.WallpaperChange;
import com.sosuisha.domain.repository.MonitorSettingsRepository;
import com.sosuisha.domain.repository.NullMonitorSettingsRepository;
import com.sosuisha.domain.service.FolderOpener;
import com.sosuisha.domain.service.ImageComposer;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.NullFolderOpener;
import com.sosuisha.domain.service.NullWallpaperChanger;
import com.sosuisha.domain.service.NullWallpaperService;
import com.sosuisha.domain.service.WallpaperService;
import com.sosuisha.service.ComposingWallpaperChanger;

class MonitorsViewModelTest {
    private static final Monitor LEFT_MONITOR = new Monitor("left", 0, 0, 1920, 1080);

    // Names the composed file after its inputs, so a test can see what was composed.
    private static final ImageComposer NAMING_COMPOSER =
        (source, monitor, alignment, fillColor) -> Path.of(
            source.getFileName() + "@" + monitor.width() + "x" + monitor.height() + "-"
                + alignment + "-" + fillColor
        );

    private static final ImagePicker NO_IMAGE_PICKER = _ -> Optional.empty();
    private static final ImagePicker PICK_RANDOM_PNG =
        folder -> Optional.of(folder.resolve("random.png"));

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("モニター一覧は、左端のx座標が小さい順に並ぶ")
    void the_monitor_list_is_sorted_by_the_x_coordinate_of_the_left_edge() {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(
                    new Monitor("center", 0, 0, 1920, 1080),
                    new Monitor("right", 1920, 0, 1080, 1920),
                    new Monitor("left", -2560, 0, 2560, 1440)
                );
            }
        };
        var viewModel = viewModel(
            service, NAMING_COMPOSER, NO_IMAGE_PICKER, new NullMonitorSettingsRepository(),
            new NullFolderOpener()
        );

        viewModel.loadMonitors();

        var ids = viewModel.getMonitors().stream().map(Monitor::id).toList();
        assertEquals(List.of("left", "center", "right"), ids);
    }

    @Test
    @DisplayName("左端のx座標が同じモニターは、上端のy座標が小さい順に並ぶ")
    void monitors_with_the_same_left_edge_are_sorted_by_the_y_coordinate_of_the_top_edge() {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(
                    new Monitor("lower", 0, 1080, 1920, 1080),
                    new Monitor("upper", 0, -1080, 1920, 1080),
                    new Monitor("middle", 0, 0, 1920, 1080)
                );
            }
        };
        var viewModel = viewModel(
            service, NAMING_COMPOSER, NO_IMAGE_PICKER, new NullMonitorSettingsRepository(),
            new NullFolderOpener()
        );

        viewModel.loadMonitors();

        var ids = viewModel.getMonitors().stream().map(Monitor::id).toList();
        assertEquals(List.of("upper", "middle", "lower"), ids);
    }

    @Test
    @DisplayName("ドロップすると、モニターの大きさと寄せ方で合成した画像が壁紙に設定される")
    void dropping_an_image_sets_the_image_composed_for_the_monitor_size_and_alignment() {
        var wallpapers = new ArrayList<String>();
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("notes.txt"), Path.of("photo.JPG")));

        assertEquals(List.of("left:photo.JPG@1920x1080-LEFT-WHITE"), wallpapers);
    }

    @Test
    @DisplayName("画像ではないファイルだけがドロップされたとき、壁紙は変わらない")
    void dropping_only_non_image_files_does_not_change_the_wallpaper() {
        var wallpapers = new ArrayList<String>();
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("notes.txt"), Path.of("folder")));

        assertEquals(List.of(), wallpapers);
    }

    @Test
    @DisplayName("寄せ方の既定は左寄せである")
    void the_default_alignment_is_left() {
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        assertEquals(Alignment.LEFT, viewModel.alignmentProperty(LEFT_MONITOR).get());
    }

    @Test
    @DisplayName("寄せ方を変えると、最後にドロップした画像で壁紙が作り直される")
    void changing_the_alignment_remakes_the_wallpaper_from_the_last_dropped_image() {
        var wallpapers = new ArrayList<String>();
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("first.png")));
        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("second.png")));

        viewModel.alignmentProperty(LEFT_MONITOR).set(Alignment.RIGHT);

        assertEquals("left:second.png@1920x1080-RIGHT-WHITE", wallpapers.getLast());
    }

    @Test
    @DisplayName("ドロップする前に寄せ方を変えても、壁紙は変わらない")
    void changing_the_alignment_before_any_drop_does_not_change_the_wallpaper() {
        var wallpapers = new ArrayList<String>();
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.alignmentProperty(LEFT_MONITOR).set(Alignment.RIGHT);

        assertEquals(List.of(), wallpapers);
    }

    @Test
    @DisplayName("フォルダを枠にドロップすると、そのモニターのフォルダとして登録され、保存される")
    void dropping_a_folder_on_a_frame_registers_and_saves_it_as_the_folder_of_the_monitor()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos"));
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var repository = savingRepository(saved);
        var viewModel =
            viewModel(
                new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER, repository,
                new NullFolderOpener()
            );

        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        assertEquals("photos", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
        assertEquals(
            Optional.of(folder),
            saved.getLast().getOrDefault("left", MonitorSettings.DEFAULT).folder()
        );
    }

    @Test
    @DisplayName("フォルダをドロップすると、フォルダの中からランダムに選んだ1枚が壁紙に設定される")
    void dropping_a_folder_sets_an_image_picked_at_random_from_the_folder_as_the_wallpaper()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos.png"));
        var wallpapers = new ArrayList<String>();
        ImagePicker picker = from -> Optional.of(from.resolve("picked.jpg"));
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, picker,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        assertEquals(List.of("left:picked.jpg@1920x1080-LEFT-WHITE"), wallpapers);
    }

    @Test
    @DisplayName("フォルダをドロップしたあとで寄せ方を変えると、フォルダから選んだ画像で壁紙が作り直される")
    void changing_the_alignment_after_dropping_a_folder_remakes_the_wallpaper_from_the_picked_image()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos"));
        var wallpapers = new ArrayList<String>();
        ImagePicker picker = from -> Optional.of(from.resolve("picked.jpg"));
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, picker,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        viewModel.alignmentProperty(LEFT_MONITOR).set(Alignment.RIGHT);

        assertEquals("left:picked.jpg@1920x1080-RIGHT-WHITE", wallpapers.getLast());
    }

    @Test
    @DisplayName("画像のないフォルダをドロップすると、フォルダは登録されるが、壁紙は変わらない")
    void dropping_a_folder_without_images_registers_it_but_does_not_change_the_wallpaper()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("empty"));
        var wallpapers = new ArrayList<String>();
        var viewModel = viewModel(
            recordingService(wallpapers), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        assertEquals("empty", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
        assertEquals(List.of(), wallpapers);
    }

    @Test
    @DisplayName("フォルダから画像を選べないとき、フォルダは登録されない")
    void a_folder_that_cannot_be_read_is_not_registered() throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("locked"));
        ImagePicker picker = from -> {
            throw new FolderReadException("Cannot read the folder: " + from, null);
        };
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, picker,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        assertThrows(
            FolderReadException.class, () -> viewModel.dropFiles(LEFT_MONITOR, List.of(folder))
        );

        assertEquals("", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    @Test
    @DisplayName("ドロップした画像を合成できないとき、元の画像とフォルダの登録は変わらず、保存もされない")
    void an_image_that_cannot_be_composed_changes_neither_the_source_nor_the_saved_settings()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos"));
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        ImageComposer composer = (source, monitor, alignment, fillColor) -> {
            if (source.endsWith("broken.png")) {
                throw new ImageComposeException("Cannot read the image: " + source, null);
            }
            return NAMING_COMPOSER.compose(source, monitor, alignment, fillColor);
        };
        var viewModel = viewModel(
            new NullWallpaperService(), composer, PICK_RANDOM_PNG, savingRepository(saved),
            new NullFolderOpener()
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        assertThrows(
            ImageComposeException.class,
            () -> viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("broken.png")))
        );

        assertEquals("photos", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
        assertEquals(1, saved.size());
    }

    @Test
    @DisplayName("フォルダを登録した枠に画像をドロップすると、フォルダの登録が解除され、保存される")
    void dropping_an_image_on_a_frame_with_a_folder_unregisters_and_saves_the_folder()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos"));
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, PICK_RANDOM_PNG, savingRepository(saved),
            new NullFolderOpener()
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("photo.png")));

        // A registered folder wins over the image, so the image name means the folder is gone.
        assertEquals("photo.png", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
        assertEquals(
            Optional.empty(),
            saved.getLast().getOrDefault("left", MonitorSettings.DEFAULT).folder()
        );
    }

    @Test
    @DisplayName("フォルダを登録した枠には、フォルダの名前が表示される")
    void a_frame_with_a_folder_shows_the_name_of_the_folder() throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("cats"));
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, PICK_RANDOM_PNG,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        assertEquals("cats", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    @Test
    @DisplayName("フォルダを登録した枠に画像をドロップすると、画像のファイル名が表示される")
    void dropping_an_image_on_a_frame_with_a_folder_shows_the_file_name_of_the_image()
        throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("cats"));
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, PICK_RANDOM_PNG,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );
        var sourceName = viewModel.sourceNameProperty(LEFT_MONITOR);
        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("dir", "photo.png")));

        assertEquals("photo.png", sourceName.getValue());
    }

    @Test
    @DisplayName("フォルダも画像もない枠には、何も表示されない")
    void a_frame_without_a_folder_or_an_image_shows_nothing() {
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        assertEquals("", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    @Test
    @DisplayName("ドライブのルートを登録した枠には、ルートのパスが表示される")
    void a_frame_with_a_drive_root_shows_the_path_of_the_root() {
        var root = tempDir.getRoot();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(root));

        assertEquals(root.toString(), viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    @Test
    @DisplayName("モニター一覧を読み込むと、保存されたフォルダの登録も各モニターに読み込まれる")
    void loading_the_monitors_also_loads_the_saved_folder_of_each_monitor() {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(LEFT_MONITOR);
            }
        };
        var repository = loadingRepository(
            Map.of(
                "left",
                new MonitorSettings(
                    Optional.of(Path.of("photos")), Optional.empty(), Alignment.LEFT,
                    FillColor.WHITE
                )
            )
        );
        var viewModel =
            viewModel(
                service, NAMING_COMPOSER, NO_IMAGE_PICKER, repository, new NullFolderOpener()
            );

        viewModel.loadMonitors();

        assertEquals("photos", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    @Test
    @DisplayName("保存された設定を読み込めないとき、エラーになるが、モニター一覧は読み込まれる")
    void a_failure_to_load_the_settings_is_an_error_but_the_monitors_are_still_loaded() {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(LEFT_MONITOR);
            }
        };
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                throw new RepositoryException("Cannot read the settings file", null);
            }
        };
        var viewModel =
            viewModel(
                service, NAMING_COMPOSER, NO_IMAGE_PICKER, repository, new NullFolderOpener()
            );

        assertThrows(RepositoryException.class, viewModel::loadMonitors);

        assertEquals(List.of(LEFT_MONITOR), viewModel.getMonitors());
    }

    @Test
    @DisplayName("枠の大きさは、モニターの大きさに比例する（縦置きのモニターの枠は縦長になる）")
    void the_frame_size_is_proportional_to_the_monitor_size() {
        var landscape = new Monitor("landscape", 0, 0, 1920, 1080);
        var portrait = new Monitor("portrait", 1920, 0, 1080, 1920);
        var viewModel = viewModelWith(landscape, portrait);

        viewModel.loadMonitors();

        var landscapeFrame = viewModel.frameSize(landscape);
        var portraitFrame = viewModel.frameSize(portrait);
        double scale = landscapeFrame.width() / 1920;
        assertEquals(1080 * scale, landscapeFrame.height(), 0.001);
        assertEquals(1080 * scale, portraitFrame.width(), 0.001);
        assertEquals(1920 * scale, portraitFrame.height(), 0.001);
    }

    @Test
    @DisplayName("モニターが多いとき、全モニターの枠の合計幅は決まった幅に収まる")
    void with_many_monitors_the_total_width_of_the_frames_fits_in_the_fixed_width() {
        var viewModel = viewModelWith(
            new Monitor("a", 0, 0, 3840, 2160),
            new Monitor("b", 3840, 0, 3840, 2160),
            new Monitor("c", 7680, 0, 3840, 2160)
        );

        viewModel.loadMonitors();

        double totalWidth = viewModel.getMonitors().stream()
            .mapToDouble(monitor -> viewModel.frameSize(monitor).width())
            .sum();
        assertEquals(MonitorsViewModel.MAX_FRAMES_WIDTH, totalWidth, 0.001);
    }

    @Test
    @DisplayName("モニター一覧を読み込むと、各モニターの現在の壁紙も読み込まれる")
    void loading_the_monitors_also_loads_the_current_wallpaper_of_each_monitor() {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(LEFT_MONITOR);
            }

            @Override
            public Optional<Path> getWallpaper(String monitorId) {
                return Optional.of(Path.of(monitorId + "-wallpaper.jpg"));
            }
        };
        var viewModel = viewModel(
            service, NAMING_COMPOSER, NO_IMAGE_PICKER, new NullMonitorSettingsRepository(),
            new NullFolderOpener()
        );

        viewModel.loadMonitors();

        assertEquals(
            Optional.of(Path.of("left-wallpaper.jpg")),
            viewModel.wallpaperProperty(LEFT_MONITOR).get()
        );
    }

    @Test
    @DisplayName("壁紙が設定されていないモニターでは、現在の壁紙は空である")
    void the_current_wallpaper_is_empty_for_a_monitor_without_a_wallpaper() {
        var viewModel = viewModelWith(LEFT_MONITOR);

        viewModel.loadMonitors();

        assertEquals(Optional.empty(), viewModel.wallpaperProperty(LEFT_MONITOR).get());
    }

    @Test
    @DisplayName("画像をドロップして壁紙を変えると、そのモニターの現在の壁紙も変わる")
    void changing_the_wallpaper_by_dropping_an_image_also_changes_the_current_wallpaper() {
        var viewModel = viewModelWith(LEFT_MONITOR);
        viewModel.loadMonitors();

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("photo.png")));

        assertEquals(
            Optional.of(Path.of("photo.png@1920x1080-LEFT-WHITE")),
            viewModel.wallpaperProperty(LEFT_MONITOR).get()
        );
    }

    @Test
    @DisplayName("画像をドロップすると、元の画像と寄せ方がモニターの設定として保存される")
    void dropping_an_image_saves_the_source_image_and_the_alignment_as_the_settings_of_the_monitor() {
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER, savingRepository(saved),
            new NullFolderOpener()
        );

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("photo.png")));

        assertEquals(
            new MonitorSettings(
                Optional.empty(), Optional.of(Path.of("photo.png")), Alignment.LEFT, FillColor.WHITE
            ),
            saved.getLast().get("left")
        );
    }

    @Test
    @DisplayName("寄せ方を変えると、その寄せ方が保存される")
    void changing_the_alignment_saves_the_alignment() {
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER, savingRepository(saved),
            new NullFolderOpener()
        );

        viewModel.alignmentProperty(LEFT_MONITOR).set(Alignment.RIGHT);

        assertEquals(
            Alignment.RIGHT,
            saved.getLast().getOrDefault("left", MonitorSettings.DEFAULT).alignment()
        );
    }

    @Test
    @DisplayName("モニター一覧を読み込むと、保存された寄せ方が読み込まれ、壁紙は変わらない")
    void loading_the_monitors_loads_the_saved_alignment_without_changing_the_wallpaper() {
        var wallpapers = new ArrayList<String>();
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return Map.of(
                    "left", new MonitorSettings(
                        Optional.empty(), Optional.of(Path.of("photo.png")), Alignment.RIGHT,
                        FillColor.WHITE
                    )
                );
            }

            @Override
            public void save(Map<String, MonitorSettings> settings) {
                saved.add(settings);
            }
        };
        var viewModel = viewModel(
            recordingService(wallpapers, LEFT_MONITOR), NAMING_COMPOSER, NO_IMAGE_PICKER,
            repository, new NullFolderOpener()
        );

        viewModel.loadMonitors();

        assertEquals(Alignment.RIGHT, viewModel.alignmentProperty(LEFT_MONITOR).get());
        assertEquals(List.of(), wallpapers);
        assertEquals(List.of(), saved);
    }

    @Test
    @DisplayName("再起動後にドロップしていなくても、寄せ方を変えると、前回の元の画像で壁紙が作り直される")
    void after_a_restart_changing_the_alignment_remakes_the_wallpaper_from_the_last_source_image() {
        var wallpapers = new ArrayList<String>();
        var repository = loadingRepository(
            Map.of(
                "left", new MonitorSettings(
                    Optional.empty(), Optional.of(Path.of("photo.png")), Alignment.LEFT,
                    FillColor.WHITE
                )
            )
        );
        var viewModel = viewModel(
            recordingService(wallpapers, LEFT_MONITOR), NAMING_COMPOSER, NO_IMAGE_PICKER,
            repository, new NullFolderOpener()
        );
        viewModel.loadMonitors();

        viewModel.alignmentProperty(LEFT_MONITOR).set(Alignment.RIGHT);

        assertEquals(List.of("left:photo.png@1920x1080-RIGHT-WHITE"), wallpapers);
    }

    @Test
    @DisplayName("埋める色の初期値は白である")
    void the_default_fill_color_is_white() {
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        assertEquals(FillColor.WHITE, viewModel.fillColorProperty(LEFT_MONITOR).get());
    }

    @Test
    @DisplayName("埋める色を変えると、前回の元の画像で壁紙が作り直される")
    void changing_the_fill_color_remakes_the_wallpaper_from_the_last_source_image() {
        var wallpapers = new ArrayList<String>();
        var repository = loadingRepository(
            Map.of(
                "left", new MonitorSettings(
                    Optional.empty(), Optional.of(Path.of("photo.png")), Alignment.LEFT,
                    FillColor.WHITE
                )
            )
        );
        var viewModel = viewModel(
            recordingService(wallpapers, LEFT_MONITOR), NAMING_COMPOSER, NO_IMAGE_PICKER,
            repository, new NullFolderOpener()
        );
        viewModel.loadMonitors();

        viewModel.fillColorProperty(LEFT_MONITOR).set(FillColor.BLACK);

        assertEquals(List.of("left:photo.png@1920x1080-LEFT-BLACK"), wallpapers);
    }

    @Test
    @DisplayName("埋める色を変えると、その色が保存される")
    void changing_the_fill_color_saves_the_fill_color() {
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER, savingRepository(saved),
            new NullFolderOpener()
        );

        viewModel.fillColorProperty(LEFT_MONITOR).set(FillColor.BLACK);

        assertEquals(
            FillColor.BLACK,
            saved.getLast().getOrDefault("left", MonitorSettings.DEFAULT).fillColor()
        );
    }

    @Test
    @DisplayName("モニター一覧を読み込むと、保存された埋める色が読み込まれる")
    void loading_the_monitors_loads_the_saved_fill_color() {
        var repository = loadingRepository(
            Map.of(
                "left", new MonitorSettings(
                    Optional.empty(), Optional.empty(), Alignment.LEFT, FillColor.BLACK
                )
            )
        );
        var viewModel = viewModel(
            recordingService(new ArrayList<>(), LEFT_MONITOR), NAMING_COMPOSER, NO_IMAGE_PICKER,
            repository, new NullFolderOpener()
        );

        viewModel.loadMonitors();

        assertEquals(FillColor.BLACK, viewModel.fillColorProperty(LEFT_MONITOR).get());
    }

    @Test
    @DisplayName("フォルダを登録したモニターの場所を開くと、そのフォルダが開かれる")
    void opening_the_location_of_a_monitor_with_a_folder_opens_the_folder() throws IOException {
        var folder = Files.createDirectory(tempDir.resolve("photos"));
        var opened = new ArrayList<Path>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, PICK_RANDOM_PNG,
            new NullMonitorSettingsRepository(), opened::add
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(folder));

        viewModel.openLocation(LEFT_MONITOR);

        assertEquals(List.of(folder), opened);
    }

    @Test
    @DisplayName("画像をドロップしたモニターの場所を開くと、画像のあるフォルダが開かれる")
    void opening_the_location_of_a_monitor_with_an_image_opens_the_folder_of_the_image() {
        var opened = new ArrayList<Path>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), opened::add
        );
        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("pictures", "photo.png")));

        viewModel.openLocation(LEFT_MONITOR);

        assertEquals(List.of(Path.of("pictures")), opened);
    }

    @Test
    @DisplayName("フォルダも画像もないモニターの場所を開いても、何も開かれない")
    void opening_the_location_of_a_monitor_without_a_folder_or_an_image_opens_nothing() {
        var opened = new ArrayList<Path>();
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), opened::add
        );

        viewModel.openLocation(LEFT_MONITOR);

        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("フォルダも画像もないモニターは場所を開けず、画像をドロップすると開けるようになる")
    void the_location_can_be_opened_only_after_the_monitor_gets_a_folder_or_an_image() {
        var viewModel = viewModel(
            new NullWallpaperService(), NAMING_COMPOSER, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );
        var canOpen = viewModel.canOpenLocationProperty(LEFT_MONITOR);
        assertEquals(false, canOpen.getValue());

        viewModel.dropFiles(LEFT_MONITOR, List.of(Path.of("pictures", "photo.png")));

        assertEquals(true, canOpen.getValue());
    }

    private static NullMonitorSettingsRepository savingRepository(
        List<Map<String, MonitorSettings>> saved) {
        return new NullMonitorSettingsRepository() {
            @Override
            public void save(Map<String, MonitorSettings> settings) {
                saved.add(settings);
            }
        };
    }

    private static NullMonitorSettingsRepository loadingRepository(
        Map<String, MonitorSettings> settings) {
        return new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return settings;
            }
        };
    }

    @Test
    @DisplayName("ランダム変更すると、フォルダが登録された全モニターの壁紙が、それぞれのフォルダからランダムに選んだ画像になる")
    void randomizing_sets_an_image_picked_from_its_own_folder_on_every_monitor_with_a_folder() {
        var wallpapers = new ArrayList<String>();
        var right = new Monitor("right", 1920, 0, 1080, 1920);
        var viewModel = viewModel(
            recordingService(wallpapers, LEFT_MONITOR, right), NAMING_COMPOSER, PICK_RANDOM_PNG,
            loadingRepository(
                Map.of(
                    "left", folderSettings("left-photos"), "right", folderSettings("right-photos")
                )
            ), new NullFolderOpener()
        );
        viewModel.loadMonitors();

        viewModel.randomizeAll();

        assertEquals(
            List.of(
                "left:random.png@1920x1080-LEFT-WHITE", "right:random.png@1080x1920-LEFT-WHITE"
            ),
            wallpapers
        );
    }

    @Test
    @DisplayName("フォルダが登録されていないモニターの壁紙は、ランダム変更しても変わらない")
    void randomizing_does_not_change_the_wallpaper_of_a_monitor_without_a_folder() {
        var wallpapers = new ArrayList<String>();
        var right = new Monitor("right", 1920, 0, 1080, 1920);
        var viewModel = viewModel(
            recordingService(wallpapers, LEFT_MONITOR, right), NAMING_COMPOSER, PICK_RANDOM_PNG,
            loadingRepository(Map.of("right", folderSettings("right-photos"))),
            new NullFolderOpener()
        );
        viewModel.loadMonitors();

        viewModel.randomizeAll();

        assertEquals(List.of("right:random.png@1080x1920-LEFT-WHITE"), wallpapers);
    }

    @Test
    @DisplayName("ランダムに選んだ画像は、元の画像として保存される")
    void the_image_picked_at_random_is_saved_as_the_source_image() {
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return Map.of("left", folderSettings("photos"));
            }

            @Override
            public void save(Map<String, MonitorSettings> settings) {
                saved.add(settings);
            }
        };
        var viewModel = viewModel(
            recordingService(new ArrayList<>(), LEFT_MONITOR), NAMING_COMPOSER, PICK_RANDOM_PNG,
            repository, new NullFolderOpener()
        );
        viewModel.loadMonitors();

        viewModel.randomizeAll();

        assertEquals(
            Optional.of(Path.of("photos", "random.png")),
            saved.getLast().getOrDefault("left", MonitorSettings.DEFAULT).image()
        );
    }

    @Test
    @DisplayName("ランダム変更すると、変わったモニターの現在の壁紙と元の画像の名前が更新される")
    void randomizing_updates_the_current_wallpaper_and_the_source_name_of_the_changed_monitor() {
        var changer = new NullWallpaperChanger() {
            @Override
            public List<WallpaperChange> randomizeAll() {
                return List.of(
                    new WallpaperChange(
                        "left", Path.of("photos", "cat.png"), Path.of("composed-cat.png")
                    )
                );
            }
        };
        var viewModel = new MonitorsViewModel(
            new NullWallpaperService(), changer, NO_IMAGE_PICKER,
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );

        viewModel.randomizeAll();

        assertEquals(
            Optional.of(Path.of("composed-cat.png")),
            viewModel.wallpaperProperty(LEFT_MONITOR).get()
        );
        assertEquals("cat.png", viewModel.sourceNameProperty(LEFT_MONITOR).getValue());
    }

    private static MonitorSettings folderSettings(String folder) {
        return new MonitorSettings(
            Optional.of(Path.of(folder)), Optional.empty(), Alignment.LEFT, FillColor.WHITE
        );
    }

    private static WallpaperService recordingService(List<String> wallpapers, Monitor... monitors) {
        return new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(monitors);
            }

            @Override
            public void setWallpaper(String monitorId, Path image) {
                wallpapers.add(monitorId + ":" + image);
            }
        };
    }

    private static MonitorsViewModel viewModelWith(Monitor... monitors) {
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(monitors);
            }
        };
        return viewModel(
            service, NAMING_COMPOSER, NO_IMAGE_PICKER, new NullMonitorSettingsRepository(),
            new NullFolderOpener()
        );
    }

    private static WallpaperService recordingService(List<String> wallpapers) {
        return new NullWallpaperService() {
            @Override
            public void setWallpaper(String monitorId, Path image) {
                wallpapers.add(monitorId + ":" + image);
            }
        };
    }

    // Uses the real changer, so the tests see what the view model makes the services do.
    private static MonitorsViewModel viewModel(
        WallpaperService service, ImageComposer composer, ImagePicker picker,
        MonitorSettingsRepository repository, FolderOpener opener) {
        return new MonitorsViewModel(
            service, new ComposingWallpaperChanger(service, composer, picker, repository), picker,
            repository, opener
        );
    }
}
