package com.erodev.sodiumrelief.hover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.config.ReliefMode;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.SafeTime;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Differential characterization test: proves that the split
 * {@code recordSample(...)} + {@code assessFallbackNeed(...)} is behaviourally identical to the
 * original single {@code assess(...)} method — both the returned {@link HoverAssessment} and the
 * debug counters — across every flag combination and a long fuzzed frame sequence.
 *
 * <p>The {@link Oracle} below is a faithful, line-for-line copy of the original monolithic
 * algorithm (only {@code SafeTime.nowNanos()} was turned into an injected parameter and
 * {@code new HoverAssessment(...)} into the interning factory). If the production split ever
 * diverges from it, this test fails.
 */
class HoverSmoothingServiceCharacterizationTest {

    @Test
    void splitMatchesOriginalAssessAcrossAllConfigsAndFuzzedFrames() {
        boolean[] bools = {false, true};
        ReliefMode[] hoverModes = ReliefMode.values();
        AdaptiveMode[] adaptiveModes = AdaptiveMode.values();
        // A few representative prediction delays, including the boundary value 1.
        int[] predictionDelays = {1, 28, 250};

        long configSeed = 0L;
        for (boolean enableMod : bools) {
            for (boolean enableHoverSmoothing : bools) {
                for (boolean skipRedundant : bools) {
                    for (boolean enableHoverPrediction : bools) {
                        for (ReliefMode hoverMode : hoverModes) {
                            for (AdaptiveMode adaptiveMode : adaptiveModes) {
                                for (int predictionDelay : predictionDelays) {
                                    ReliefConfig config = new ReliefConfig();
                                    config.enableMod = enableMod;
                                    config.enableHoverSmoothing = enableHoverSmoothing;
                                    config.skipRedundantHoverRecomputations = skipRedundant;
                                    config.enableHoverPrediction = enableHoverPrediction;
                                    config.hoverMode = hoverMode;
                                    config.adaptiveMode = adaptiveMode;
                                    config.hoverPredictionDelayMillis = predictionDelay;
                                    runFuzzedSequence(config, configSeed++);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void runFuzzedSequence(ReliefConfig config, long seed) {
        ReliefMetrics productionMetrics = new ReliefMetrics();
        productionMetrics.applyDebugMode(true); // so the gated hover counters actually tick
        HoverSmoothingService production = new HoverSmoothingService(productionMetrics, new HoverTracker());

        ReliefMetrics oracleMetrics = new ReliefMetrics();
        oracleMetrics.applyDebugMode(true);
        Oracle oracle = new Oracle(oracleMetrics, new HoverTracker());

        Random random = new Random(seed);
        long nowNanos = 0L;
        int mouseX = 0;
        int mouseY = 0;
        // A small pool of slots so "same hover target" flips true/false realistically.
        int slotId = 0;

        for (int frame = 0; frame < 400; frame++) {
            // Advance time by anything from sub-microsecond to >120 ms (crosses hold windows).
            nowNanos += 1L + (long) (random.nextDouble() * 130_000_000L);
            // Move the cursor by anything from 0 px (within slack) to a large jump (rapid).
            mouseX += random.nextInt(41) - 20;
            mouseY += random.nextInt(41) - 20;
            if (random.nextInt(5) == 0) {
                slotId = random.nextInt(4); // occasionally switch the hovered slot
            }

            Class<?> screenClass = String.class;
            int itemRawId = 7;
            int itemCount = 1;
            int componentHash = 0;

            HoverAssessment expected = oracle.assess(
                config, screenClass, slotId, itemRawId, itemCount, componentHash, mouseX, mouseY, nowNanos);

            production.recordSample(
                config, screenClass, slotId, itemRawId, itemCount, componentHash, mouseX, mouseY, nowNanos);
            HoverAssessment actual = production.assessFallbackNeed(config);

            assertSame(
                expected, actual,
                "mismatch at frame " + frame + " seed " + seed
                    + " (skip=" + actual.skipRedundant() + " defer=" + actual.deferHeavyWork()
                    + " rapid=" + actual.rapidMovement() + " vs expected skip=" + expected.skipRedundant()
                    + " defer=" + expected.deferHeavyWork() + " rapid=" + expected.rapidMovement() + ")");
        }

        // Debug counters and current state must also have evolved identically over the run.
        assertEquals(oracleMetrics.hoverSkips(), productionMetrics.hoverSkips(), "hoverSkips, seed " + seed);
        assertEquals(oracleMetrics.hoverDeferrals(), productionMetrics.hoverDeferrals(), "hoverDeferrals, seed " + seed);
        assertEquals(oracleMetrics.hoverState(), productionMetrics.hoverState(), "hoverState, seed " + seed);
    }

    /**
     * Faithful copy of the original single-method {@code HoverSmoothingService.assess(...)}
     * (pre-split), used as the equivalence oracle. Do not "optimize" this — it must mirror the
     * historical behaviour exactly.
     */
    private static final class Oracle {
        private final ReliefMetrics metrics;
        private final HoverTracker hoverTracker;
        private long lastTimestampNanos;
        private long lastRapidMovementTimestampNanos;
        private int lastMouseX;
        private int lastMouseY;

        Oracle(ReliefMetrics metrics, HoverTracker hoverTracker) {
            this.metrics = metrics;
            this.hoverTracker = hoverTracker;
        }

        HoverAssessment assess(
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

            if (!config.enableMod || !config.enableHoverSmoothing || !config.skipRedundantHoverRecomputations || !sameHoverTarget) {
                boolean deferHeavyWork = computePredictionState(config, nowNanos, deltaNanos, distanceSquared);
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
            return HoverAssessment.of(skip, deferHeavyWork, rapidMovement);
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
}
