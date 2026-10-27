package com.sosuisha.presentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.stage.Stage;

/**
 * Manages views and shows them in windows.
 */
public class WindowManager {
    private final Map<Class<? extends View>, View> views = new HashMap<>();

    /**
     * Registers a view.
     *
     * @param view view to register
     * @throws NullPointerException if view is null
     */
    public void registerView(View view) {
        Objects.requireNonNull(view, "view must not be null");
        views.put(view.getClass(), view);
    }

    /**
     * Returns the registered view of the given class.
     *
     * @param <T>       type of the view
     * @param viewClass class of the view to get
     * @return the registered view
     * @throws NullPointerException     if viewClass is null
     * @throws IllegalArgumentException if no view of viewClass is registered
     */
    public <T extends View> T getView(Class<T> viewClass) {
        Objects.requireNonNull(viewClass, "viewClass must not be null");
        var view = views.get(viewClass);
        if (view == null) {
            throw new IllegalArgumentException("view is not registered: " + viewClass.getName());
        }
        return viewClass.cast(view);
    }

    /**
     * Shows the registered view of the given class in the given stage.
     *
     * @param viewClass class of the view to show
     * @param stage     stage to show the view in
     * @throws NullPointerException     if viewClass or stage is null
     * @throws IllegalArgumentException if no view of viewClass is registered
     */
    public void showWindow(Class<? extends View> viewClass, Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        var view = getView(viewClass);
        stage.initStyle(view.stageStyle());
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }
}
