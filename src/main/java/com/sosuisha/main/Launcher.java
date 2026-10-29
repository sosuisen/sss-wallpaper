package com.sosuisha.main;

import java.util.Arrays;
import java.util.Objects;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.domain.service.WallpaperChanger;

import javafx.application.Application;

/**
 * Entry point of the application.
 * This launcher class starts the JavaFX runtime from the classpath.
 */
public class Launcher {
    /** Option that randomizes the wallpapers without a window and exits. */
    static final String RANDOMIZE_OPTION = "--randomize";

    private Launcher() {}

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
            randomizeWithoutWindow(App.createWallpaperChanger());
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
     * Changes the wallpaper of every monitor to an image picked at random from
     * its registered folder.
     *
     * @param wallpaperChanger changer that does the work without a window
     * @throws NullPointerException if wallpaperChanger is null
     */
    static void randomizeWithoutWindow(WallpaperChanger wallpaperChanger) {
        Objects.requireNonNull(wallpaperChanger, "wallpaperChanger must not be null");
        wallpaperChanger.randomizeAll();
    }
}
