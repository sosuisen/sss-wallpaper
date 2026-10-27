package com.sosuisha.main;

import java.util.Arrays;
import java.util.Objects;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.presentation.screens.monitors.MonitorsViewModel;

import javafx.application.Application;

/**
 * Entry point of the application.
 * This launcher class starts the JavaFX runtime from the classpath.
 */
public class Launcher {
    /** Option that randomizes the wallpapers without a window and exits. */
    static final String RANDOMIZE_OPTION = "--randomize";

    /**
     * Application entry point. With {@value #RANDOMIZE_OPTION}, it changes the
     * wallpapers at random and exits without showing a window.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (!isWindowless(args)) {
            Application.launch(App.class, args);
            return;
        }
        try {
            randomizeWithoutWindow(App.createMonitorsViewModel());
        } catch (UnrecoverableException e) {
            // No window shows the error, so the exit code tells a scheduler that it failed.
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Returns whether the application starts without a window.
     *
     * @param args command line arguments
     * @return true if the arguments have {@value #RANDOMIZE_OPTION}
     * @throws NullPointerException if args is null
     */
    static boolean isWindowless(String[] args) {
        Objects.requireNonNull(args, "args must not be null");
        return Arrays.asList(args).contains(RANDOMIZE_OPTION);
    }

    /**
     * Loads the monitors and the settings, then changes the wallpaper of every
     * monitor to an image picked at random from its registered folder.
     *
     * @param viewModel view model that does the work without a view
     * @throws NullPointerException if viewModel is null
     */
    static void randomizeWithoutWindow(MonitorsViewModel viewModel) {
        Objects.requireNonNull(viewModel, "viewModel must not be null");
        viewModel.loadMonitors();
        viewModel.randomizeAll();
    }
}
