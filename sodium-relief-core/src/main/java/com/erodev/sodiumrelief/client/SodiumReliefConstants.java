package com.erodev.sodiumrelief.client;

/**
 * Pure-Java identity constants shared between platform modules and the core module.
 * Lives in {@code sodium-relief-core} so that classes which must not depend on
 * Minecraft / Fabric (logger, config manager, etc.) can still reference the mod id.
 */
public final class SodiumReliefConstants {
    public static final String MOD_ID = "sodiumrelief";
    public static final String MOD_NAME = "Sodium Relief";

    private SodiumReliefConstants() {
    }
}
