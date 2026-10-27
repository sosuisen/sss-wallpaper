package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.MonitorSettings;

class PropertiesMonitorSettingsRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("保存したモニターごとの設定（フォルダ・元の画像・寄せ方）は、読み込むと同じ内容で戻る")
    void saved_settings_of_each_monitor_are_loaded_back_with_the_same_content() {
        var repository =
            new PropertiesMonitorSettingsRepository(tempDir.resolve("sub/settings.properties"));
        // Real monitor ids are device paths with backslashes, '#' and '{'.
        var settings = Map.of(
            "\\\\?\\DISPLAY#GSM5B09#4&1d2c3b4&0&UID4352#{e6f07b5f-ee97-4a90-b076-33f57bf4eaa7}",
            new MonitorSettings(
                Optional.of(tempDir.resolve("写真")), Optional.of(tempDir.resolve("写真/cat.jpg")),
                Alignment.RIGHT, FillColor.WHITE
            ),
            "second",
            new MonitorSettings(Optional.empty(), Optional.empty(), Alignment.LEFT, FillColor.WHITE)
        );

        repository.save(settings);

        assertEquals(settings, repository.load());
    }

    @Test
    @DisplayName("設定ファイルがないとき、モニターごとの設定は空である")
    void the_settings_of_the_monitors_are_empty_when_the_settings_file_does_not_exist() {
        var repository =
            new PropertiesMonitorSettingsRepository(tempDir.resolve("missing.properties"));

        assertEquals(Map.of(), repository.load());
    }

    @Test
    @DisplayName("保存した埋める色は、読み込むと同じ色で戻る")
    void the_saved_fill_color_is_loaded_back_as_the_same_color() {
        var repository =
            new PropertiesMonitorSettingsRepository(tempDir.resolve("settings.properties"));
        var settings = Map.of(
            "first",
            new MonitorSettings(Optional.empty(), Optional.empty(), Alignment.LEFT, FillColor.BLACK)
        );

        repository.save(settings);

        assertEquals(settings, repository.load());
    }

    @Test
    @DisplayName("埋める色が保存されていない設定ファイルでは、埋める色は白である")
    void the_fill_color_is_white_in_a_settings_file_without_it() throws IOException {
        var file = tempDir.resolve("settings.properties");
        Files.writeString(file, "first.alignment=RIGHT\n");
        var repository = new PropertiesMonitorSettingsRepository(file);

        assertEquals(
            Map.of(
                "first", new MonitorSettings(
                    Optional.empty(), Optional.empty(), Alignment.RIGHT, FillColor.WHITE
                )
            ),
            repository.load()
        );
    }
}
