package com.erodev.sodiumrelief.hover;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.SafeTime;

public final class HoverSmoothingService {
    private final ReliefMetrics metrics;
    private final HoverTracker hoverTracker;
    private long lastTimestampNanos;
    private long lastRapidMovementTimestampNanos;
    private int lastMouseX;
    private int lastMouseY;

    public HoverSmoothingService(ReliefMetrics metrics, HoverTracker hoverTracker) {
        this.metrics = metrics;
        this.hoverTracker = hoverTracker;
    }

    public HoverAssessment assess(
        ReliefConfig config,
        Class<?> screenClass,
        int slotId,
        int itemRawId,
        int itemCount,
        int componentHash,
        int mouseX,
        int mouseY
    ) {
        long nowNanos = SafeTime.nowNanos();
        boolean sameHoverTarget = hoverTracker.matches(screenClass, slotId, itemRawId, itemCount, componentHash);
        hoverTracker.set(screenClass, slotId, itemRawId, itemCount, componentHash);

        int deltaX = mouseX - lastMouseX;
        int deltaY = mouseY - lastMouseY;
        long deltaNanos = Math.max(1L, nowNanos - lastTimestampNanos);
        long distanceSquared = (long) deltaX * deltaX + (long) deltaY * deltaY;
        lastTimestampNanos = nowNanos;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (!config.enableMod || !config.enableHoverSmoothing || !config.skipRedundantHoverRecomputations || !sameHoverTarget) {
            boolean deferHeavyWork = computePredictionState(config, nowNanos, deltaNanos, distanceSquared);
            boolean rapidMovement = deferHeavyWork && lastRapidMovementTimestampNanos == nowNanos;
            metrics.hoverState(rapidMovement ? "MOVING" : deferHeavyWork ? "PREDICTIVE" : "STABLE");
            if (deferHeavyWork) {
                metrics.hoverDeferred();
            }
            return new HoverAssessment(false, deferHeavyWork, rapidMovement);
        }

        long holdWindowNanos = SafeTime.millisToNanos(config.hoverMode.holdMillis());
        double pointerSlack = config.hoverMode.pointerSlack();
        double pointerSlackSquared = pointerSlack * pointerSlack;
        boolean withinHoldWindow = deltaNanos <= holdWindowNanos;
        boolean withinPointerSlack = distanceSquared <= pointerSlackSquared;
        if (!config.enableHoverPrediction && withinHoldWindow && withinPointerSlack) {
            metrics.hoverState("STABLE");
            metrics.hoverSkip();
            return new HoverAssessment(true, false, false);
        }

        AdaptiveMode adaptiveMode = config.adaptiveMode;
        boolean rapidMovement = updateRapidMovement(config, adaptiveMode, nowNanos, deltaNanos, distanceSquared);
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
        return new HoverAssessment(skip, deferHeavyWork, rapidMovement);
    }

    private boolean computePredictionState(ReliefConfig config, long nowNanos, long deltaNanos, long distanceSquared) {
        if (!config.enableMod || !config.enableHoverPrediction) {
            return false;
        }
        AdaptiveMode adaptiveMode = config.adaptiveMode;
        updateRapidMovement(config, adaptiveMode, nowNanos, deltaNanos, distanceSquared);
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
