package com.erodev.sodiumrelief.performance;

import java.util.EnumMap;

public final class TaskCostEstimator {
    private static final double EWMA_ALPHA = 0.22D;

    private final EnumMap<TaskType, Double> averages = createAverages();

    public TaskCostEstimator() {
        averages.put(TaskType.CACHE_INVALIDATION, 600.0D);
        averages.put(TaskType.TOOLTIP_LAYOUT, 350.0D);
        averages.put(TaskType.TEXT_LAYOUT, 200.0D);
        averages.put(TaskType.UI_MAINTENANCE, 300.0D);
        averages.put(TaskType.DIAGNOSTICS, 120.0D);
    }

    public int estimate(TaskType type) {
        double average = averages.getOrDefault(type, 250.0D);
        return Math.max(50, (int) Math.round(average));
    }

    public void record(TaskType type, long observedRuntimeMicros) {
        double previous = averages.getOrDefault(type, (double) observedRuntimeMicros);
        double updated = (previous * (1.0D - EWMA_ALPHA)) + (observedRuntimeMicros * EWMA_ALPHA);
        averages.put(type, updated);
    }

    public int averageMicros(TaskType type) {
        return Math.max(1, (int) Math.round(averages.getOrDefault(type, 0.0D)));
    }

    @SuppressWarnings({"DataFlowIssue", "NullableProblems"})
    private static EnumMap<TaskType, Double> createAverages() {
        return new EnumMap<>(taskTypeClass());
    }

    private static Class<TaskType> taskTypeClass() {
        return TaskType.CACHE_INVALIDATION.getDeclaringClass();
    }
}
