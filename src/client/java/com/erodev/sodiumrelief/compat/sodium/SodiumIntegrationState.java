package com.erodev.sodiumrelief.compat.sodium;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SodiumIntegrationState {
    private static final AtomicBoolean SIDEBAR_REGISTERED = new AtomicBoolean(false);

    private SodiumIntegrationState() {
    }

    public static void markSidebarRegistered() {
        SIDEBAR_REGISTERED.set(true);
    }

    public static boolean isSidebarRegistered() {
        return SIDEBAR_REGISTERED.get();
    }
}
