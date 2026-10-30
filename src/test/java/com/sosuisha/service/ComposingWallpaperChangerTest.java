package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.model.WallpaperChange;
import com.sosuisha.domain.repository.MonitorSettingsRepository;
import com.sosuisha.domain.repository.NullMonitorSettingsRepository;
import com.sosuisha.domain.service.ImageComposer;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.NullWallpaperService;
import com.sosuisha.domain.service.WallpaperService;

class ComposingWallpaperChangerTest {
    private static final Monitor LEFT_MONITOR = new Monitor("left", 0, 0, 1920, 1080);
    private static final Monitor RIGHT_MONITOR = new Monitor("right", 1920, 0, 1080, 1920);

    // Names the composed file after its inputs, so a test can see what was composed.
    private static final ImageComposer NAMING_COMPOSER =
        (source, monitor, alignment, fillColor) -> Path.of(
            source.getFileName() + "@" + monitor.width() + "x" + monitor.height() + "-"
                + alignment + "-" + fillColor
        );

    private static final ImagePicker PICK_RANDOM_PNG =
        folder -> Optional.of(folder.resolve("random.png"));

    @Test
    @DisplayName("壁紙を変えると、モニターの大きさと寄せ方と埋める色で合成した画像が設定され、その画像が返る")
    void changing_sets_and_returns_the_image_composed_for_the_monitor_alignment_and_fill_color() {
        var events = new ArrayList<String>();
        var changer = new ComposingWallpaperChanger(
            recordingService(events), NAMING_COMPOSER, PICK_RANDOM_PNG,
            new NullMonitorSettingsRepository()
        );

        var wallpaper =
            changer.change(LEFT_MONITOR, Path.of("photo.jpg"), Alignment.RIGHT, FillColor.BLACK);

        assertEquals(Path.of("photo.jpg@1920x1080-RIGHT-BLACK"), wallpaper);
        assertEquals(List.of("set left:photo.jpg@1920x1080-RIGHT-BLACK"), events);
    }

    @Test
    @DisplayName("ランダム変更すると、フォルダが登録された全モニターの壁紙が、左から順に、それぞれのフォルダから選んだ画像になる")
    void randomizing_sets_an_image_from_its_own_folder_on_every_monitor_from_left_to_right() {
        var events = new ArrayList<String>();
        var changer = new ComposingWallpaperChanger(
            recordingService(events, RIGHT_MONITOR, LEFT_MONITOR), NAMING_COMPOSER,
            PICK_RANDOM_PNG,
            recordingRepository(
                events,
                Map.of(
                    "left", folderSettings("left-photos", Alignment.LEFT),
                    "right", folderSettings("right-photos", Alignment.RIGHT)
                )
            )
        );

        changer.randomizeAll();

        assertEquals(
            List.of(
                "set left:random.png@1920x1080-LEFT-WHITE",
                "set right:random.png@1080x1920-RIGHT-WHITE"
            ),
            events.stream().filter(event -> event.startsWith("set")).toList()
        );
    }

    @Test
    @DisplayName("フォルダが登録されていないモニターの壁紙は、ランダム変更しても変わらない")
    void randomizing_does_not_change_the_wallpaper_of_a_monitor_without_a_folder() {
        var events = new ArrayList<String>();
        var changer = new ComposingWallpaperChanger(
            recordingService(events, LEFT_MONITOR, RIGHT_MONITOR), NAMING_COMPOSER,
            PICK_RANDOM_PNG,
            recordingRepository(
                events, Map.of("right", folderSettings("right-photos", Alignment.LEFT))
            )
        );

        changer.randomizeAll();

        assertEquals(
            List.of("set right:random.png@1080x1920-LEFT-WHITE"),
            events.stream().filter(event -> event.startsWith("set")).toList()
        );
    }

    @Test
    @DisplayName("ランダムに選んだ画像は、壁紙を変える前に、元の画像として保存される")
    void the_picked_image_is_saved_as_the_source_image_before_the_wallpaper_changes() {
        var events = new ArrayList<String>();
        var saved = new ArrayList<Map<String, MonitorSettings>>();
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return Map.of("left", folderSettings("photos", Alignment.LEFT));
            }

            @Override
            public void save(Map<String, MonitorSettings> settings) {
                events.add("save");
                saved.add(settings);
            }
        };
        var changer = new ComposingWallpaperChanger(
            recordingService(events, LEFT_MONITOR), NAMING_COMPOSER, PICK_RANDOM_PNG, repository
        );

        changer.randomizeAll();

        assertEquals(List.of("save", "set left:random.png@1920x1080-LEFT-WHITE"), events);
        assertEquals(
            new MonitorSettings(
                Optional.of(Path.of("photos")), Optional.of(Path.of("photos", "random.png")),
                Alignment.LEFT, FillColor.WHITE
            ),
            saved.getLast().get("left")
        );
    }

    @Test
    @DisplayName("ランダム変更の結果として、モニターごとに元の画像と設定した壁紙が返る")
    void randomizing_returns_the_source_image_and_the_wallpaper_of_each_monitor() {
        var changer = new ComposingWallpaperChanger(
            recordingService(new ArrayList<>(), LEFT_MONITOR), NAMING_COMPOSER, PICK_RANDOM_PNG,
            recordingRepository(
                new ArrayList<>(), Map.of("left", folderSettings("photos", Alignment.LEFT))
            )
        );

        var changes = changer.randomizeAll();

        assertEquals(
            List.of(
                new WallpaperChange(
                    "left", Path.of("photos", "random.png"),
                    Path.of("random.png@1920x1080-LEFT-WHITE")
                )
            ),
            changes
        );
    }

    @Test
    @DisplayName("どのフォルダからも画像を選べなかったとき、設定は保存されず、壁紙も変わらない")
    void when_no_image_is_picked_nothing_is_saved_and_no_wallpaper_changes() {
        var events = new ArrayList<String>();
        var changer = new ComposingWallpaperChanger(
            recordingService(events, LEFT_MONITOR), NAMING_COMPOSER, _ -> Optional.empty(),
            recordingRepository(events, Map.of("left", folderSettings("empty", Alignment.LEFT)))
        );

        var changes = changer.randomizeAll();

        assertEquals(List.of(), changes);
        assertEquals(List.of(), events);
    }

    private static MonitorSettings folderSettings(String folder, Alignment alignment) {
        return new MonitorSettings(
            Optional.of(Path.of(folder)), Optional.empty(), alignment, FillColor.WHITE
        );
    }

    private static WallpaperService recordingService(List<String> events, Monitor... monitors) {
        return new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(monitors);
            }

            @Override
            public void setWallpaper(String monitorId, Path image) {
                events.add("set " + monitorId + ":" + image);
            }
        };
    }

    private static MonitorSettingsRepository recordingRepository(
        List<String> events, Map<String, MonitorSettings> settings) {
        return new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return settings;
            }

            @Override
            public void save(Map<String, MonitorSettings> saved) {
                events.add("save");
            }
        };
    }
}
