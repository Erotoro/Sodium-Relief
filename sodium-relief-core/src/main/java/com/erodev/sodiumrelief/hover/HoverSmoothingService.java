package com.erodev.sodiumrelief.hover;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.SafeTime;

/**
 * Hover state machine, split into two phases so the tooltip hot path only pays for the
 * fallback decision when it actually needs it:
 *
 * <ul>
 *   <li>{@link #recordSample} runs on <em>every</em> hovered frame. It advances the mouse
 *       velocity / rapid-movement state and captures a lightweight sample. It allocates
 *       nothing and makes no decision.</li>
 *   <li>{@link #assessFallbackNeed} turns the most recent sample into a {@link HoverAssessment}.
 *       It is only needed on a cache miss, so on a cache hit the caller skips it entirely.</li>
 * </ul>
 *
 * <p>The split is behaviour-identical to the previous single {@code assess(...)} method:
 * {@code recordSample(...)} immediately followed by {@code assessFallbackNeed(...)} reproduces
 * it exactly. This is locked down by a differential characterization test that fuzzes both
 * against a faithful copy of the original monolithic algorithm. The one deliberate behavioural
 * choice is that callers skip {@code assessFallbackNeed} on cache-hit frames; because the old
 * method's return value was discarded on hits, the only observable effect is that the
 * debug-only hover counters now tick at decision points (misses) instead of every frame.
 *
 * <p>Time is injected ({@code nowNanos}) rather than read internally, both to remove a
 * redundant {@code System.nanoTime()} per frame (the caller already has the timestamp) and to
 * make the logic deterministic for testing.
 */
public final class HoverSmoothingService {
    private final ReliefMetrics metrics;
    private final HoverTracker hoverTracker;
    private long lastTimestampNanos;
    private long lastRapidMovementTimestampNanos;
    private int lastMouseX;
    private int lastMouseY;

    // Most recent sample, captured by recordSample(...) and consumed by assessFallbackNeed(...).
    private boolean sampleSameHoverTarget;
    private long sampleDeltaNanos;
    private long sampleDistanceSquared;
    private long sampleNowNanos;
    private boolean sampleRapidMovement;

    public HoverSmoothingService(ReliefMetrics metrics, HoverTracker hoverTracker) {
        this.metrics = metrics;
        this.hoverTracker = hoverTracker;
    }

    /**
     * Advances the per-frame hover sample. Must run on every hovered frame to keep the
     * velocity and rapid-movement state evolving exactly as the original did. Allocation-free
     * and decision-free.
     */
    public void recordSample(
        ReliefConfig config,
        Class<?> screenClass,
        int slotId,
        int itemRawId,
        int itemCount,
        int componentHash,
        int mouseX,
        int mouseY,
        long nowNanos
    ) {
        boolean sameHoverTarget = hoverTracker.matches(screenClass, slotId, itemRawId, itemCount, componentHash);
        hoverTracker.set(screenClass, slotId, itemRawId, itemCount, componentHash);

        int deltaX = mouseX - lastMouseX;
        int deltaY = mouseY - lastMouseY;
        long deltaNanos = Math.max(1L, nowNanos - lastTimestampNanos);
        long distanceSquared = (long) deltaX * deltaX + (long) deltaY * deltaY;
        lastTimestampNanos = nowNanos;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // Calling this unconditionally every frame is equivalent to the original
        // branch-conditional calls: updateRapidMovement only mutates state when hover
        // prediction is enabled, and the single original branch that skipped it (the early
        // "stable" return) is only reachable when prediction is disabled — where this is a no-op.
        boolean rapidMovement = updateRapidMovement(config, config.adaptiveMode, nowNanos, deltaNanos, distanceSquared);

        sampleSameHoverTarget = sameHoverTarget;
        sampleDeltaNanos = deltaNanos;
        sampleDistanceSquared = distanceSquared;
        sampleNowNanos = nowNanos;
        sampleRapidMovement = rapidMovement;
    }

    /**
     * Turns the most recently recorded sample into a fallback assessment. Call only after
     * {@link #recordSample}; only needed on a cache miss.
     */
    public HoverAssessment assessFallbackNeed(ReliefConfig config) {
        boolean sameHoverTarget = sampleSameHoverTarget;
        long deltaNanos = sampleDeltaNanos;
        long distanceSquared = sampleDistanceSquared;
        long nowNanos = sampleNowNanos;

        if (!config.enableMod || !config.enableHoverSmoothing || !config.skipRedundantHoverRecomputations || !sameHoverTarget) {
            boolean deferHeavyWork = predictionDeferred(config, nowNanos);
            boolean rapidMovement = deferHeavyWork && lastRapidMovementTimestampNanos == nowNanos;
            metrics.hoverState(rapidMovement ? "MOVING" : deferHeavyWork ? "PREDICTIVE" : "STABLE");
            if (deferHeavyWork) {
                metrics.hoverDeferred();
            }
            return HoverAssessment.of(false, deferHeavyWork, rapidMovement);
        }

        long holdWindowNanos = SafeTime.millisToNanos(config.hoverMode.holdMillis());
        double pointerSlack = config.hoverMode.pointerSlack();
        double pointerSlackSquared = pointerSlack * pointerSlack;
        boolean withinHoldWindow = deltaNanos <= holdWindowNanos;
        boolean withinPointerSlack = distanceSquared <= pointerSlackSquared;
        if (!config.enableHoverPrediction && withinHoldWindow && withinPointerSlack) {
            metrics.hoverState("STABLE");
            metrics.hoverSkip();
            return HoverAssessment.of(true, false, false);
        }

        AdaptiveMode adaptiveMode = config.adaptiveMode;
        boolean rapidMovement = sampleRapidMovement;
        boolean deferHeavyWork = config.enableMod
            && config.enableHoverPrediction
            && nowNanos - lastRapidMovementTimestampNanos <= Math.max(
                SafeTime.millisToNanos(config.hoverPredictionDelayMillis),
                adaptiveMode.hoverPredictionDelayNanos()
            );
        metrics.hoverState(rapidMovement ? "MOVING" : deferHeavyWork ? "PREDICTIVE" : "STABLE");

        boolean skip = withinHoldWindow && withinPointerSlack;
        if (skip) {
            metrics.hoverSkip();
        }
        if (deferHeavyWork) {
            metrics.hoverDeferred();
        }
        return HoverAssessment.of(skip, deferHeavyWork, rapidMovement);
    }

    private boolean predictionDeferred(ReliefConfig config, long nowNanos) {
        if (!config.enableMod || !config.enableHoverPrediction) {
            return false;
        }
        AdaptiveMode adaptiveMode = config.adaptiveMode;
        long predictionWindowNanos = Math.max(
            SafeTime.millisToNanos(config.hoverPredictionDelayMillis),
            adaptiveMode.hoverPredictionDelayNanos()
        );
        return nowNanos - lastRapidMovementTimestampNanos <= predictionWindowNanos;
    }

    private boolean updateRapidMovement(ReliefConfig config, AdaptiveMode adaptiveMode, long nowNanos, long deltaNanos, long distanceSquared) {
        if (!config.enableMod || !config.enableHoverPrediction) {
            return false;
        }

        double speedThreshold = adaptiveMode.hoverSpeedThreshold();
        double speedThresholdSquared = speedThreshold * speedThreshold;
        double scaledDistanceSquared = distanceSquared * 1_000_000_000_000_000_000.0D;
        double thresholdDistanceSquared = speedThresholdSquared * (double) deltaNanos * (double) deltaNanos;
        boolean rapidMovement = scaledDistanceSquared >= thresholdDistanceSquared;
        if (rapidMovement) {
            lastRapidMovementTimestampNanos = nowNanos;
        }
        return rapidMovement;
    }
}
