package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;

public final class FrameBudgetManager {
    private final ReliefMetrics metrics;

    private long rawFrameBudgetMicros = 6_944L;
    private long effectiveFrameBudgetMicros = 5_694L;
    private long targetFrameBudgetMicros = 16_667L;
    private long lastFrameMicros = 16_667L;
    private long budgetPressureMicros;
    private long safetyMarginMicros;
    private boolean floorActive;
    private boolean enabled = true;
    private double safetyMarginRatio = 0.18D;
    private boolean enableBudgetFloor = true;
    private long minimumEffectiveBudgetMicros = 4_200L;

    public FrameBudgetManager(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public void applyConfig(ReliefConfig config, int targetRefreshRate) {
        enabled = config.enableMod && config.enableFrameBudgetManager;
        safetyMarginRatio = Math.min(0.40D, Math.max(0.05D, config.safetyMarginRatio));
        enableBudgetFloor = config.enableBudgetFloor;
        minimumEffectiveBudgetMicros = Math.max(2_000L, config.minimumEffectiveBudgetMicros);

        rawFrameBudgetMicros = Math.max(1L, Math.round(1_000_000.0D / Math.max(1, targetRefreshRate)));
        safetyMarginMicros = Math.max(250L, Math.round(rawFrameBudgetMicros * safetyMarginRatio));

        long unclampedEffectiveBudget = Math.max(1L, rawFrameBudgetMicros - safetyMarginMicros);
        floorActive = enableBudgetFloor && unclampedEffectiveBudget < minimumEffectiveBudgetMicros;
        effectiveFrameBudgetMicros = floorActive ? minimumEffectiveBudgetMicros : unclampedEffectiveBudget;

        targetFrameBudgetMicros = Math.max(effectiveFrameBudgetMicros, Math.max(1, config.targetFrameBudgetMicros));
        metrics.budgetDetails(rawFrameBudgetMicros, effectiveFrameBudgetMicros, safetyMarginMicros, floorActive, currentBudgetState());
    }

    public void observe(FrameSample sample) {
        lastFrameMicros = sample.frameMicros();
        budgetPressureMicros = Math.max(0L, sample.frameMicros() - effectiveFrameBudgetMicros);
        metrics.budgetState(targetFrameBudgetMicros, budgetPressureMicros);
        metrics.budgetDetails(rawFrameBudgetMicros, effectiveFrameBudgetMicros, safetyMarginMicros, floorActive, currentBudgetState());
    }

    public ExecutionPlan createPlan(int baseBudgetMicros, int baseMaxTasks, AdaptiveMode mode, boolean stabilizing, boolean queueEnabled) {
        if (!enabled || !queueEnabled) {
            return new ExecutionPlan(Math.max(1, baseBudgetMicros), Math.max(1, baseMaxTasks), false, currentBudgetState());
        }

        double pressureRatio = Math.max(1.0D, (double) lastFrameMicros / (double) effectiveFrameBudgetMicros);
        int adjustedBudget = (int) Math.max(0, Math.round(baseBudgetMicros * mode.baseBudgetScale() / pressureRatio));
        int adjustedTasks = (int) Math.max(0, Math.round(baseMaxTasks * mode.taskScale() / pressureRatio));

        if (stabilizing) {
            adjustedBudget = (int) Math.max(0, Math.round(adjustedBudget * mode.stabilizationBudgetScale()));
            adjustedTasks = Math.min(adjustedTasks, 1);
        } else if (budgetPressureMicros > 0L) {
            adjustedTasks = Math.min(adjustedTasks, Math.max(0, baseMaxTasks - 1));
        } else if (currentBudgetState() == BudgetState.NEAR) {
            adjustedTasks = Math.min(adjustedTasks, Math.max(1, baseMaxTasks - 1));
        }

        boolean shouldDefer = stabilizing || budgetPressureMicros > 0L || currentBudgetState() == BudgetState.NEAR;
        return new ExecutionPlan(adjustedBudget, adjustedTasks, shouldDefer, currentBudgetState());
    }

    public boolean isOverBudget() {
        return enabled && budgetPressureMicros > 0L;
    }

    public long targetFrameBudgetMicros() {
        return targetFrameBudgetMicros;
    }

    public long budgetPressureMicros() {
        return budgetPressureMicros;
    }

    public long rawFrameBudgetMicros() {
        return rawFrameBudgetMicros;
    }

    public long effectiveFrameBudgetMicros() {
        return effectiveFrameBudgetMicros;
    }

    public long safetyMarginMicros() {
        return safetyMarginMicros;
    }

    public boolean floorActive() {
        return floorActive;
    }

    public BudgetState currentBudgetState() {
        if (!enabled) {
            return BudgetState.UNDER;
        }
        if (budgetPressureMicros > 0L) {
            return BudgetState.OVER;
        }

        double nearThreshold = effectiveFrameBudgetMicros * 0.90D;
        return lastFrameMicros >= nearThreshold ? BudgetState.NEAR : BudgetState.UNDER;
    }
}
