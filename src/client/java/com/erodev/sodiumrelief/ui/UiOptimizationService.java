package com.erodev.sodiumrelief.ui;

import com.erodev.sodiumrelief.config.ReliefConfig;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.Screen;

public final class UiOptimizationService {
    public boolean shouldOptimize(Screen screen, ReliefConfig config) {
        if (!config.enableMod || !config.enableUiOptimization) {
            return false;
        }

        return config.optimizeInventoryScreens && screen instanceof HandledScreen<?>;
    }
}
