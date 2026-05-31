package com.erodev.sodiumrelief.tooltip;

import com.erodev.sodiumrelief.cache.TooltipLayoutCache.CachedTooltipLayout;
import com.erodev.sodiumrelief.util.TooltipFingerprint;
import java.util.Objects;
import java.util.Optional;

public final class TooltipPresentationService {
    private static final long GUARANTEED_RENDER_WINDOW_NANOS = 60_000_000L;
    private static final long HARD_MAX_TOOLTIP_DELAY_NANOS = 100_000_000L;

    private TooltipFingerprint currentFingerprint;
    private long hoverStartNanos;
    private TooltipFingerprint lastValidFingerprint;
    private CachedTooltipLayout lastValidLayout;

    public void updateHover(TooltipFingerprint fingerprint, long nowNanos) {
        if (!fingerprint.equals(currentFingerprint)) {
            currentFingerprint = fingerprint;
            hoverStartNanos = nowNanos;
        }
    }

    public long stableHoverDurationNanos(long nowNanos) {
        return currentFingerprint == null ? 0L : Math.max(0L, nowNanos - hoverStartNanos);
    }

    public TooltipTiming timing(long nowNanos) {
        long stableHoverNanos = stableHoverDurationNanos(nowNanos);
        boolean guaranteedRenderWindowReached = stableHoverNanos >= GUARANTEED_RENDER_WINDOW_NANOS;
        boolean hardMaxDelayReached = stableHoverNanos >= HARD_MAX_TOOLTIP_DELAY_NANOS;
        return new TooltipTiming(stableHoverNanos, guaranteedRenderWindowReached, hardMaxDelayReached);
    }

    public void remember(TooltipFingerprint fingerprint, CachedTooltipLayout layout) {
        lastValidFingerprint = Objects.requireNonNull(fingerprint);
        lastValidLayout = Objects.requireNonNull(layout);
    }

    public Optional<CachedTooltipLayout> reuse(TooltipFingerprint fingerprint) {
        if (fingerprint.equals(lastValidFingerprint) && lastValidLayout != null) {
            return Optional.of(lastValidLayout);
        }
        return Optional.empty();
    }

    public Optional<CachedTooltipLayout> fallback(TooltipFingerprint fingerprint, TooltipTiming timing) {
        if (timing.guaranteedRenderWindowReached()) {
            return Optional.empty();
        }
        return reuse(fingerprint);
    }

    public void clear() {
        currentFingerprint = null;
        hoverStartNanos = 0L;
        lastValidFingerprint = null;
        lastValidLayout = null;
    }

    public record TooltipTiming(long stableHoverNanos, boolean guaranteedRenderWindowReached, boolean hardMaxDelayReached) {
    }
}
