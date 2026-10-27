package com.sosuisha.presentation;

import javafx.scene.Scene;
import javafx.stage.StageStyle;

/**
 * A view that provides a scene and a window title.
 */
public interface View {
    /**
     * Returns the scene of this view.
     *
     * @return scene of this view
     */
    Scene getScene();

    /**
     * Returns the window title of this view.
     *
     * @return window title
     */
    String getTitle();

    /**
     * Returns the style of the window that shows this view. The default is
     * {@link StageStyle#DECORATED}.
     *
     * @return style of the window of this view
     */
    default StageStyle stageStyle() {
        return StageStyle.DECORATED;
    }
}
