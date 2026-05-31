package com.erodev.sodiumrelief.config;

public enum ReliefMode {
    CONSERVATIVE(35L, 0.0D),
    BALANCED(60L, 0.75D),
    AGGRESSIVE(90L, 1.5D);

    private final long holdMillis;
    private final double pointerSlack;

    ReliefMode(long holdMillis, double pointerSlack) {
        this.holdMillis = holdMillis;
        this.pointerSlack = pointerSlack;
    }

    public long holdMillis() {
        return holdMillis;
    }

    public double pointerSlack() {
        return pointerSlack;
    }
}
