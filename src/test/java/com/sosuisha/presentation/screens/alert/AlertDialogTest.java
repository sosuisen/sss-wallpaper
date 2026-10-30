package com.sosuisha.presentation.screens.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import atlantafx.base.theme.PrimerLight;

import javafx.application.Application;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AlertDialogTest {
    @Start
    void setup(Stage stage) {
        // Initializes the JavaFX toolkit. The dialog is shown by each test.
        // The theme resolves the AtlantaFX color names that the dialog uses.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    @Test
    @DisplayName("エラーダイアログのメッセージは、展開可能な領域に表示され、初期状態で展開されている")
    void the_message_of_the_error_dialog_is_shown_in_the_expandable_content_and_expanded(
        FxRobot robot) {
        robot.interact(() -> AlertDialog.showError("The settings file cannot be read."));

        var pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        assertTrue(pane.isExpanded());
        var content = assertInstanceOf(Label.class, pane.getExpandableContent());
        assertEquals("The settings file cannot be read.", content.getText());
    }

    @Test
    @DisplayName("エラーダイアログを閉じると、閉じたときの処理が呼ばれる")
    void closing_the_error_dialog_runs_the_action_for_the_close(FxRobot robot) {
        var closed = new AtomicBoolean(false);
        robot.interact(
            () -> AlertDialog.showError("The folder cannot be read.", () -> closed.set(true))
        );

        robot.clickOn("OK");

        assertTrue(closed.get());
    }

    @Test
    @DisplayName("想定外のエラーのダイアログには、例外のスタックトレースが読み取り専用で表示され、初期状態で展開されている")
    void the_unexpected_error_dialog_shows_the_stack_trace_read_only_and_expanded(FxRobot robot) {
        var exception = new IllegalStateException("boom");

        robot.interact(() -> AlertDialog.showUnexpectedError(exception));

        var pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        assertTrue(pane.isExpanded());
        var details = assertInstanceOf(TextArea.class, pane.getExpandableContent());
        assertFalse(details.isEditable());
        assertTrue(details.getText().startsWith("java.lang.IllegalStateException: boom"));
        assertTrue(details.getText().contains("AlertDialogTest"));
    }
}
