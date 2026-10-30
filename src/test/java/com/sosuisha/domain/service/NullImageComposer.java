package com.sosuisha.domain.service;

import java.nio.file.Path;

import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;

/**
 * An image composer that composes nothing and returns the source as it is.
 */
public class NullImageComposer implements ImageComposer {
    @Override
    public Path compose(Path source, Monitor monitor, Alignment alignment, FillColor fillColor) {
        return source;
    }
}
