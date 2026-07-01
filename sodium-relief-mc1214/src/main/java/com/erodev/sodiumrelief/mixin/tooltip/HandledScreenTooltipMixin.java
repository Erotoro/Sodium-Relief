package com.erodev.sodiumrelief.mixin.tooltip;

import com.erodev.sodiumrelief.cache.TooltipLayoutCache.CachedTooltipLayout;
import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.hover.HoverAssessment;
import com.erodev.sodiumrelief.tooltip.TooltipPresentationService.TooltipTiming;
import com.erodev.sodiumrelief.util.SafeTime;
import com.erodev.sodiumrelief.util.TooltipFingerprint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HandledScreen.class)
public abstract class HandledScreenTooltipMixin {
    @Shadow protected Slot focusedSlot;

    private Class<?> sodiumRelief$lastScreenClass;
    private int sodiumRelief$lastSlotId = Integer.MIN_VALUE;
    private ItemStack sodiumRelief$lastStack;
    private Item sodiumRelief$lastItem;
    private ComponentChanges sodiumRelief$lastComponentChanges;
    private int sodiumRelief$lastItemCount = Integer.MIN_VALUE;
    private boolean sodiumRelief$lastAdvancedTooltips;
    private String sodiumRelief$lastLanguage;
    private TooltipFingerprint sodiumRelief$lastTooltipFingerprint;

    /**
     * Cooperative wrap (MixinExtras) around the tooltip preparation call inside
     * {@code drawMouseoverTooltip}. Unlike {@code @Redirect}, this coexists with
     * other mods that wrap the same invocation: on a cache miss we call
     * {@code original} (so their logic still runs), and on a hit we serve the
     * cached layout and intentionally skip recomputation entirely.
     */
    @WrapOperation(
        method = "drawMouseoverTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;"
        )
    )
    private List<Text> sodiumRelief$optimizeTooltipPreparation(
        HandledScreen<?> screen,
        ItemStack stack,
        Operation<List<Text>> original,
        @Local(argsOnly = true, ordinal = 0) int mouseX,
        @Local(argsOnly = true, ordinal = 1) int mouseY
    ) {
        SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
        if (runtime == null) {
            return original.call(screen, stack);
        }

        ReliefConfig config = runtime.configManager().config();
        if (!config.enableMod) {
            return original.call(screen, stack);
        }

        runtime.metrics().tooltipPathInvocation();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || focusedSlot == null || !focusedSlot.hasStack()) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return original.call(screen, stack);
        }

        if (!runtime.uiOptimizationService().shouldOptimize(screen, config)) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return original.call(screen, stack);
        }

        if (config.extraSafetyChecks && config.strictTooltipInvalidation && stack.getTooltipData().isPresent()) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return original.call(screen, stack);
        }

        boolean detailed = runtime.metrics().detailedMetricsEnabled();
        Class<?> screenClass = screen.getClass();
        int slotId = focusedSlot.id;
        boolean advancedTooltips = client.options.advancedItemTooltips;
        String language = client.options.language;
        ComponentChanges componentChanges = stack.getComponentChanges();
        Item item = stack.getItem();
        runtime.metrics().tooltipFastPathCheckPerformed();
        long fastPathCheckStartNanos = detailed ? SafeTime.nowNanos() : 0L;
        TooltipFingerprint fingerprint = sodiumRelief$checkFastTooltipFingerprint(
            runtime,
            screenClass,
            slotId,
            stack,
            item,
            componentChanges,
            advancedTooltips,
            language
        );
        if (detailed) {
            runtime.metrics().tooltipFastPathCheckTime(SafeTime.nowNanos() - fastPathCheckStartNanos);
        }
        if (fingerprint == null) {
            fingerprint = sodiumRelief$buildTooltipFingerprint(
                runtime,
                detailed,
                screenClass,
                slotId,
                stack,
                item,
                componentChanges,
                advancedTooltips,
                language
            );
        }
        long nowNanos = SafeTime.nowNanos();
        long nowMillis = nowNanos / 1_000_000L;
        runtime.tooltipPresentationService().updateHover(fingerprint, nowNanos);

        runtime.hoverSmoothingService().recordSample(
            config,
            fingerprint.screenClass(),
            fingerprint.slotId(),
            fingerprint.itemRawId(),
            fingerprint.itemCount(),
            fingerprint.componentHash(),
            mouseX,
            mouseY,
            nowNanos
        );

        runtime.metrics().tooltipCacheLookupPerformed();
        long cacheLookupStartNanos = detailed ? SafeTime.nowNanos() : 0L;
        CachedTooltipLayout cachedLayout = runtime.tooltipLayoutCache().get(fingerprint, nowMillis);
        if (detailed) {
            runtime.metrics().tooltipCacheLookupTime(SafeTime.nowNanos() - cacheLookupStartNanos);
        }
        if (cachedLayout != null) {
            runtime.tooltipPresentationService().remember(fingerprint, cachedLayout);
            return cachedLayout.lines();
        }

        HoverAssessment hoverAssessment = runtime.hoverSmoothingService().assessFallbackNeed(config);
        if (shouldUseFallback(config, hoverAssessment)) {
            long fallbackStartNanos = detailed ? SafeTime.nowNanos() : 0L;
            runtime.metrics().tooltipFallbackEvaluation();
            TooltipTiming tooltipTiming = runtime.tooltipPresentationService().timing(nowNanos);
            CachedTooltipLayout fallbackLayout = runtime.tooltipPresentationService().fallback(fingerprint, tooltipTiming).orElse(null);
            long fallbackElapsedNanos = detailed ? SafeTime.nowNanos() - fallbackStartNanos : 0L;
            if (detailed) {
                runtime.metrics().tooltipFallbackEvaluationTime(fallbackElapsedNanos);
            }
            if (fallbackLayout != null) {
                runtime.metrics().tooltipSuppressed();
                runtime.metrics().tooltipReused();
                if (detailed) {
                    runtime.metrics().tooltipFallbackReuseTime(fallbackElapsedNanos);
                }
                return fallbackLayout.lines();
            }

            runtime.metrics().tooltipFallbackMiss();
            if (detailed) {
                runtime.metrics().tooltipFallbackMissTime(fallbackElapsedNanos);
            }
            if (tooltipTiming.guaranteedRenderWindowReached() || tooltipTiming.hardMaxDelayReached()) {
                runtime.metrics().tooltipForced();
                runtime.metrics().tooltipForcedRender();
                if (detailed) {
                    runtime.metrics().tooltipForcedRenderTime(fallbackElapsedNanos);
                }
            }
        }

        long tooltipBuildStartNanos = detailed ? SafeTime.nowNanos() : 0L;
        List<Text> lines = List.copyOf(original.call(screen, stack));
        if (detailed) {
            runtime.metrics().tooltipBuild(SafeTime.nowNanos() - tooltipBuildStartNanos);
        }
        CachedTooltipLayout computed = new CachedTooltipLayout(lines);
        runtime.tooltipLayoutCache().put(fingerprint, computed, nowMillis);
        runtime.tooltipPresentationService().remember(fingerprint, computed);
        return lines;
    }

    private static boolean shouldUseFallback(ReliefConfig config, HoverAssessment hoverAssessment) {
        return hoverAssessment.skipRedundant() || (config.enableLazyEvaluation && hoverAssessment.deferHeavyWork());
    }

    private TooltipFingerprint sodiumRelief$checkFastTooltipFingerprint(
        SodiumReliefRuntime runtime,
        Class<?> screenClass,
        int slotId,
        ItemStack stack,
        Item item,
        ComponentChanges componentChanges,
        boolean advancedTooltips,
        String language
    ) {
        if (sodiumRelief$lastTooltipFingerprint != null
            && sodiumRelief$lastScreenClass == screenClass
            && sodiumRelief$lastSlotId == slotId
            && sodiumRelief$lastStack == stack
            && sodiumRelief$lastItem == item
            && sodiumRelief$lastComponentChanges == componentChanges
            && sodiumRelief$lastItemCount == stack.getCount()
            && sodiumRelief$lastAdvancedTooltips == advancedTooltips
            && language.equals(sodiumRelief$lastLanguage)) {
            runtime.metrics().tooltipFastPathHit();
            return sodiumRelief$lastTooltipFingerprint;
        }
        return null;
    }

    private TooltipFingerprint sodiumRelief$buildTooltipFingerprint(
        SodiumReliefRuntime runtime,
        boolean detailed,
        Class<?> screenClass,
        int slotId,
        ItemStack stack,
        Item item,
        ComponentChanges componentChanges,
        boolean advancedTooltips,
        String language
    ) {
        long expensivePathStartNanos = detailed ? SafeTime.nowNanos() : 0L;
        int itemRawId = Registries.ITEM.getRawId(stack.getItem());
        TooltipFingerprint fingerprint = new TooltipFingerprint(
            screenClass,
            slotId,
            itemRawId,
            stack.getCount(),
            componentChanges.hashCode(),
            advancedTooltips,
            language
        );
        sodiumRelief$lastScreenClass = screenClass;
        sodiumRelief$lastSlotId = slotId;
        sodiumRelief$lastStack = stack;
        sodiumRelief$lastItem = item;
        sodiumRelief$lastComponentChanges = componentChanges;
        sodiumRelief$lastItemCount = fingerprint.itemCount();
        sodiumRelief$lastAdvancedTooltips = advancedTooltips;
        sodiumRelief$lastLanguage = language;
        sodiumRelief$lastTooltipFingerprint = fingerprint;
        runtime.metrics().tooltipExpensivePathInvocation();
        if (detailed) {
            runtime.metrics().tooltipFingerprintPathTime(SafeTime.nowNanos() - expensivePathStartNanos);
        }
        return fingerprint;
    }
}
