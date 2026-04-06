package com.erodev.sodiumrelief.resourcepack;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.SafeTime;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class ResourcePackTransitionVisualController implements ResourcePackTransitionListener, ResourcePackApplyGate {
    private static final int OVERLAY_RGB = 0x000000;
    private static final float MAX_ALPHA = 0.24F;

    private boolean initialized;
    private boolean supported = true;
    private boolean enabled = true;
    private long fadeDurationNanos = TimeUnit.MILLISECONDS.toNanos(120L);
    private VisualPhase phase = VisualPhase.INACTIVE;
    private long phaseStartedNanos;
    private float alpha;
    private float fadeInStartAlpha;
    private boolean applyingFramePresented;
    private BooleanSupplier debugLoggingEnabled = () -> false;

    public void initialize() {
        initialized = true;
    }

    public void debugLoggingEnabled(BooleanSupplier debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled == null ? () -> false : debugLoggingEnabled;
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableSmoothResourcePackTransition;
        fadeDurationNanos = TimeUnit.MILLISECONDS.toNanos(config.resourcePackTransitionFadeMillis);
        if (!enabled) {
            reset();
        }
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        long nowNanos = SafeTime.nowNanos();
        switch (phase) {
            case FADING_OUT -> {
                float progress = progress(nowNanos, phaseStartedNanos, fadeDurationNanos);
                alpha = MAX_ALPHA * progress;
                if (progress >= 1.0F) {
                    alpha = MAX_ALPHA;
                    phase = VisualPhase.HOLDING;
                }
            }
            case FADING_IN -> {
                float progress = progress(nowNanos, phaseStartedNanos, fadeDurationNanos);
                alpha = fadeInStartAlpha * (1.0F - progress);
                if (progress >= 1.0F) {
                    reset();
                }
            }
            case HOLDING -> alpha = MAX_ALPHA;
            case INACTIVE -> reset();
        }
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!isActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || client.options.hudHidden) {
            return;
        }

        int alphaChannel = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        if (alphaChannel <= 0) {
            return;
        }

        int color = (alphaChannel << 24) | OVERLAY_RGB;
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), color);
        if (phase == VisualPhase.FADING_OUT || phase == VisualPhase.HOLDING) {
            applyingFramePresented = true;
        }
    }

    @Override
    public void onApplying(ResourcePackTransitionSnapshot snapshot) {
        if (!initialized || !enabled || !supported) {
            return;
        }

        phase = VisualPhase.FADING_OUT;
        phaseStartedNanos = SafeTime.nowNanos();
        fadeInStartAlpha = 0.0F;
        applyingFramePresented = false;
    }

    @Override
    public void onResolved(ResourcePackTransitionSnapshot snapshot) {
        if (!initialized || !supported) {
            return;
        }

        if (!enabled) {
            reset();
            return;
        }

        phase = VisualPhase.FADING_IN;
        phaseStartedNanos = SafeTime.nowNanos();
        fadeInStartAlpha = alpha;
    }

    @Override
    public boolean canStartApply(ResourcePackTransitionSnapshot snapshot) {
        MinecraftClient client = MinecraftClient.getInstance();
        return !supported
            || !enabled
            || client.options == null
            || client.options.hudHidden
            || applyingFramePresented;
    }

    public void disableSupport(Throwable throwable) {
        supported = false;
        reset();
        ReliefLogger.warn("Disabled smooth resource-pack transition overlay");
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Smooth resource-pack transition overlay support failure", throwable);
    }

    private boolean isActive() {
        return supported && phase != VisualPhase.INACTIVE;
    }

    private void reset() {
        phase = VisualPhase.INACTIVE;
        phaseStartedNanos = 0L;
        alpha = 0.0F;
        fadeInStartAlpha = 0.0F;
        applyingFramePresented = false;
    }

    private static float progress(long nowNanos, long startNanos, long durationNanos) {
        if (durationNanos <= 0L) {
            return 1.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, (float) (nowNanos - startNanos) / (float) durationNanos));
    }

    private enum VisualPhase {
        INACTIVE,
        FADING_OUT,
        HOLDING,
        FADING_IN
    }
}
