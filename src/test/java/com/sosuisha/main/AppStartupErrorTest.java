package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppStartupErrorTest {
    private Stage stage;

    @Start
    void setup(Stage stage) throws IOException {
        // A folder in place of the settings file makes reading it fail.
        var folder = Files.createTempDirectory("sss-wallpaper-test");
        var settingsFile = Files.createDirectory(folder.resolve("settings.properties"));
        System.setProperty(App.SETTINGS_FILE_PROPERTY, settingsFile.toString());
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
    @DisplayName("起動時にモニター一覧の読み込みに失敗すると、エラーダイアログが表示され、ウィンドウは開いたままである")
    void a_failure_to_load_the_monitors_at_startup_shows_an_error_dialog_and_keeps_the_window_open(
        FxRobot robot) {
        var pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        var content = assertInstanceOf(Label.class, pane.getExpandableContent());
        assertTrue(content.getText().startsWith("Cannot read the settings file"));
        assertTrue(stage.isShowing());
    }
}
