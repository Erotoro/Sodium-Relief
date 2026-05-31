package com.erodev.sodiumrelief.config;

public enum AdaptiveMode {
    SAFE(16_000_000L, 12_000.0D),
    BALANCED(28_000_000L, 9_000.0D),
    AGGRESSIVE(40_000_000L, 7_000.0D);

    private final long hoverPredictionDelayNanos;
    private final double hoverSpeedThreshold;

    AdaptiveMode(long hoverPredictionDelayNanos, double hoverSpeedThreshold) {
        this.hoverPredictionDelayNanos = hoverPredictionDelayNanos;
        this.hoverSpeedThreshold = hoverSpeedThreshold;
    }

    public long hoverPredictionDelayNanos() {
        return hoverPredictionDelayNanos;
    }

    public double hoverSpeedThreshold() {
        return hoverSpeedThreshold;
    }
}
