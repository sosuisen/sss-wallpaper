package com.sosuisha.presentation.screens.monitors;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.IntStream;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;
import com.sosuisha.presentation.View;

import atlantafx.base.theme.Styles;

import io.github.sosuisen.jfxbuilder.controls.ButtonBuilder;
import io.github.sosuisen.jfxbuilder.controls.ComboBoxBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.graphics.HBoxBuilder;
import io.github.sosuisen.jfxbuilder.graphics.ImageViewBuilder;
import io.github.sosuisen.jfxbuilder.graphics.SceneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.StackPaneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.VBoxBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * View that shows the frames of all monitors.
 */
public class MonitorsView implements View {
    private static final String TITLE = "SSS Wallpaper";
    private static final double WIDTH = 1000;
    private static final double HEIGHT = 400;
    private static final String LABEL_STYLE =
        "-fx-background-color: rgba(255, 255, 255, 0.75); -fx-padding: 2 6;";

    private final MonitorsViewModel viewModel;
    private final Scene scene;

    /**
     * Creates the view.
     *
     * @param viewModel view model of the monitors screen
     * @throws NullPointerException if viewModel is null
     */
    public MonitorsView(MonitorsViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel must not be null");
        this.scene = buildSceneGraph();
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    private Scene buildSceneGraph() {
        var frames = HBoxBuilder.create()
            .id("monitorFrames")
            .spacing(10)
            .build();
        viewModel.getMonitors().subscribe(() -> showFrames(frames));
        var randomize = ButtonBuilder.create()
            .id("randomize")
            .text("Randomize all")
            .addStyleClass(Styles.ACCENT)
            .onAction(_ -> viewModel.randomizeAll())
            .build();
        return SceneBuilder
            .create(
                VBoxBuilder.withChildren(randomize, frames)
                    .alignment(Pos.TOP_CENTER)
                    .spacing(10)
                    .padding(new Insets(10))
                    .build(),
                WIDTH, HEIGHT
            )
            .build();
    }

    private void showFrames(HBox frames) {
        var monitors = viewModel.getMonitors();
        frames.getChildren().setAll(
            IntStream.range(0, monitors.size())
                .mapToObj(i -> frame(monitors.get(i), i + 1))
                .toList()
        );
    }

    private Node frame(Monitor monitor, int number) {
        var size = viewModel.frameSize(monitor);
        return StackPaneBuilder.withChildren(
            ImageViewBuilder.create()
                .id("thumbnail")
                .fitWidth(size.width())
                .fitHeight(size.height())
                .preserveRatio(true)
                .apply(thumbnail -> showWallpaper(thumbnail, monitor, size))
                .build(),
            LabelBuilder.create().text(String.valueOf(number)).style(LABEL_STYLE).build(),
            LabelBuilder.create()
                .id("folder")
                .style(LABEL_STYLE)
                .textPropertyApply(
                    text -> text.bind(
                        viewModel.folderProperty(monitor)
                            .map(folder -> folder.map(MonitorsView::folderName).orElse(""))
                    )
                )
                .alignmentInStackPane(Pos.TOP_CENTER)
                .build(),
            // Stacked, because a frame of a portrait monitor is too narrow
            // for two combo boxes side by side.
            VBoxBuilder.withChildren(
                ComboBoxBuilder.<Alignment>create()
                    .id("alignment")
                    .apply(comboBox -> comboBox.getItems().addAll(Alignment.values()))
                    .valuePropertyApply(
                        prop -> prop.bindBidirectional(viewModel.alignmentProperty(monitor))
                    )
                    .build(),
                ComboBoxBuilder.<FillColor>create()
                    .id("fillColor")
                    .apply(comboBox -> comboBox.getItems().addAll(FillColor.values()))
                    .valuePropertyApply(
                        prop -> prop.bindBidirectional(viewModel.fillColorProperty(monitor))
                    )
                    .build()
            )
                .alignment(Pos.BOTTOM_CENTER)
                .spacing(4)
                .padding(new Insets(4))
                // Without it, the box covers the whole frame and the thumbnail.
                .apply(box -> box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE))
                .alignmentInStackPane(Pos.BOTTOM_CENTER)
                .build()
        )
            .prefWidth(size.width())
            .prefHeight(size.height())
            // The frame keeps the shape of the monitor, so the HBox must not stretch it.
            .apply(frame -> frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE))
            .style("-fx-border-color: -color-border-default; -fx-border-width: 2;")
            .userData(monitor)
            .onDragOver(MonitorsView::acceptFiles)
            .onDragDropped(event -> dropFiles(monitor, event))
            .build();
    }

    private void showWallpaper(ImageView thumbnail, Monitor monitor, FrameSize size) {
        viewModel.wallpaperProperty(monitor).subscribe(
            wallpaper -> thumbnail.setImage(
                wallpaper
                    // Loaded in the background at the frame size, because a
                    // wallpaper of a 4K monitor is large.
                    .map(
                        path -> new Image(
                            path.toUri().toString(), size.width(), size.height(), true, true, true
                        )
                    )
                    .orElse(null)
            )
        );
    }

    private static String folderName(Path folder) {
        var name = folder.getFileName();
        // A drive root such as C:\ has no file name.
        return name == null ? folder.toString() : name.toString();
    }

    private static void acceptFiles(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void dropFiles(Monitor monitor, DragEvent event) {
        var dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            viewModel.dropFiles(monitor, dragboard.getFiles().stream().map(File::toPath).toList());
        }
        event.setDropCompleted(dragboard.hasFiles());
        event.consume();
    }
}
