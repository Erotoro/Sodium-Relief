package com.erodev.sodiumrelief.compat.sodium;

import com.erodev.sodiumrelief.debug.ReliefLogger;
import net.fabricmc.loader.api.FabricLoader;

public final class SodiumCompat {
    private final boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
    private final boolean reesesLoaded = FabricLoader.getInstance().isModLoaded("reeses-sodium-options");
    private final boolean modMenuLoaded = FabricLoader.getInstance().isModLoaded("modmenu");
    private final FallbackConfigButtonInjector fallbackConfigButtonInjector = new FallbackConfigButtonInjector();
    private int startupTicks;
    private boolean sidebarStatusLogged;

    public void initialize() {
        ReliefLogger.info("Sodium detected: " + yesNo(sodiumLoaded));
        ReliefLogger.info("Reese's Sodium Options detected: " + yesNo(reesesLoaded));

        if (sodiumLoaded) {
            ReliefLogger.info("Sodium sidebar integration registration path: Fabric entrypoint 'sodium:config_api_user'");
        } else {
            ReliefLogger.info("Sodium sidebar integration skipped because Sodium is not installed");
        }

        if (modMenuLoaded) {
            ReliefLogger.info("Fallback config entry attached via Mod Menu");
        } else {
            fallbackConfigButtonInjector.initialize();
        }
    }

    public boolean isSodiumLoaded() {
        return sodiumLoaded;
    }

    public boolean isReesesLoaded() {
        return reesesLoaded;
    }

    public boolean isModMenuLoaded() {
        return modMenuLoaded;
    }

    public void tick() {
        if (!sodiumLoaded || sidebarStatusLogged) {
            return;
        }

        startupTicks++;
        if (startupTicks < 40) {
            return;
        }

        sidebarStatusLogged = true;
        if (SodiumIntegrationState.isSidebarRegistered()) {
            ReliefLogger.info("Sodium sidebar integration attached successfully");
            return;
        }

        ReliefLogger.warn("Sodium sidebar integration was not attached. Using fallback config entry instead.");
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
