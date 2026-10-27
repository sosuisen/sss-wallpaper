package com.sosuisha.service;

import com.sosuisha.domain.exception.WallpaperException;

/**
 * Checks the HRESULT values that COM methods return.
 */
final class HResult {
    private HResult() {}

    /**
     * Checks that a COM call succeeded.
     *
     * @param result   HRESULT returned by the call
     * @param function name of the called function, for the message
     * @throws WallpaperException if result is negative, which means failure
     */
    static void check(int result, String function) {
        if (result < 0) {
            throw new WallpaperException(
                "%s failed: HRESULT 0x%08X".formatted(function, result)
            );
        }
    }
}
