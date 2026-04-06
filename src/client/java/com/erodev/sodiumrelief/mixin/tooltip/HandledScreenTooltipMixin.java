package com.erodev.sodiumrelief.mixin.tooltip;

import com.erodev.sodiumrelief.cache.TooltipLayoutCache.CachedTooltipLayout;
import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.hover.HoverAssessment;
import com.erodev.sodiumrelief.tooltip.TooltipPresentationService.TooltipTiming;
import com.erodev.sodiumrelief.util.SafeTime;
import com.erodev.sodiumrelief.util.TooltipFingerprint;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HandledScreen.class)
public abstract class HandledScreenTooltipMixin {
    @Shadow protected Slot focusedSlot;
    @Shadow protected abstract List<Text> getTooltipFromItem(ItemStack stack);

    private Class<?> sodiumRelief$lastScreenClass;
    private int sodiumRelief$lastSlotId = Integer.MIN_VALUE;
    private ItemStack sodiumRelief$lastStack;
    private Item sodiumRelief$lastItem;
    private ComponentChanges sodiumRelief$lastComponentChanges;
    private int sodiumRelief$lastItemCount = Integer.MIN_VALUE;
    private boolean sodiumRelief$lastAdvancedTooltips;
    private String sodiumRelief$lastLanguage;
    private TooltipFingerprint sodiumRelief$lastTooltipFingerprint;

    @Redirect(
        method = "drawMouseoverTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;"
        )
    )
    private List<Text> sodiumRelief$optimizeTooltipPreparation(HandledScreen<?> screen, ItemStack stack, DrawContext context, int mouseX, int mouseY) {
        SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
        if (runtime == null) {
            return getTooltipFromItem(stack);
        }
        runtime.metrics().tooltipPathInvocation();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || focusedSlot == null || !focusedSlot.hasStack()) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return getTooltipFromItem(stack);
        }

        ReliefConfig config = runtime.configManager().config();
        if (!runtime.uiOptimizationService().shouldOptimize(screen, config)) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return getTooltipFromItem(stack);
        }

        if (config.extraSafetyChecks && config.strictTooltipInvalidation && stack.getTooltipData().isPresent()) {
            runtime.cacheInvalidationManager().resetTooltipContext();
            return getTooltipFromItem(stack);
        }

        Class<?> screenClass = screen.getClass();
        int slotId = focusedSlot.id;
        boolean advancedTooltips = client.options.advancedItemTooltips;
        String language = client.options.language;
        ComponentChanges componentChanges = stack.getComponentChanges();
        Item item = stack.getItem();
        runtime.metrics().tooltipFastPathCheckPerformed();
        long fastPathCheckStartNanos = SafeTime.nowNanos();
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
        runtime.metrics().tooltipFastPathCheckTime(SafeTime.nowNanos() - fastPathCheckStartNanos);
        if (fingerprint == null) {
            fingerprint = sodiumRelief$buildTooltipFingerprint(
                runtime,
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
        runtime.tooltipPresentationService().updateHover(fingerprint, nowNanos);

        HoverAssessment hoverAssessment = runtime.hoverSmoothingService().assess(
            config,
            fingerprint.screenClass(),
            fingerprint.slotId(),
            fingerprint.itemRawId(),
            fingerprint.itemCount(),
            fingerprint.componentHash(),
            mouseX,
            mouseY
        );

        runtime.metrics().tooltipCacheLookupPerformed();
        long cacheLookupStartNanos = SafeTime.nowNanos();
        CachedTooltipLayout cachedLayout = runtime.tooltipLayoutCache().get(fingerprint).orElse(null);
        runtime.metrics().tooltipCacheLookupTime(SafeTime.nowNanos() - cacheLookupStartNanos);
        if (cachedLayout != null) {
            runtime.tooltipPresentationService().remember(fingerprint, cachedLayout);
            return cachedLayout.lines();
        }

        if (shouldUseFallback(config, hoverAssessment)) {
            long fallbackStartNanos = SafeTime.nowNanos();
            runtime.metrics().tooltipFallbackEvaluation();
            TooltipTiming tooltipTiming = runtime.tooltipPresentationService().timing(nowNanos);
            CachedTooltipLayout fallbackLayout = runtime.tooltipPresentationService().fallback(fingerprint, tooltipTiming).orElse(null);
            long fallbackElapsedNanos = SafeTime.nowNanos() - fallbackStartNanos;
            runtime.metrics().tooltipFallbackEvaluationTime(fallbackElapsedNanos);
            if (fallbackLayout != null) {
                runtime.metrics().tooltipSuppressed();
                runtime.metrics().tooltipReused();
                runtime.metrics().tooltipFallbackReuseTime(fallbackElapsedNanos);
                return fallbackLayout.lines();
            }

            runtime.metrics().tooltipFallbackMiss();
            runtime.metrics().tooltipFallbackMissTime(fallbackElapsedNanos);
            if (tooltipTiming.guaranteedRenderWindowReached() || tooltipTiming.hardMaxDelayReached()) {
                runtime.metrics().tooltipForced();
                runtime.metrics().tooltipForcedRender();
                runtime.metrics().tooltipForcedRenderTime(fallbackElapsedNanos);
            }
        }

        List<Text> lines = List.copyOf(getTooltipFromItem(stack));
        CachedTooltipLayout computed = new CachedTooltipLayout(lines);
        runtime.tooltipLayoutCache().put(fingerprint, computed);
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
        Class<?> screenClass,
        int slotId,
        ItemStack stack,
        Item item,
        ComponentChanges componentChanges,
        boolean advancedTooltips,
        String language
    ) {
        long expensivePathStartNanos = SafeTime.nowNanos();
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
        runtime.metrics().tooltipFingerprintPathTime(SafeTime.nowNanos() - expensivePathStartNanos);
        return fingerprint;
    }
}
