package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import module java.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.WallpaperChange;
import com.sosuisha.domain.service.NullWallpaperChanger;

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
    @DisplayName("ウィンドウなしの起動では、全モニターの壁紙を登録フォルダからランダムに変える")
    void starting_without_a_window_randomizes_all_wallpapers() {
        var randomized = new ArrayList<String>();
        var changer = new NullWallpaperChanger() {
            @Override
            public List<WallpaperChange> randomizeAll() {
                randomized.add("all");
                return List.of();
            }
        };

        Launcher.randomizeWithoutWindow(changer);

        assertEquals(List.of("all"), randomized);
    }
}
