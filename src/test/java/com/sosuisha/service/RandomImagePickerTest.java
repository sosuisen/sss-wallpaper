package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.exception.FolderReadException;

class RandomImagePickerTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("フォルダの中の画像ファイルから、ランダムに1枚を選ぶ")
    void one_image_file_in_the_folder_is_picked_at_random() throws IOException {
        Files.createFile(tempDir.resolve("a.jpg"));
        Files.createFile(tempDir.resolve("b.png"));
        Files.createFile(tempDir.resolve("c.png"));
        Files.createFile(tempDir.resolve("notes.txt"));
        var picker = new RandomImagePicker(fixedIndex(1));

        var picked = picker.pick(tempDir);

        // The image files are sorted by name, so index 1 is b.png.
        assertEquals(Optional.of(tempDir.resolve("b.png")), picked);
    }

    @Test
    @DisplayName("画像ファイルがないフォルダからは、何も選ばない")
    void nothing_is_picked_from_a_folder_without_image_files() throws IOException {
        Files.createFile(tempDir.resolve("notes.txt"));
        Files.createDirectory(tempDir.resolve("folder.jpg"));
        var picker = new RandomImagePicker(fixedIndex(0));

        assertEquals(Optional.empty(), picker.pick(tempDir));
    }

    @Test
    @DisplayName("フォルダが読めないとき（消されたときなど）、FolderReadExceptionを投げる")
    void a_folder_that_cannot_be_read_such_as_a_deleted_one_throws_folder_read_exception() {
        var picker = new RandomImagePicker(fixedIndex(0));

        assertThrows(FolderReadException.class, () -> picker.pick(tempDir.resolve("deleted")));
    }

    private static RandomGenerator fixedIndex(int index) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return index;
            }

            @Override
            public int nextInt(int bound) {
                return index;
            }
        };
    }
}
