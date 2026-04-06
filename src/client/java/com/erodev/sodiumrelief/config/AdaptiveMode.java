package com.erodev.sodiumrelief.config;

public enum AdaptiveMode {
    SAFE(0.90D, 1.00D, 16_000_000L, 12_000.0D, 0.55D),
    BALANCED(1.00D, 1.00D, 28_000_000L, 9_000.0D, 0.30D),
    AGGRESSIVE(1.10D, 1.20D, 40_000_000L, 7_000.0D, 0.15D);

    private final double baseBudgetScale;
    private final double taskScale;
    private final long hoverPredictionDelayNanos;
    private final double hoverSpeedThreshold;
    private final double stabilizationBudgetScale;

    AdaptiveMode(double baseBudgetScale, double taskScale, long hoverPredictionDelayNanos, double hoverSpeedThreshold, double stabilizationBudgetScale) {
        this.baseBudgetScale = baseBudgetScale;
        this.taskScale = taskScale;
        this.hoverPredictionDelayNanos = hoverPredictionDelayNanos;
        this.hoverSpeedThreshold = hoverSpeedThreshold;
        this.stabilizationBudgetScale = stabilizationBudgetScale;
    }

    public double baseBudgetScale() {
        return baseBudgetScale;
    }

    public double taskScale() {
        return taskScale;
    }

    public long hoverPredictionDelayNanos() {
        return hoverPredictionDelayNanos;
    }

    public double hoverSpeedThreshold() {
        return hoverSpeedThreshold;
    }

    public double stabilizationBudgetScale() {
        return stabilizationBudgetScale;
    }
}
