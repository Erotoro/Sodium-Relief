package com.erodev.sodiumrelief.compat.modmenu;

import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.config.ReliefMode;
import com.erodev.sodiumrelief.util.ReliefTexts;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SodiumReliefConfigScreen extends Screen {
    private final Screen parent;
    private final SodiumReliefRuntime runtime;
    private Category category = Category.GENERAL;

    public SodiumReliefConfigScreen(Screen parent, SodiumReliefRuntime runtime) {
        super(ReliefTexts.modName());
        this.parent = parent;
        this.runtime = runtime;
    }

    @Override
    protected void init() {
        clearChildren();
        buildCategories();
        buildOptions(runtime.configManager().config());
        addDrawableChild(ButtonWidget.builder(ReliefTexts.button("reset_caches"), button -> runtime.cacheInvalidationManager().invalidateAll()).dimensions(width - 210, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(ReliefTexts.button("done"), button -> close()).dimensions(width - 106, height - 28, 100, 20).build());
    }

    @Override
    public void close() {
        runtime.saveAndReapplyConfig();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
    }

    private void buildCategories() {
        int x = 8;
        int y = 36;
        for (Category value : Category.values()) {
            Category current = value;
            addDrawableChild(ButtonWidget.builder(current.title, button -> {
                category = current;
                clearAndInit();
            }).dimensions(x, y, 92, 20).build());
            x += 96;
            if (x + 92 > width - 8) {
                x = 8;
                y += 24;
            }
        }
    }

    private void buildOptions(ReliefConfig config) {
        switch (category) {
            case GENERAL -> {
                addDrawableChild(toggle(0, "enable_mod", () -> config.enableMod, value -> config.enableMod = value));
            }
            case TOOLTIPS -> {
                addDrawableChild(toggle(0, "tooltip_cache", () -> config.enableTooltipLayoutCache, value -> config.enableTooltipLayoutCache = value));
                addDrawableChild(toggle(1, "lazy_evaluation", () -> config.enableLazyEvaluation, value -> config.enableLazyEvaluation = value));
            }
            case HOVER -> {
                addDrawableChild(toggle(0, "hover_stabilization", () -> config.enableHoverSmoothing, value -> config.enableHoverSmoothing = value));
                addDrawableChild(enumCycle(1, "adaptive_mode", () -> Objects.requireNonNull(config.adaptiveMode), value -> config.adaptiveMode = value, AdaptiveMode.values(), ReliefTexts::adaptiveMode));
                addDrawableChild(toggle(2, "hover_prediction", () -> config.enableHoverPrediction, value -> config.enableHoverPrediction = value));
                addDrawableChild(intCycle(3, "hover_stability_window", () -> config.hoverPredictionDelayMillis, value -> config.hoverPredictionDelayMillis = value, new int[]{8, 12, 16, 24, 32, 40, 60}, ReliefTexts::milliseconds));
                addDrawableChild(enumCycle(4, "hover_mode", () -> Objects.requireNonNull(config.hoverMode), value -> config.hoverMode = value, ReliefMode.values(), ReliefTexts::reliefMode));
            }
            case COMPATIBILITY -> {
                addDrawableChild(toggle(0, "optimize_inventory_screens", () -> config.optimizeInventoryScreens, value -> config.optimizeInventoryScreens = value));
                addDrawableChild(toggle(1, "strict_tooltip_invalidation", () -> config.strictTooltipInvalidation, value -> config.strictTooltipInvalidation = value));
                addDrawableChild(toggle(2, "extra_safety_checks", () -> config.extraSafetyChecks, value -> config.extraSafetyChecks = value));
            }
            case ADVANCED -> {
                addDrawableChild(toggle(0, "debug_logging", () -> config.debugLogging, value -> config.debugLogging = value));
                addDrawableChild(toggle(1, "debug_overlay", () -> config.debugOverlay, value -> config.debugOverlay = value));
            }
        }
    }

    private ButtonWidget toggle(int row, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return button(row, () -> ReliefTexts.formatLabelValue(ReliefTexts.option(key), ReliefTexts.toggleValue(getter.get())), button -> {
            setter.accept(!getter.get());
            button.setMessage(ReliefTexts.formatLabelValue(ReliefTexts.option(key), ReliefTexts.toggleValue(getter.get())));
        });
    }

    private ButtonWidget intCycle(int row, String key, Supplier<Integer> getter, Consumer<Integer> setter, int[] values, IntFunction<Text> valueFormatter) {
        return button(row, () -> ReliefTexts.formatLabelValue(ReliefTexts.option(key), valueFormatter.apply(getter.get())), button -> {
            setter.accept(next(values, getter.get()));
            button.setMessage(ReliefTexts.formatLabelValue(ReliefTexts.option(key), valueFormatter.apply(getter.get())));
        });
    }

    private <E extends Enum<E>> ButtonWidget enumCycle(int row, String key, Supplier<E> getter, Consumer<E> setter, E[] values, Function<E, Text> valueText) {
        return button(row, () -> ReliefTexts.formatLabelValue(ReliefTexts.option(key), valueText.apply(Objects.requireNonNull(getter.get()))), button -> {
            E current = Objects.requireNonNull(getter.get());
            E nextValue = Objects.requireNonNull(values[(current.ordinal() + 1) % values.length]);
            setter.accept(nextValue);
            button.setMessage(ReliefTexts.formatLabelValue(ReliefTexts.option(key), valueText.apply(Objects.requireNonNull(getter.get()))));
        });
    }

    private ButtonWidget button(int row, Supplier<Text> supplier, Consumer<ButtonWidget> action) {
        int top = 96;
        return ButtonWidget.builder(supplier.get(), action::accept).dimensions(20, top + row * 24, 360, 20).build();
    }

    private static int next(int[] values, int current) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) {
                return values[(index + 1) % values.length];
            }
        }
        return values[0];
    }

    private enum Category {
        GENERAL(ReliefTexts.category("general")),
        TOOLTIPS(ReliefTexts.category("tooltips")),
        HOVER(ReliefTexts.category("hover")),
        COMPATIBILITY(ReliefTexts.category("compatibility")),
        ADVANCED(ReliefTexts.category("advanced"));

        private final Text title;

        Category(Text title) {
            this.title = title;
        }
    }
}
