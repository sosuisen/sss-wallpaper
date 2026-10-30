package com.sosuisha.presentation.screens.alert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import io.github.sosuisen.jfxbuilder.controls.AlertBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.TextAreaBuilder;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Helper that shows common alert dialogs.
 */
public final class AlertDialog {
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
        show("An error occurred", messageLabel(message));
    }

    /**
     * Shows an error alert dialog with the given message, and runs an action
     * when the dialog is closed. Otherwise it is the same as
     * {@link #showError(String)}.
     *
     * @param message  message shown in the dialog
     * @param onClosed action that runs when the dialog is closed
     * @throws NullPointerException if message or onClosed is null
     */
    public static void showError(String message, Runnable onClosed) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(onClosed, "onClosed must not be null");
        show("An error occurred", messageLabel(message)).setOnHidden(_ -> onClosed.run());
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
        show(
            "An unexpected error occurred",
            TextAreaBuilder.create().text(stackTrace.toString()).editable(false).build()
        );
    }

    private static Node messageLabel(String message) {
        return LabelBuilder.create().text(message).wrapText(true).build();
    }

    private static Alert show(String headerText, Node details) {
        var alert = AlertBuilder.create(AlertType.ERROR)
            .title("Error")
            .headerText(headerText)
            .apply(a -> {
                a.getDialogPane().setExpandableContent(details);
                a.getDialogPane().setExpanded(true);
            })
            .build();
        alert.show();
        return alert;
    }
}
