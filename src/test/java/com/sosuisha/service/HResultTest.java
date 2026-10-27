package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.exception.WallpaperException;

class HResultTest {
    private static final int E_FAIL = 0x80004005;
    private static final int S_FALSE = 1;

    @Test
    @DisplayName("COMの呼び出しが失敗したとき（HRESULTが負）、WallpaperExceptionを投げる")
    void a_failed_com_call_with_a_negative_hresult_throws_wallpaper_exception() {
        assertThrows(WallpaperException.class, () -> HResult.check(E_FAIL, "SetWallpaper"));
    }

    @Test
    @DisplayName("COMの呼び出しが成功したとき（HRESULTが0以上）、例外を投げない")
    void a_successful_com_call_with_a_non_negative_hresult_throws_nothing() {
        assertDoesNotThrow(() -> HResult.check(S_FALSE, "SetWallpaper"));
    }
}
