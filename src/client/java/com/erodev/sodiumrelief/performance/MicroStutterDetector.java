package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;

public final class MicroStutterDetector {
    private final ReliefMetrics metrics;
    private boolean enabled = true;
    private long spikeThresholdMicros = 5_000L;
    private int stabilizationFrames = 6;
    private int stabilizationFramesRemaining;

    public MicroStutterDetector(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableMicroStutterDetector;
        spikeThresholdMicros = Math.max(500L, config.spikeThresholdMicros);
        stabilizationFrames = Math.max(1, config.stabilizationFrames);
    }

    public void observe(FrameSample sample, long targetFrameBudgetMicros) {
        if (!enabled) {
            stabilizationFramesRemaining = 0;
            metrics.stabilizationState(0);
            return;
        }

        long budgetOverrun = Math.max(0L, sample.frameMicros() - targetFrameBudgetMicros);
        boolean spike = budgetOverrun >= spikeThresholdMicros
            || sample.frameMillis() >= sample.averageMillis() * 1.55D;

        if (spike) {
            if (stabilizationFramesRemaining == 0) {
                metrics.stabilizationEntry();
            }
            stabilizationFramesRemaining = stabilizationFrames;
            metrics.spikeDetected();
        } else if (stabilizationFramesRemaining > 0) {
            stabilizationFramesRemaining--;
        }

        metrics.stabilizationState(stabilizationFramesRemaining);
    }

    public boolean isStabilizing() {
        return stabilizationFramesRemaining > 0;
    }

    public int stabilizationFramesRemaining() {
        return stabilizationFramesRemaining;
    }
}
