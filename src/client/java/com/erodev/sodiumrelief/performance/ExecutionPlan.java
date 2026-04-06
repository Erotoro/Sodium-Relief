package com.erodev.sodiumrelief.performance;

public record ExecutionPlan(int budgetMicros, int maxTasks, boolean shouldDeferNonCriticalWork, BudgetState budgetState) {
}
