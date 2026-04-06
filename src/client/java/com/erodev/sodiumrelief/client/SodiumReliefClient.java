package com.erodev.sodiumrelief.client;

import net.fabricmc.api.ClientModInitializer;

public final class SodiumReliefClient implements ClientModInitializer {
    public static final String MOD_ID = "sodiumrelief";
    public static final String MOD_NAME = "Sodium Relief";

    private static SodiumReliefRuntime runtime;

    @Override
    public void onInitializeClient() {
        runtime = new SodiumReliefRuntime();
        runtime.initialize();
    }

    public static SodiumReliefRuntime runtime() {
        return runtime;
    }
}
