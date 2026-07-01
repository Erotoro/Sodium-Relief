package com.erodev.sodiumrelief.client;

import net.fabricmc.api.ClientModInitializer;

public final class SodiumReliefClient implements ClientModInitializer {
    public static final String MOD_ID = SodiumReliefConstants.MOD_ID;
    public static final String MOD_NAME = SodiumReliefConstants.MOD_NAME;

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
