package com.erodev.sodiumrelief.config;

public enum RefreshPreset {
    HZ_60(60),
    HZ_120(120),
    HZ_144(144),
    HZ_165(165),
    HZ_240(240),
    HZ_360(360);

    private final int refreshRate;

    RefreshPreset(int refreshRate) {
        this.refreshRate = refreshRate;
    }

    public int refreshRate() {
        return refreshRate;
    }
}
