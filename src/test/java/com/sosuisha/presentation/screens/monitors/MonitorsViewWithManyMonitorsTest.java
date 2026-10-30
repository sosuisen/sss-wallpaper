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
import com.sosuisha.domain.repository.NullMonitorSettingsRepository;
import com.sosuisha.domain.service.NullFolderOpener;
import com.sosuisha.domain.service.NullWallpaperChanger;
import com.sosuisha.domain.service.NullWallpaperService;
import com.sosuisha.presentation.WindowManager;

import atlantafx.base.theme.PrimerLight;

import javafx.application.Application;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class MonitorsViewWithManyMonitorsTest {
    @Start
    void setup(Stage stage) {
        // MonitorsView uses looked-up colors of the theme, as App does.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var service = new NullWallpaperService() {
            @Override
            public List<Monitor> listMonitors() {
                return List.of(
                    new Monitor("a", 0, 0, 3840, 2160),
                    new Monitor("b", 3840, 0, 3840, 2160),
                    new Monitor("c", 7680, 0, 3840, 2160)
                );
            }
        };
        var viewModel = new MonitorsViewModel(
            service, new NullWallpaperChanger(), _ -> Optional.empty(),
            new NullMonitorSettingsRepository(), new NullFolderOpener()
        );
        var windowManager = new WindowManager();
        windowManager.registerView(new MonitorsView(viewModel));
        // The injected primary stage is reused across tests and rejects
        // initStyle, so the view gets a fresh stage.
        windowManager.showWindow(MonitorsView.class, new Stage());
        viewModel.loadMonitors();
    }

    @Test
    @DisplayName("モニターが多いとき、枠は合計幅が決まった幅に収まる大きさで表示される")
    void with_many_monitors_the_frames_are_shown_in_sizes_that_fit_in_the_fixed_width(
        FxRobot robot) {
        var frames = robot.lookup("#monitorFrames").queryAs(Pane.class).getChildren();

        double totalWidth = frames.stream().mapToDouble(f -> ((Region) f).getPrefWidth()).sum();

        assertEquals(MonitorsViewModel.MAX_FRAMES_WIDTH, totalWidth, 0.001);
    }
}
