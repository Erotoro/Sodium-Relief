package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.config.RefreshMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public final class RefreshRateResolver {
    private static final int WINDOW_STATE_POLL_INTERVAL_TICKS = 20;
    private static final int WINDOW_CHANGE_THRESHOLD_PIXELS = 24;

    private boolean autoFallbackActive = true;
    private int resolvedRefreshRate = 144;
    private RefreshMode refreshMode = RefreshMode.AUTO;
    private int refreshPresetRate = 144;
    private int customRefreshRate = 144;
    private int minimumRefreshRate = 30;
    private int maximumRefreshRate = 500;
    private int ticksUntilWindowPoll;
    private WindowState lastWindowState;
    private final int[] windowX = new int[1];
    private final int[] windowY = new int[1];
    private final int[] windowWidth = new int[1];
    private final int[] windowHeight = new int[1];
    private final int[] monitorX = new int[1];
    private final int[] monitorY = new int[1];

    public void applyConfig(ReliefConfig config) {
        refreshMode = config.refreshMode;
        refreshPresetRate = config.refreshPreset.refreshRate();
        customRefreshRate = config.customRefreshRate;
        minimumRefreshRate = Math.max(1, config.minimumRefreshRate);
        maximumRefreshRate = Math.max(minimumRefreshRate, config.maximumRefreshRate);
        ticksUntilWindowPoll = 0;
        resolveNow(currentClientWindow());
    }

    public boolean refreshIfNeeded(MinecraftClient client) {
        if (refreshMode != RefreshMode.AUTO) {
            return false;
        }

        if (ticksUntilWindowPoll > 0) {
            ticksUntilWindowPoll--;
            return false;
        }
        ticksUntilWindowPoll = WINDOW_STATE_POLL_INTERVAL_TICKS;

        Window window = client == null ? null : client.getWindow();
        WindowState currentState = captureWindowState(window);
        if (currentState.equals(lastWindowState) || !currentState.requiresRefreshFrom(lastWindowState)) {
            lastWindowState = currentState;
            return false;
        }

        int previousRefreshRate = resolvedRefreshRate;
        boolean previousFallbackState = autoFallbackActive;
        resolveNow(window);
        return previousRefreshRate != resolvedRefreshRate || previousFallbackState != autoFallbackActive;
    }

    public boolean autoFallbackActive() {
        return autoFallbackActive;
    }

    public int resolvedRefreshRate() {
        return resolvedRefreshRate;
    }

    private void resolveNow(Window window) {
        lastWindowState = captureWindowState(window);
        int refreshRate = switch (refreshMode) {
            case PRESET -> refreshPresetRate;
            case CUSTOM -> customRefreshRate;
            case AUTO -> resolveAuto(window);
        };
        resolvedRefreshRate = clamp(refreshRate, minimumRefreshRate, maximumRefreshRate);
    }

    private int resolveAuto(Window window) {
        long monitor = resolveActiveMonitor(window);
        if (monitor != 0L) {
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode != null && mode.refreshRate() > 0) {
                autoFallbackActive = false;
                return mode.refreshRate();
            }
        }

        autoFallbackActive = true;
        return refreshPresetRate;
    }

    private long resolveActiveMonitor(Window window) {
        if (window == null) {
            return 0L;
        }

        long windowHandle = window.getHandle();
        if (windowHandle == 0L) {
            return 0L;
        }

        long fullscreenMonitor = GLFW.glfwGetWindowMonitor(windowHandle);
        if (fullscreenMonitor != 0L) {
            return fullscreenMonitor;
        }

        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null || monitors.limit() == 0) {
            return GLFW.glfwGetPrimaryMonitor();
        }

        GLFW.glfwGetWindowPos(windowHandle, windowX, windowY);
        GLFW.glfwGetWindowSize(windowHandle, windowWidth, windowHeight);
        if (windowWidth[0] <= 0 || windowHeight[0] <= 0) {
            return GLFW.glfwGetPrimaryMonitor();
        }

        long bestMonitor = 0L;
        long bestOverlapArea = -1L;
        for (int index = 0; index < monitors.limit(); index++) {
            long monitor = monitors.get(index);
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode == null) {
                continue;
            }

            GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);
            long overlapArea = overlapArea(
                windowX[0], windowY[0], windowWidth[0], windowHeight[0],
                monitorX[0], monitorY[0], mode.width(), mode.height()
            );
            if (overlapArea > bestOverlapArea) {
                bestOverlapArea = overlapArea;
                bestMonitor = monitor;
            }
        }

        if (bestMonitor != 0L && bestOverlapArea > 0L) {
            return bestMonitor;
        }
        return GLFW.glfwGetPrimaryMonitor();
    }

    private Window currentClientWindow() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.getWindow();
    }

    private WindowState captureWindowState(Window window) {
        if (window == null) {
            return WindowState.missing();
        }

        long handle = window.getHandle();
        if (handle == 0L) {
            return WindowState.missing();
        }

        long fullscreenMonitor = GLFW.glfwGetWindowMonitor(handle);
        GLFW.glfwGetWindowPos(handle, windowX, windowY);
        GLFW.glfwGetWindowSize(handle, windowWidth, windowHeight);
        return new WindowState(handle, fullscreenMonitor, windowX[0], windowY[0], windowWidth[0], windowHeight[0]);
    }

    private static long overlapArea(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        int overlapWidth = Math.max(0, Math.min(ax + aw, bx + bw) - Math.max(ax, bx));
        int overlapHeight = Math.max(0, Math.min(ay + ah, by + bh) - Math.max(ay, by));
        return (long) overlapWidth * overlapHeight;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record WindowState(long handle, long fullscreenMonitor, int x, int y, int width, int height) {
        private static WindowState missing() {
            return new WindowState(0L, 0L, 0, 0, 0, 0);
        }

        private boolean requiresRefreshFrom(WindowState previous) {
            if (previous == null) {
                return true;
            }
            if (handle != previous.handle || fullscreenMonitor != previous.fullscreenMonitor) {
                return true;
            }
            if (width <= 0 || height <= 0 || previous.width <= 0 || previous.height <= 0) {
                return width != previous.width || height != previous.height;
            }

            return Math.abs(x - previous.x) >= WINDOW_CHANGE_THRESHOLD_PIXELS
                || Math.abs(y - previous.y) >= WINDOW_CHANGE_THRESHOLD_PIXELS
                || Math.abs(width - previous.width) >= WINDOW_CHANGE_THRESHOLD_PIXELS
                || Math.abs(height - previous.height) >= WINDOW_CHANGE_THRESHOLD_PIXELS;
        }
    }
}
