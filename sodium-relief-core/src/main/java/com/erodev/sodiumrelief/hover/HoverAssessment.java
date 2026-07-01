package com.erodev.sodiumrelief.hover;

/**
 * Result of a per-frame hover evaluation.
 *
 * <p>There are only three booleans — eight possible combinations — and the hover path
 * produces one of these on <em>every</em> hovered frame. Rather than allocate a fresh
 * object each time, all eight states are interned at class-load and handed out by
 * {@link #of(boolean, boolean, boolean)}. This keeps the hover hot path allocation-free
 * without changing any observable behavior: instances are immutable and value-equal
 * instances are reference-identical, so existing accessor-based call sites are unaffected.
 */
public final class HoverAssessment {
    private static final HoverAssessment[] POOL = new HoverAssessment[8];

    static {
        for (int bits = 0; bits < POOL.length; bits++) {
            POOL[bits] = new HoverAssessment((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0);
        }
    }

    private final boolean skipRedundant;
    private final boolean deferHeavyWork;
    private final boolean rapidMovement;

    private HoverAssessment(boolean skipRedundant, boolean deferHeavyWork, boolean rapidMovement) {
        this.skipRedundant = skipRedundant;
        this.deferHeavyWork = deferHeavyWork;
        this.rapidMovement = rapidMovement;
    }

    /**
     * Returns the interned state for the given flags. Never allocates.
     */
    public static HoverAssessment of(boolean skipRedundant, boolean deferHeavyWork, boolean rapidMovement) {
        int bits = (skipRedundant ? 1 : 0) | (deferHeavyWork ? 2 : 0) | (rapidMovement ? 4 : 0);
        return POOL[bits];
    }

    public boolean skipRedundant() {
        return skipRedundant;
    }

    public boolean deferHeavyWork() {
        return deferHeavyWork;
    }

    public boolean rapidMovement() {
        return rapidMovement;
    }
}
