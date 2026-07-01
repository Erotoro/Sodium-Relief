package com.erodev.sodiumrelief.util;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefMode;
import net.minecraft.network.chat.Component;

public final class ReliefTexts {
    private ReliefTexts() {
    }

    public static Component modName() {
        return Component.translatable("sodiumrelief.mod_name");
    }

    public static Component screen(String key) {
        return Component.translatable("sodiumrelief.screen." + key);
    }

    public static Component button(String key) {
        return Component.translatable("sodiumrelief.button." + key);
    }

    public static Component category(String key) {
        return Component.translatable("sodiumrelief.category." + key);
    }

    public static Component option(String key) {
        return Component.translatable("sodiumrelief.option." + key);
    }

    public static Component desc(String key) {
        return Component.translatable("sodiumrelief.desc." + key);
    }

    public static Component formatLabelValue(Component label, Component value) {
        return Component.translatable("sodiumrelief.format.label_value", label, value);
    }

    public static Component toggleValue(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }

    public static Component boolValue(boolean value) {
        return Component.translatable(value ? "gui.yes" : "gui.no");
    }

    public static Component milliseconds(int value) {
        return Component.translatable("sodiumrelief.unit.ms", value);
    }

    public static Component milliseconds(long value) {
        return Component.translatable("sodiumrelief.unit.ms", value);
    }

    public static Component adaptiveMode(AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> Component.translatable("sodiumrelief.value.adaptive_mode.safe");
            case BALANCED -> Component.translatable("sodiumrelief.value.adaptive_mode.balanced");
            case AGGRESSIVE -> Component.translatable("sodiumrelief.value.adaptive_mode.aggressive");
        };
    }

    public static Component reliefMode(ReliefMode mode) {
        return switch (mode) {
            case CONSERVATIVE -> Component.translatable("sodiumrelief.value.relief_mode.conservative");
            case BALANCED -> Component.translatable("sodiumrelief.value.relief_mode.balanced");
            case AGGRESSIVE -> Component.translatable("sodiumrelief.value.relief_mode.aggressive");
        };
    }

    public static Component hoverState(String state) {
        return switch (state) {
            case "MOVING" -> Component.translatable("sodiumrelief.value.hover_state.moving");
            case "PREDICTIVE" -> Component.translatable("sodiumrelief.value.hover_state.predictive");
            default -> Component.translatable("sodiumrelief.value.hover_state.stable");
        };
    }

    public static Component overlay(String key, Object... args) {
        return Component.translatable("sodiumrelief.debug." + key, args);
    }
}
