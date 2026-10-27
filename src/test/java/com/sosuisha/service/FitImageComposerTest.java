package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.exception.ImageComposeException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.Monitor;

class FitImageComposerTest {
    private static final int RED = Color.RED.getRGB();
    private static final int WHITE = Color.WHITE.getRGB();
    private static final int BLACK = Color.BLACK.getRGB();
    private static final Monitor WIDE_MONITOR = new Monitor("wide", 0, 0, 200, 100);

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("縦長の画像を横長のモニターへ左寄せでFitすると、画像は左端に置かれ、残りは白になる")
    void a_tall_image_fitted_left_on_a_wide_monitor_is_placed_at_the_left_edge_with_white_elsewhere() {
        var fitted =
            FitImageComposer.fit(redImage(50, 100), 200, 100, Alignment.LEFT, FillColor.WHITE);

        assertEquals(RED, fitted.getRGB(0, 50));
        assertEquals(RED, fitted.getRGB(48, 50));
        assertEquals(WHITE, fitted.getRGB(52, 50));
        assertEquals(WHITE, fitted.getRGB(199, 50));
    }

    @Test
    @DisplayName("右寄せでFitすると、画像は右端に置かれ、残りは白になる")
    void an_image_fitted_right_is_placed_at_the_right_edge_with_white_elsewhere() {
        var fitted =
            FitImageComposer.fit(redImage(50, 100), 200, 100, Alignment.RIGHT, FillColor.WHITE);

        assertEquals(WHITE, fitted.getRGB(0, 50));
        assertEquals(WHITE, fitted.getRGB(147, 50));
        assertEquals(RED, fitted.getRGB(151, 50));
        assertEquals(RED, fitted.getRGB(199, 50));
    }

    @Test
    @DisplayName("横長の画像を縦長のモニターへFitすると、縦横比を保って上下中央に置かれる")
    void a_wide_image_fitted_on_a_tall_monitor_keeps_its_aspect_ratio_and_is_centered_vertically() {
        var fitted =
            FitImageComposer.fit(redImage(100, 50), 100, 200, Alignment.LEFT, FillColor.WHITE);

        assertEquals(WHITE, fitted.getRGB(50, 73));
        assertEquals(RED, fitted.getRGB(50, 77));
        assertEquals(RED, fitted.getRGB(50, 123));
        assertEquals(WHITE, fitted.getRGB(50, 127));
    }

    @Test
    @DisplayName("合成した画像は、モニターと同じ大きさのPNGファイルとして保存される")
    void the_composed_image_is_saved_as_a_png_file_of_the_monitor_size() throws IOException {
        var composer = new FitImageComposer(tempDir.resolve("out"));

        var composed =
            composer.compose(
                writeRedPng("source.jpg", 50, 100), WIDE_MONITOR, Alignment.LEFT, FillColor.WHITE
            );

        var image = ImageIO.read(composed.toFile());
        assertEquals(200, image.getWidth());
        assertEquals(100, image.getHeight());
        assertEquals("png", composed.getFileName().toString().replaceAll(".*\\.", ""));
    }

    @Test
    @DisplayName("同じモニターの古い合成画像は、新しい画像を作るときに削除される")
    void the_old_composed_image_of_the_same_monitor_is_deleted_when_a_new_one_is_made()
        throws IOException {
        var composer = new FitImageComposer(tempDir.resolve("out"));
        var source = writeRedPng("source.png", 50, 100);
        var first = composer.compose(source, WIDE_MONITOR, Alignment.LEFT, FillColor.WHITE);

        var second = composer.compose(source, WIDE_MONITOR, Alignment.RIGHT, FillColor.WHITE);

        assertFalse(Files.exists(first));
        try (var files = Files.list(tempDir.resolve("out"))) {
            assertEquals(List.of(second), files.toList());
        }
    }

    @Test
    @DisplayName("画像として読めないファイルを合成しようとすると、ImageComposeExceptionを投げる")
    void composing_a_file_that_is_not_an_image_throws_image_compose_exception() throws IOException {
        var composer = new FitImageComposer(tempDir.resolve("out"));
        var notImage = Files.writeString(tempDir.resolve("broken.png"), "not an image");

        assertThrows(
            ImageComposeException.class,
            () -> composer.compose(notImage, WIDE_MONITOR, Alignment.LEFT, FillColor.WHITE)
        );
    }

    @Test
    @DisplayName("黒で埋めるようにFitすると、画像以外は黒になる")
    void an_image_fitted_with_black_fill_is_black_outside_the_image() {
        var fitted =
            FitImageComposer.fit(redImage(50, 100), 200, 100, Alignment.LEFT, FillColor.BLACK);

        assertEquals(RED, fitted.getRGB(0, 50));
        assertEquals(BLACK, fitted.getRGB(52, 50));
        assertEquals(BLACK, fitted.getRGB(199, 50));
    }

    private static BufferedImage redImage(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private Path writeRedPng(String name, int width, int height) throws IOException {
        var file = tempDir.resolve(name);
        // The content is PNG even for a .jpg name. ImageIO reads by content.
        ImageIO.write(redImage(width, height), "png", file.toFile());
        return file;
    }
}
