package com.erodev.sodiumrelief.compat.sodium;

import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.util.Identifier;

public final class SodiumConfigEntryPoint implements ConfigEntryPoint {
    private static final Identifier ICON_ID = Identifier.of(SodiumReliefClient.MOD_ID, "icon.png");
    private static final String MOD_VERSION = FabricLoader.getInstance()
        .getModContainer(SodiumReliefClient.MOD_ID)
        .map(container -> container.getMetadata().getVersion().getFriendlyString())
        .orElse("unknown");

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        try {
            SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
            if (runtime == null) {
                ReliefLogger.warn("Skipped native Sodium page registration because runtime was not initialized yet");
                return;
            }

            ModOptionsBuilder modOptions = builder.registerModOptions(
                SodiumReliefClient.MOD_ID,
                SodiumReliefClient.MOD_NAME,
                MOD_VERSION
            );

            modOptions.setIcon(ICON_ID);
            modOptions.addPage(new SodiumOptionPageFactory(runtime).create(builder));
            SodiumIntegrationState.markSidebarRegistered();
            ReliefLogger.info("Registered native Sodium Relief options page in Sodium config sidebar");
        } catch (Throwable throwable) {
            ReliefLogger.error("Failed to attach Sodium sidebar integration", throwable);
        }
    }
}
