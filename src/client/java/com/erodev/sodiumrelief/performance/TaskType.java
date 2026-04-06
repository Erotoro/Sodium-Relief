package com.erodev.sodiumrelief.performance;

public enum TaskType {
    CACHE_INVALIDATION(true),
    TOOLTIP_LAYOUT(false),
    TEXT_LAYOUT(false),
    UI_MAINTENANCE(false),
    DIAGNOSTICS(false);

    private final boolean correctnessCritical;

    TaskType(boolean correctnessCritical) {
        this.correctnessCritical = correctnessCritical;
    }

    public boolean correctnessCritical() {
        return correctnessCritical;
    }
}
