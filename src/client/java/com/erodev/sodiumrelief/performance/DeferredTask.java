package com.erodev.sodiumrelief.performance;

import java.util.Objects;

public record DeferredTask(Runnable action, TaskType type, TaskPriority priority, int estimatedCostMicros, String coalescingKey) {
    public DeferredTask(Runnable action, TaskType type, TaskPriority priority, int estimatedCostMicros) {
        this(action, type, priority, estimatedCostMicros, null);
    }

    public DeferredTask {
        action = Objects.requireNonNull(action);
        type = Objects.requireNonNull(type);
        priority = Objects.requireNonNull(priority);
        estimatedCostMicros = Math.max(1, estimatedCostMicros);
        coalescingKey = normalizeCoalescingKey(coalescingKey);
    }

    public boolean correctnessCritical() {
        return type.correctnessCritical();
    }

    public boolean isCoalescable() {
        return coalescingKey != null;
    }

    private static String normalizeCoalescingKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
