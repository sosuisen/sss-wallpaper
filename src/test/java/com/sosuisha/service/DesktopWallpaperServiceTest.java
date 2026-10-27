package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

// These tests call the real IDesktopWallpaper of Windows. They only read the
// state of the PC, so they do not change the wallpaper of the user.
@EnabledOnOs(OS.WINDOWS)
class DesktopWallpaperServiceTest {
    @Test
    @DisplayName("実機のモニター一覧を、FFMで1件以上取得できる")
    void at_least_one_monitor_of_the_real_pc_is_listed_through_ffm() {
        var service = new DesktopWallpaperService();

        var monitors = service.listMonitors();

        assertFalse(monitors.isEmpty());
    }
}
