package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import module java.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.domain.exception.WallpaperException;

import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppErrorTest {
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
    @DisplayName("FXスレッドで回復できないエラーが起きると、そのメッセージのエラーダイアログが表示され、ウィンドウは開いたままである")
    void an_unrecoverable_error_on_the_fx_thread_shows_its_message_and_keeps_the_window_open(
        FxRobot robot) {
        Platform.runLater(() -> {
            throw new WallpaperException("Cannot set the wallpaper.");
        });
        WaitForAsyncUtils.waitForFxEvents();

        var pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        var content = assertInstanceOf(Label.class, pane.getExpandableContent());
        assertEquals("Cannot set the wallpaper.", content.getText());
        assertTrue(stage.isShowing());
    }

    @Test
    @DisplayName("FXスレッドで想定外の例外が起きると、その内容を含むエラーダイアログが表示される")
    void an_unexpected_exception_on_the_fx_thread_shows_an_error_dialog_with_the_exception(
        FxRobot robot) {
        Platform.runLater(() -> {
            throw new IllegalStateException("boom");
        });
        WaitForAsyncUtils.waitForFxEvents();

        var pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        var details = assertInstanceOf(TextArea.class, pane.getExpandableContent());
        assertTrue(details.getText().contains("IllegalStateException: boom"));
    }
}
