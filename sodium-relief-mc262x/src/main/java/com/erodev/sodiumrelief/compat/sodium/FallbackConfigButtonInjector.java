package com.erodev.sodiumrelief.compat.sodium;

import com.erodev.sodiumrelief.compat.modmenu.ReliefConfigScreenFactory;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.ReliefTexts;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;

public final class FallbackConfigButtonInjector {
    private boolean initialized;

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ScreenEvents.AFTER_INIT.register(this::afterInit);
        ReliefLogger.info("Fallback config entry attached via vanilla options screen button");
    }

    private void afterInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (SodiumIntegrationState.isSidebarRegistered()) {
            return;
        }
        if (!(screen instanceof OptionsScreen) && !(screen instanceof VideoSettingsScreen)) {
            return;
        }

        Screens.getWidgets(screen).add(Button.builder(ReliefTexts.modName(), button -> {
            client.setScreenAndShow(ReliefConfigScreenFactory.create(screen));
        }).bounds(8, Math.max(8, scaledHeight - 28), 110, 20).build());
    }
}
