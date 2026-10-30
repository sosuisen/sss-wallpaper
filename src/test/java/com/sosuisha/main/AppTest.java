package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import module java.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sosuisha.presentation.screens.monitors.MonitorsView;

import javafx.application.Application;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppTest {
    private Stage stage;

    @Start
    void setup(Stage stage) throws IOException {
        // The real settings file of the user must not be read in tests.
        var folder = Files.createTempDirectory("sss-wallpaper-test");
        System.setProperty(
            App.SETTINGS_FILE_PROPERTY, folder.resolve("settings.properties").toString()
        );
        // The injected primary stage is reused across tests and rejects
        // initStyle, so App gets a fresh stage.
        this.stage = new Stage();
        new App().start(this.stage);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(App.SETTINGS_FILE_PROPERTY);
    }

    @Test
    @DisplayName("アプリを起動すると、FIRST_VIEW定数で指定したViewのウィンドウが表示される")
    void app_startup_shows_the_window_of_the_view_specified_by_first_view_constant() {
        var expectedTitles = Map.of(MonitorsView.class, "SSS Wallpaper");

        assertTrue(stage.isShowing());
        assertEquals(expectedTitles.get(App.FIRST_VIEW), stage.getTitle());
    }

    @Test
    @DisplayName("起動時にAtlantaFXのPrimer Lightテーマが適用されている")
    void the_atlantafx_primer_light_theme_is_applied_at_startup() {
        var stylesheet = Application.getUserAgentStylesheet();

        assertNotNull(stylesheet);
        assertTrue(stylesheet.endsWith("primer-light.css"));
    }
}
