package com.sosuisha.presentation;

import module java.base;

import javafx.scene.Node;
import javafx.scene.input.TransferMode;

/**
 * Helper that lets a node accept the files dropped from the OS, such as
 * Explorer. The tests cannot make the drag events with files, so this class
 * keeps the handling of the events small and out of the views.
 */
public final class FileDrop {
    private FileDrop() {}

    /**
     * Makes the node accept dropped files and passes them to the handler.
     *
     * @param node    node that accepts the files
     * @param handler handler that receives the paths of the dropped files
     * @throws NullPointerException if node or handler is null
     */
    public static void install(Node node, Consumer<List<Path>> handler) {
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        node.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        node.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                handler.accept(dragboard.getFiles().stream().map(File::toPath).toList());
            }
            event.setDropCompleted(dragboard.hasFiles());
            event.consume();
        });
    }
}
