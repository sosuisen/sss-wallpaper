package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.sosuisha.domain.repository.NullMonitorSettingsRepository;
import com.sosuisha.domain.service.NullImageComposer;
import com.sosuisha.domain.service.NullWallpaperService;
import com.sosuisha.presentation.screens.monitors.MonitorsViewModel;

class LauncherTest {
    @Test
    @DisplayName("「--randomize」オプションがあるとき、ウィンドウなしで起動する")
    void it_starts_without_a_window_when_the_randomize_option_is_given() {
        assertTrue(Launcher.isWindowless(new String[] {"--randomize"}));
    }

    @Test
    @DisplayName("オプションがないとき、ウィンドウありで起動する")
    void it_starts_with_a_window_when_no_option_is_given() {
        assertFalse(Launcher.isWindowless(new String[0]));
    }

    @Test
    @DisplayName("ウィンドウなしの起動では、モニター一覧と設定を読み込んでから、全モニターの壁紙を登録フォルダからランダムに変える")
    void starting_without_a_window_loads_the_monitors_and_settings_and_randomizes_all_wallpapers() {
        var wallpapers = new ArrayList<String>();
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(new Monitor("left", 0, 0, 1920, 1080));
            }

            @Override
            public void setWallpaper(String monitorId, Path image) {
                wallpapers.add(monitorId + ":" + image);
            }
        };
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return Map.of(
                    "left",
                    new MonitorSettings(
                        Optional.of(Path.of("photos")), Optional.empty(), Alignment.LEFT,
                        FillColor.WHITE
                    )
                );
            }
        };
        var viewModel = new MonitorsViewModel(
            service, new NullImageComposer(), folder -> Optional.of(folder.resolve("random.png")),
            repository
        );

        Launcher.randomizeWithoutWindow(viewModel);

        assertEquals(List.of("left:" + Path.of("photos", "random.png")), wallpapers);
    }
}
