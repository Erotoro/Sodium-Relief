package com.erodev.sodiumrelief.compat.sodium;

import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.config.ReliefMode;
import com.erodev.sodiumrelief.util.ReliefTexts;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SodiumOptionPageFactory {
    private final SodiumReliefRuntime runtime;
    private final ReliefConfig config;
    private final ReliefConfig defaults = new ReliefConfig();

    public SodiumOptionPageFactory(SodiumReliefRuntime runtime) {
        this.runtime = runtime;
        this.config = runtime.configManager().config();
    }

    public OptionPageBuilder create(ConfigBuilder builder) {
        return builder.createOptionPage()
            .setName(ReliefTexts.modName())
            .addOptionGroup(generalGroup(builder))
            .addOptionGroup(tooltipsGroup(builder))
            .addOptionGroup(hoverGroup(builder))
            .addOptionGroup(compatibilityGroup(builder))
            .addOptionGroup(advancedGroup(builder));
    }

    private OptionGroupBuilder generalGroup(ConfigBuilder builder) {
        return group(builder, "general")
            .addOption(bool(builder, "enable_mod", () -> config.enableMod, value -> config.enableMod = value));
    }

    private OptionGroupBuilder tooltipsGroup(ConfigBuilder builder) {
        return group(builder, "tooltips")
            .addOption(bool(builder, "tooltip_cache", () -> config.enableTooltipLayoutCache, value -> config.enableTooltipLayoutCache = value))
            .addOption(bool(builder, "lazy_evaluation", () -> config.enableLazyEvaluation, value -> config.enableLazyEvaluation = value));
    }

    private OptionGroupBuilder hoverGroup(ConfigBuilder builder) {
        return group(builder, "hover")
            .addOption(bool(builder, "hover_stabilization", () -> config.enableHoverSmoothing, value -> config.enableHoverSmoothing = value))
            .addOption(enumOption(
                builder,
                "adaptive_mode",
                AdaptiveMode.class,
                Objects.requireNonNull(defaults.adaptiveMode),
                () -> Objects.requireNonNull(config.adaptiveMode),
                value -> config.adaptiveMode = value,
                ReliefTexts::adaptiveMode
            ))
            .addOption(bool(builder, "hover_prediction", () -> config.enableHoverPrediction, value -> config.enableHoverPrediction = value))
            .addOption(intOption(builder, "hover_stability_window", () -> config.hoverPredictionDelayMillis, value -> config.hoverPredictionDelayMillis = value, 8, 60, 2, millisecondsFormatter()))
            .addOption(enumOption(
                builder,
                "hover_mode",
                ReliefMode.class,
                Objects.requireNonNull(defaults.hoverMode),
                () -> Objects.requireNonNull(config.hoverMode),
                value -> config.hoverMode = value,
                ReliefTexts::reliefMode
            ));
    }

    private OptionGroupBuilder compatibilityGroup(ConfigBuilder builder) {
        return group(builder, "compatibility")
            .addOption(bool(builder, "optimize_inventory_screens", () -> config.optimizeInventoryScreens, value -> config.optimizeInventoryScreens = value))
            .addOption(bool(builder, "strict_tooltip_invalidation", () -> config.strictTooltipInvalidation, value -> config.strictTooltipInvalidation = value))
            .addOption(bool(builder, "extra_safety_checks", () -> config.extraSafetyChecks, value -> config.extraSafetyChecks = value));
    }

    private OptionGroupBuilder advancedGroup(ConfigBuilder builder) {
        return group(builder, "advanced")
            .addOption(bool(builder, "debug_logging", () -> config.debugLogging, value -> config.debugLogging = value))
            .addOption(bool(builder, "debug_overlay", () -> config.debugOverlay, value -> config.debugOverlay = value));
    }

    private OptionGroupBuilder group(ConfigBuilder builder, String key) {
        return builder.createOptionGroup().setName(ReliefTexts.category(key));
    }

    private BooleanOptionBuilder bool(ConfigBuilder builder, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return builder.createBooleanOption(id(key))
            .setName(ReliefTexts.option(key))
            .setTooltip(ReliefTexts.desc(key))
            .setDefaultValue(defaultBooleanValue(key))
            .setStorageHandler(runtime::saveAndReapplyConfig)
            .setBinding(bindingSaver(setter), getter)
            .setApplyHook(ignored -> runtime.reapplyConfig());
    }

    private IntegerOptionBuilder intOption(
        ConfigBuilder builder,
        String key,
        Supplier<Integer> getter,
        Consumer<Integer> setter,
        int min,
        int max,
        int step,
        ControlValueFormatter formatter
    ) {
        return builder.createIntegerOption(id(key))
            .setName(ReliefTexts.option(key))
            .setTooltip(ReliefTexts.desc(key))
            .setDefaultValue(defaultIntValue(key))
            .setStorageHandler(runtime::saveAndReapplyConfig)
            .setRange(min, max, step)
            .setValueFormatter(formatter)
            .setBinding(bindingSaver(setter), getter)
            .setApplyHook(ignored -> runtime.reapplyConfig());
    }

    private <E extends Enum<E>> EnumOptionBuilder<E> enumOption(
        ConfigBuilder builder,
        String key,
        Class<E> enumClass,
        E defaultValue,
        Supplier<E> getter,
        Consumer<E> setter,
        Function<E, Text> valueText
    ) {
        return builder.createEnumOption(id(key), enumClass)
            .setName(ReliefTexts.option(key))
            .setTooltip(ReliefTexts.desc(key))
            .setDefaultValue(defaultValue)
            .setStorageHandler(runtime::saveAndReapplyConfig)
            .setAllowedValues(EnumSet.allOf(enumClass))
            .setElementNameProvider(value -> valueText.apply(Objects.requireNonNull(value)))
            .setBinding(bindingSaver(setter), getter)
            .setApplyHook(ignored -> runtime.reapplyConfig());
    }

    private static Identifier id(String path) {
        return Identifier.of(SodiumReliefClient.MOD_ID, path);
    }

    private static <T> Consumer<T> bindingSaver(Consumer<T> setter) {
        return value -> setter.accept(requireValue(value));
    }

    private static ControlValueFormatter millisecondsFormatter() {
        return ReliefTexts::milliseconds;
    }

    private Boolean defaultBooleanValue(String path) {
        return switch (path) {
            case "enable_mod" -> defaults.enableMod;
            case "tooltip_cache" -> defaults.enableTooltipLayoutCache;
            case "lazy_evaluation" -> defaults.enableLazyEvaluation;
            case "strict_tooltip_invalidation" -> defaults.strictTooltipInvalidation;
            case "hover_stabilization" -> defaults.enableHoverSmoothing;
            case "hover_prediction" -> defaults.enableHoverPrediction;
            case "optimize_inventory_screens" -> defaults.optimizeInventoryScreens;
            case "debug_logging" -> defaults.debugLogging;
            case "debug_overlay" -> defaults.debugOverlay;
            case "extra_safety_checks" -> defaults.extraSafetyChecks;
            default -> throw new IllegalArgumentException("Unknown boolean default path: " + path);
        };
    }

    private Integer defaultIntValue(String path) {
        return switch (path) {
            case "hover_stability_window" -> defaults.hoverPredictionDelayMillis;
            default -> throw new IllegalArgumentException("Unknown integer default path: " + path);
        };
    }

    private static <T> T requireValue(T value) {
        return Objects.requireNonNull(value);
    }
}
