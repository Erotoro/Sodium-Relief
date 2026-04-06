package com.erodev.sodiumrelief.compat.modmenu;

import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.ReliefTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.MessageScreen;

public final class ReliefConfigScreenFactory {
    private ReliefConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
        if (runtime == null) {
            ReliefLogger.warn("Config screen requested before runtime initialization completed");
            return new MessageScreen(ReliefTexts.screen("initializing"));
        }

        return new SodiumReliefConfigScreen(parent, runtime);
    }
}
