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
    @DisplayName("画像が2枚以上あれば、現在の画像は選ばず、残りからランダムに選ぶ")
    void the_current_image_is_not_picked_when_the_folder_has_another_image() throws IOException {
        Files.createFile(tempDir.resolve("a.jpg"));
        var current = Files.createFile(tempDir.resolve("b.png"));
        var other = Files.createFile(tempDir.resolve("c.png"));
        var picker = new RandomImagePicker(fixedIndex(1));

        var picked = picker.pick(tempDir, Optional.of(current));

        // Without b.png, the images are a.jpg and c.png, so index 1 is c.png.
        assertEquals(Optional.of(other), picked);
    }

    @Test
    @DisplayName("乱数の範囲は、現在の画像を除いた枚数になる")
    void the_random_range_is_the_number_of_images_without_the_current_one() throws IOException {
        Files.createFile(tempDir.resolve("a.jpg"));
        var last = Files.createFile(tempDir.resolve("b.png"));
        var current = Files.createFile(tempDir.resolve("c.png"));
        var picker = new RandomImagePicker(lastIndex());

        var picked = picker.pick(tempDir, Optional.of(current));

        assertEquals(Optional.of(last), picked);
    }

    @Test
    @DisplayName("画像が現在の画像の1枚だけなら、その画像を選ぶ")
    void the_current_image_is_picked_when_it_is_the_only_image() throws IOException {
        var only = Files.createFile(tempDir.resolve("only.png"));
        var picker = new RandomImagePicker(fixedIndex(0));

        assertEquals(Optional.of(only), picker.pick(tempDir, Optional.of(only)));
    }

    @Test
    @DisplayName("現在の画像がフォルダにないときは、全画像から選ぶ")
    void every_image_can_be_picked_when_the_current_image_is_not_in_the_folder()
        throws IOException {
        Files.createFile(tempDir.resolve("a.jpg"));
        var last = Files.createFile(tempDir.resolve("b.png"));
        var picker = new RandomImagePicker(lastIndex());

        var picked =
            picker.pick(tempDir, Optional.of(tempDir.resolve("elsewhere").resolve("x.png")));

        assertEquals(Optional.of(last), picked);
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

    private static RandomGenerator lastIndex() {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };
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
