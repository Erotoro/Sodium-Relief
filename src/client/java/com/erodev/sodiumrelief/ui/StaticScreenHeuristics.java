package com.erodev.sodiumrelief.ui;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;

public final class StaticScreenHeuristics {
    public ScreenType classify(Screen screen) {
        if (screen instanceof HandledScreen<?>) {
            return ScreenType.INVENTORY_LIKE;
        }

        String className = screen.getClass().getName();
        if (screen instanceof GameOptionsScreen || isVanillaOptionLike(className)) {
            return ScreenType.OPTION_LIKE;
        }
        if (screen instanceof TitleScreen || screen instanceof GameMenuScreen || isKnownVanillaScreen(className)) {
            return ScreenType.KNOWN_VANILLA;
        }
        return ScreenType.UNKNOWN_MODDED;
    }

    public enum ScreenType {
        INVENTORY_LIKE,
        KNOWN_VANILLA,
        OPTION_LIKE,
        UNKNOWN_MODDED
    }

    private static boolean isKnownVanillaScreen(String className) {
        return className.startsWith("net.minecraft.client.gui.screen.")
            && !isVanillaOptionLike(className)
            && !className.startsWith("net.minecraft.client.gui.screen.ingame.");
    }

    private static boolean isVanillaOptionLike(String className) {
        return className.startsWith("net.minecraft.client.gui.screen.option.")
            || className.endsWith("OptionsScreen")
            || className.endsWith("PackScreen")
            || className.endsWith("ControlsOptionsScreen")
            || className.endsWith("AccessibilityOptionsScreen")
            || className.endsWith("CustomizeBuffetLevelScreen")
            || className.endsWith("ExperimentsScreen");
    }
}
