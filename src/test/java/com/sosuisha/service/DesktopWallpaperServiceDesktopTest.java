package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import module java.base;
import module java.desktop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

// These tests change the real wallpaper of the PC for a moment, so they are
// excluded from the normal build. Run them by hand: mvn test -Pdesktop
@Tag("desktop")
@EnabledOnOs(OS.WINDOWS)
class DesktopWallpaperServiceDesktopTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("FFMで設定した壁紙が、そのモニターの壁紙として読み戻せる")
    void the_wallpaper_set_through_ffm_is_read_back_as_the_wallpaper_of_the_monitor()
        throws IOException {
        var service = new DesktopWallpaperService();
        var monitorId = service.listMonitors().getFirst().id();
        var original = service.getWallpaper(monitorId);
        var image = tempDir.resolve("test-wallpaper.png");
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", image.toFile());

        try {
            service.setWallpaper(monitorId, image);

            assertEquals(Optional.of(image), service.getWallpaper(monitorId));
        } finally {
            original.ifPresent(path -> service.setWallpaper(monitorId, path));
        }
    }
}
