package com.sosuisha.presentation.screens.monitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import module java.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.repository.NullMonitorSettingsRepository;
import com.sosuisha.domain.service.ImagePicker;
import com.sosuisha.domain.service.NullImageComposer;
import com.sosuisha.domain.service.NullWallpaperService;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.service.ComposingWallpaperChanger;

import atlantafx.base.theme.PrimerLight;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@ExtendWith(ApplicationExtension.class)
class MonitorsViewTest {
    private static final Path WALLPAPER = Path.of("wallpapers", "current.png").toAbsolutePath();

    private MonitorsViewModel viewModel;
    private Stage stage;
    private final List<Path> openedFolders = new ArrayList<>();

    @Start
    void setup(Stage stage) {
        // MonitorsView uses looked-up colors of the theme, as App does.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(
                    new Monitor("right", 1920, 0, 1920, 1080),
                    new Monitor("left", 0, 0, 1920, 1080)
                );
            }

            @Override
            public Optional<Path> getWallpaper(String monitorId) {
                return Optional.of(WALLPAPER);
            }
        };
        var repository = new NullMonitorSettingsRepository() {
            @Override
            public Map<String, MonitorSettings> load() {
                return Map.of(
                    "left", new MonitorSettings(
                        Optional.of(Path.of("pictures", "cats")), Optional.empty(), Alignment.LEFT,
                        FillColor.WHITE
                    )
                );
            }
        };
        ImagePicker picker = folder -> Optional.of(folder.resolve("random.png"));
        viewModel = new MonitorsViewModel(
            service,
            new ComposingWallpaperChanger(service, new NullImageComposer(), picker, repository),
            picker, repository, openedFolders::add
        );
        var windowManager = new WindowManager();
        windowManager.registerView(new MonitorsView(viewModel));
        // The injected primary stage is reused across tests and rejects
        // initStyle, so the view gets a fresh stage.
        this.stage = new Stage();
        windowManager.showWindow(MonitorsView.class, this.stage);
        // Loaded after the window is shown, so the frames must follow the list.
        viewModel.loadMonitors();
    }

    @Test
    @DisplayName("ウィンドウは、OSのタイトルバーのないEXTENDEDで表示される")
    void the_window_is_shown_in_the_extended_style_without_the_title_bar_of_the_os() {
        assertEquals(StageStyle.EXTENDED, stage.getStyle());
    }

    @Test
    @DisplayName("HeaderBarに、ウィンドウのタイトルが表示される")
    void the_title_of_the_window_is_shown_in_the_header_bar(FxRobot robot) {
        var headerBar = robot.lookup("#headerBar").queryAs(HeaderBar.class);

        var title = robot.from(headerBar).lookup("#title").queryLabeled();

        assertEquals(stage.getTitle(), title.getText());
    }

    @Test
    @DisplayName("フォルダを登録した枠の「場所を開く」ボタンを押すと、そのフォルダが開かれる")
    void pressing_open_location_of_a_frame_with_a_folder_opens_the_folder(FxRobot robot) {
        var leftFrame = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getFirst();
        var openLocation = robot.from(leftFrame).lookup("#openLocation").queryButton();

        robot.interact(openLocation::fire);

        assertEquals(List.of(Path.of("pictures", "cats")), openedFolders);
    }

    @Test
    @DisplayName("フォルダも画像もない枠の「場所を開く」ボタンは、押せない")
    void open_location_of_a_frame_without_a_folder_or_an_image_is_disabled(FxRobot robot) {
        var rightFrame = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getLast();

        var openLocation = robot.from(rightFrame).lookup("#openLocation").queryButton();

        assertEquals(true, openLocation.isDisabled());
    }

    @Test
    @DisplayName("モニターの数だけ、枠が左から順に表示される")
    void one_frame_for_each_monitor_is_shown_from_left_to_right(FxRobot robot) {
        var frames = robot.lookup("#monitorFrames").queryAs(Pane.class);

        var shownMonitors = frames.getChildren().stream().map(Node::getUserData).toList();
        assertEquals(viewModel.getMonitors(), shownMonitors);
    }

    @Test
    @DisplayName("枠には、左から順に1からの番号が表示される")
    void the_frames_show_numbers_from_1_in_order_from_the_left(FxRobot robot) {
        var frames = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren();

        var numbers = frames.stream()
            .map(frame -> robot.from(frame).lookup("#number").queryLabeled().getText())
            .toList();
        assertEquals(List.of("1", "2"), numbers);
    }

    @Test
    @DisplayName("枠の寄せ方を右寄せに切り替えると、そのモニターの寄せ方が右寄せになる")
    void switching_the_alignment_of_a_frame_to_right_sets_the_alignment_of_the_monitor_to_right(
        FxRobot robot) {
        var firstFrame =
            robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getFirst();
        ComboBox<Alignment> alignment = robot.from(firstFrame).lookup("#alignment").queryComboBox();

        robot.interact(() -> alignment.setValue(Alignment.RIGHT));

        var firstMonitor = viewModel.getMonitors().getFirst();
        assertEquals(Alignment.RIGHT, viewModel.alignmentProperty(firstMonitor).get());
    }

    @Test
    @DisplayName("登録されたフォルダの名前が枠に表示される")
    void the_name_of_the_registered_folder_is_shown_in_the_frame(FxRobot robot) {
        var leftFrame = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getFirst();

        var sourceName = robot.from(leftFrame).lookup("#sourceName").queryLabeled();

        assertEquals("cats", sourceName.getText());
    }

    @Test
    @DisplayName("枠は、ViewModelが計算した大きさで表示される")
    void each_frame_is_shown_in_the_size_that_the_view_model_calculates(FxRobot robot) {
        var frames = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren();

        for (var frame : frames.stream().map(Region.class::cast).toList()) {
            var size = viewModel.frameSize((Monitor) frame.getUserData());
            assertEquals(size.width(), frame.getPrefWidth(), 0.001);
            assertEquals(size.height(), frame.getPrefHeight(), 0.001);
        }
    }

    @Test
    @DisplayName("現在の壁紙が、枠のサムネイルに表示される")
    void the_current_wallpaper_is_shown_as_the_thumbnail_of_the_frame(FxRobot robot) {
        var leftFrame = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getFirst();

        var thumbnail = robot.from(leftFrame).lookup("#thumbnail").queryAs(ImageView.class);

        // Image normalizes "file:///" to "file:/", so the URL is compared as a path.
        assertEquals(WALLPAPER, Path.of(URI.create(thumbnail.getImage().getUrl())));
    }

    @Test
    @DisplayName("ランダム変更ボタンを押すと、全モニターの壁紙がランダムに変わる")
    void clicking_the_randomize_button_changes_the_wallpapers_of_all_monitors_at_random(
        FxRobot robot) {
        robot.clickOn("#randomize");

        var left = viewModel.getMonitors().getFirst();
        assertEquals(
            Optional.of(Path.of("pictures", "cats", "random.png")),
            viewModel.wallpaperProperty(left).get()
        );
    }

    @Test
    @DisplayName("ランダム変更ボタンは、ウィンドウの左右中央に置かれる")
    void the_randomize_button_is_centered_horizontally_in_the_window(FxRobot robot) {
        var button = robot.lookup("#randomize").queryButton();

        var bounds = button.localToScene(button.getBoundsInLocal());
        double sceneCenter = button.getScene().getWidth() / 2;
        assertEquals(sceneCenter, bounds.getCenterX(), 1.0);
    }

    @Test
    @DisplayName("枠の埋める色を黒に切り替えると、そのモニターの埋める色が黒になる")
    void switching_the_fill_color_of_a_frame_to_black_sets_the_fill_color_of_the_monitor_to_black(
        FxRobot robot) {
        var firstFrame =
            robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren().getFirst();
        ComboBox<FillColor> fillColor = robot.from(firstFrame).lookup("#fillColor").queryComboBox();

        robot.interact(() -> fillColor.setValue(FillColor.BLACK));

        var firstMonitor = viewModel.getMonitors().getFirst();
        assertEquals(FillColor.BLACK, viewModel.fillColorProperty(firstMonitor).get());
    }
}
