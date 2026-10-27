package com.sosuisha.service;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.sosuisha.domain.model.Monitor;
import com.sosuisha.domain.service.WallpaperService;

/**
 * Wallpaper service that calls the IDesktopWallpaper COM interface of Windows
 * through the Foreign Function and Memory API.
 */
public class DesktopWallpaperService implements WallpaperService {
    private static final String CLSID_DESKTOP_WALLPAPER = "C2CF3110-460E-4FC1-B9D0-8A1C0C9CC4BD";
    private static final String IID_IDESKTOP_WALLPAPER = "B92B56A9-8B55-4E14-9A89-0199BBB6F93B";
    private static final int COINIT_APARTMENTTHREADED = 0x2;
    private static final int CLSCTX_ALL = 0x17;
    private static final int S_OK = 0;

    // Indexes in the vtable of IDesktopWallpaper. The first three are IUnknown.
    private static final int RELEASE = 2;
    private static final int SET_WALLPAPER = 3;
    private static final int GET_WALLPAPER = 4;
    private static final int GET_MONITOR_DEVICE_PATH_AT = 5;
    private static final int GET_MONITOR_DEVICE_PATH_COUNT = 6;
    private static final int GET_MONITOR_RECT = 7;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup OLE32 = SymbolLookup.libraryLookup("ole32", Arena.global());
    private static final MethodHandle CO_INITIALIZE_EX =
        downcall("CoInitializeEx", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle CO_UNINITIALIZE =
        downcall("CoUninitialize", FunctionDescriptor.ofVoid());
    private static final MethodHandle CO_CREATE_INSTANCE = downcall(
        "CoCreateInstance",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
    );
    private static final MethodHandle CO_TASK_MEM_FREE =
        downcall("CoTaskMemFree", FunctionDescriptor.ofVoid(ADDRESS));

    @Override
    public List<Monitor> listMonitors() {
        return withDesktopWallpaper((arena, wallpaper) -> {
            var count = arena.allocate(JAVA_INT);
            HResult.check(
                callMethod(
                    wallpaper, GET_MONITOR_DEVICE_PATH_COUNT,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), count
                ),
                "GetMonitorDevicePathCount"
            );
            var monitors = new ArrayList<Monitor>();
            for (int i = 0; i < count.get(JAVA_INT, 0); i++) {
                var id = monitorDevicePathAt(arena, wallpaper, i);
                var rect = arena.allocate(JAVA_INT, 4);
                int result = callMethod(
                    wallpaper, GET_MONITOR_RECT,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                    arena.allocateFrom(id, StandardCharsets.UTF_16LE), rect
                );
                // A monitor that is known to Windows but detached from the
                // desktop has no rectangle, so it cannot show a wallpaper.
                if (result != S_OK) {
                    continue;
                }
                int left = rect.getAtIndex(JAVA_INT, 0);
                int top = rect.getAtIndex(JAVA_INT, 1);
                int right = rect.getAtIndex(JAVA_INT, 2);
                int bottom = rect.getAtIndex(JAVA_INT, 3);
                monitors.add(new Monitor(id, left, top, right - left, bottom - top));
            }
            return monitors;
        });
    }

    @Override
    public void setWallpaper(String monitorId, Path image) {
        Objects.requireNonNull(monitorId, "monitorId must not be null");
        Objects.requireNonNull(image, "image must not be null");
        int result = withDesktopWallpaper(
            (arena, wallpaper) -> callMethod(
                wallpaper, SET_WALLPAPER,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                arena.allocateFrom(monitorId, StandardCharsets.UTF_16LE),
                arena.allocateFrom(image.toAbsolutePath().toString(), StandardCharsets.UTF_16LE)
            )
        );
        HResult.check(result, "SetWallpaper");
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.sosuisha.domain.exception.WallpaperException if Windows fails to read it
     */
    @Override
    public Optional<Path> getWallpaper(String monitorId) {
        Objects.requireNonNull(monitorId, "monitorId must not be null");
        return withDesktopWallpaper((arena, wallpaper) -> {
            var pathPointer = arena.allocate(ADDRESS);
            HResult.check(
                callMethod(
                    wallpaper, GET_WALLPAPER,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                    arena.allocateFrom(monitorId, StandardCharsets.UTF_16LE), pathPointer
                ),
                "GetWallpaper"
            );
            // Windows returns an empty string when the monitor has no wallpaper.
            var path = takeString(pathPointer.get(ADDRESS, 0));
            return path.isEmpty() ? Optional.<Path>empty() : Optional.of(Path.of(path));
        });
    }

    private interface ComAction<T> {
        T run(Arena arena, MemorySegment wallpaper);
    }

    private static <T> T withDesktopWallpaper(ComAction<T> action) {
        // CoInitializeEx fails with RPC_E_CHANGED_MODE when the thread already
        // has another apartment. COM is usable then, but must not be released.
        int initResult =
            (int) invoke(CO_INITIALIZE_EX, MemorySegment.NULL, COINIT_APARTMENTTHREADED);
        try (var arena = Arena.ofConfined()) {
            var pointer = arena.allocate(ADDRESS);
            HResult.check(
                (int) invoke(
                    CO_CREATE_INSTANCE, guid(arena, CLSID_DESKTOP_WALLPAPER), MemorySegment.NULL,
                    CLSCTX_ALL, guid(arena, IID_IDESKTOP_WALLPAPER), pointer
                ),
                "CoCreateInstance(DesktopWallpaper)"
            );
            var wallpaper = pointer.get(ADDRESS, 0);
            try {
                return action.run(arena, wallpaper);
            } finally {
                callMethod(wallpaper, RELEASE, FunctionDescriptor.of(JAVA_INT, ADDRESS));
            }
        } finally {
            if (initResult >= 0) {
                invoke(CO_UNINITIALIZE);
            }
        }
    }

    private static String monitorDevicePathAt(Arena arena, MemorySegment wallpaper, int index) {
        var pathPointer = arena.allocate(ADDRESS);
        HResult.check(
            callMethod(
                wallpaper, GET_MONITOR_DEVICE_PATH_AT,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS), index, pathPointer
            ),
            "GetMonitorDevicePathAt"
        );
        return takeString(pathPointer.get(ADDRESS, 0));
    }

    // COM returns strings that the caller must free with CoTaskMemFree.
    private static String takeString(MemorySegment string) {
        try {
            return string.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_16LE);
        } finally {
            invoke(CO_TASK_MEM_FREE, string);
        }
    }

    private static int callMethod(
        MemorySegment comObject, int vtableIndex, FunctionDescriptor descriptor, Object... args) {
        var vtable = comObject.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
        var function = vtable.reinterpret((vtableIndex + 1) * ADDRESS.byteSize())
            .getAtIndex(ADDRESS, vtableIndex);
        var allArgs = new Object[args.length + 1];
        allArgs[0] = comObject;
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return (int) invoke(LINKER.downcallHandle(function, descriptor), allArgs);
    }

    private static MemorySegment guid(Arena arena, String text) {
        var uuid = UUID.fromString(text);
        long high = uuid.getMostSignificantBits();
        long low = uuid.getLeastSignificantBits();
        var guid = arena.allocate(16, 4);
        guid.set(JAVA_INT, 0, (int) (high >>> 32));
        guid.set(JAVA_SHORT, 4, (short) (high >>> 16));
        guid.set(JAVA_SHORT, 6, (short) high);
        for (int i = 0; i < 8; i++) {
            guid.set(JAVA_BYTE, 8 + i, (byte) (low >>> (56 - 8 * i)));
        }
        return guid;
    }

    private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
        var symbol = OLE32.find(name)
            .orElseThrow(() -> new IllegalStateException("ole32 has no function: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private static Object invoke(MethodHandle handle, Object... args) {
        try {
            return handle.invokeWithArguments(args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("native call failed", e);
        }
    }
}
