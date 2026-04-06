package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.debug.ReliefMetrics;

public final class FrametimeTracker {
    private final ReliefMetrics metrics;
    private long lastFrameNanos = System.nanoTime();
    private double averageFrametimeMs = 16.67D;
    private double worstRecentFrametimeMs = 16.67D;

    public FrametimeTracker(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public FrameSample recordFrame() {
        long now = System.nanoTime();
        long frameNanos = Math.max(0L, now - lastFrameNanos);
        lastFrameNanos = now;
        double delta = frameNanos / 1_000_000.0D;

        averageFrametimeMs = (averageFrametimeMs * 0.9D) + (delta * 0.1D);
        worstRecentFrametimeMs = Math.max(delta, worstRecentFrametimeMs * 0.92D);
        metrics.frametimeSample(delta, averageFrametimeMs, worstRecentFrametimeMs);
        return new FrameSample(frameNanos, Math.max(1L, frameNanos / 1_000L), delta, averageFrametimeMs, worstRecentFrametimeMs);
    }

    public int estimatedFps() {
        return averageFrametimeMs <= 0.0D ? 0 : (int) Math.round(1000.0D / averageFrametimeMs);
    }
}
