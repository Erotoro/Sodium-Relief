package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;

public final class WorkDistributionSystem {
    private boolean enabled = true;

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableWorkDistributionSystem;
    }

    public ExecutionPlan distribute(ExecutionPlan plan, int queuedTasks, AdaptiveMode mode) {
        if (!enabled || queuedTasks <= 1) {
            return plan;
        }

        int scaledTasks = switch (mode) {
            case SAFE -> Math.min(plan.maxTasks(), 1 + Math.max(0, queuedTasks / 4));
            case BALANCED -> Math.min(plan.maxTasks(), 1 + Math.max(0, queuedTasks / 3));
            case AGGRESSIVE -> Math.min(plan.maxTasks(), 1 + Math.max(0, queuedTasks / 2));
        };

        int divisor = switch (mode) {
            case SAFE -> Math.max(2, queuedTasks);
            case BALANCED -> Math.max(2, queuedTasks - 1);
            case AGGRESSIVE -> Math.max(2, queuedTasks - 2);
        };

        int smoothedBudget = Math.max(0, plan.budgetMicros() / divisor);
        int finalBudget = switch (plan.budgetState()) {
            case UNDER -> Math.max(smoothedBudget, Math.min(plan.budgetMicros(), smoothedBudget * 2));
            case NEAR -> Math.max(0, smoothedBudget);
            case OVER -> Math.max(0, smoothedBudget / 2);
        };

        return new ExecutionPlan(
            Math.min(plan.budgetMicros(), finalBudget),
            Math.max(1, scaledTasks),
            plan.shouldDeferNonCriticalWork(),
            plan.budgetState()
        );
    }
}
