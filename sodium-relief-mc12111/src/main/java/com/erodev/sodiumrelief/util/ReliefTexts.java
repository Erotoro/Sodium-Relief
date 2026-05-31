package com.erodev.sodiumrelief.util;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefMode;
import net.minecraft.text.Text;

public final class ReliefTexts {
    private ReliefTexts() {
    }

    public static Text modName() {
        return Text.translatable("sodiumrelief.mod_name");
    }

    public static Text screen(String key) {
        return Text.translatable("sodiumrelief.screen." + key);
    }

    public static Text button(String key) {
        return Text.translatable("sodiumrelief.button." + key);
    }

    public static Text category(String key) {
        return Text.translatable("sodiumrelief.category." + key);
    }

    public static Text option(String key) {
        return Text.translatable("sodiumrelief.option." + key);
    }

    public static Text desc(String key) {
        return Text.translatable("sodiumrelief.desc." + key);
    }

    public static Text formatLabelValue(Text label, Text value) {
        return Text.translatable("sodiumrelief.format.label_value", label, value);
    }

    public static Text toggleValue(boolean enabled) {
        return Text.translatable(enabled ? "options.on" : "options.off");
    }

    public static Text boolValue(boolean value) {
        return Text.translatable(value ? "gui.yes" : "gui.no");
    }

    public static Text milliseconds(int value) {
        return Text.translatable("sodiumrelief.unit.ms", value);
    }

    public static Text milliseconds(long value) {
        return Text.translatable("sodiumrelief.unit.ms", value);
    }

    public static Text adaptiveMode(AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> Text.translatable("sodiumrelief.value.adaptive_mode.safe");
            case BALANCED -> Text.translatable("sodiumrelief.value.adaptive_mode.balanced");
            case AGGRESSIVE -> Text.translatable("sodiumrelief.value.adaptive_mode.aggressive");
        };
    }

    public static Text reliefMode(ReliefMode mode) {
        return switch (mode) {
            case CONSERVATIVE -> Text.translatable("sodiumrelief.value.relief_mode.conservative");
            case BALANCED -> Text.translatable("sodiumrelief.value.relief_mode.balanced");
            case AGGRESSIVE -> Text.translatable("sodiumrelief.value.relief_mode.aggressive");
        };
    }

    public static Text hoverState(String state) {
        return switch (state) {
            case "MOVING" -> Text.translatable("sodiumrelief.value.hover_state.moving");
            case "PREDICTIVE" -> Text.translatable("sodiumrelief.value.hover_state.predictive");
            default -> Text.translatable("sodiumrelief.value.hover_state.stable");
        };
    }

    public static Text overlay(String key, Object... args) {
        return Text.translatable("sodiumrelief.debug." + key, args);
    }
}
