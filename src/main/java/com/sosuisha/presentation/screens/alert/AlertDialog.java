package com.sosuisha.presentation.screens.alert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import io.github.sosuisen.jfxbuilder.controls.AlertBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.TextAreaBuilder;
import javafx.scene.control.Alert.AlertType;

/**
 * Helper that shows common alert dialogs.
 */
public class AlertDialog {
    private AlertDialog() {}

    /**
     * Shows an error alert dialog with the given message. The dialog is shown
     * without blocking the caller. The message is placed in the expandable
     * content of the dialog, which is expanded at first, so that the user can
     * resize the dialog to read a long message.
     *
     * @param message message shown in the dialog
     * @throws NullPointerException if message is null
     */
    public static void showError(String message) {
        Objects.requireNonNull(message, "message must not be null");
        AlertBuilder.create(AlertType.ERROR)
            .title("Error")
            .headerText("An error occurred")
            .apply(alert -> {
                alert.getDialogPane()
                    .setExpandableContent(
                        LabelBuilder.create().text(message).wrapText(true).build()
                    );
                alert.getDialogPane().setExpanded(true);
            })
            .build()
            .show();
    }

    /**
     * Shows an error alert dialog for an exception that the application did
     * not expect. The dialog is shown without blocking the caller. The
     * exception and its stack trace are placed in a read-only text area in
     * the expandable content of the dialog, which is expanded at first, so
     * that the user can copy them into a bug report.
     *
     * @param exception exception that was not expected
     * @throws NullPointerException if exception is null
     */
    public static void showUnexpectedError(Throwable exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        var stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        AlertBuilder.create(AlertType.ERROR)
            .title("Error")
            .headerText("An unexpected error occurred")
            .apply(alert -> {
                alert.getDialogPane()
                    .setExpandableContent(
                        TextAreaBuilder.create().text(stackTrace.toString()).editable(false).build()
                    );
                alert.getDialogPane().setExpanded(true);
            })
            .build()
            .show();
    }
}
