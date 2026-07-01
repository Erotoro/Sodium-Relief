package com.erodev.sodiumrelief.compat.sodium;

import com.erodev.sodiumrelief.compat.modmenu.ReliefConfigScreenFactory;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.ReliefTexts;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

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

    private void afterInit(net.minecraft.client.MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (SodiumIntegrationState.isSidebarRegistered()) {
            return;
        }
        if (!(screen instanceof OptionsScreen) && !(screen instanceof VideoOptionsScreen)) {
            return;
        }

        Screens.getButtons(screen).add(ButtonWidget.builder(ReliefTexts.modName(), button -> {
            client.setScreen(ReliefConfigScreenFactory.create(screen));
        }).dimensions(8, Math.max(8, scaledHeight - 28), 110, 20).build());
    }
}
