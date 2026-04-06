package com.erodev.sodiumrelief.debug;

import com.erodev.sodiumrelief.cache.TooltipLayoutCache;
import com.erodev.sodiumrelief.config.ReliefConfigManager;
import com.erodev.sodiumrelief.util.ReliefTexts;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ReliefDebugOverlay {
    private final ReliefConfigManager configManager;
    private final ReliefMetrics metrics;
    private final TooltipLayoutCache tooltipLayoutCache;

    public ReliefDebugOverlay(
        ReliefConfigManager configManager,
        ReliefMetrics metrics,
        TooltipLayoutCache tooltipLayoutCache
    ) {
        this.configManager = configManager;
        this.metrics = metrics;
        this.tooltipLayoutCache = tooltipLayoutCache;
    }

    public void initialize() {
        attachHudElement("debug_overlay", this::render);
    }

    public void attachHudElement(String path, HudElement element) {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Objects.requireNonNull(Identifier.of("sodiumrelief", path)),
            Objects.requireNonNull(element, "element")
        );
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!configManager.config().enableMod || !configManager.config().debugOverlay) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        int y = 8;
        y = drawLine(context, client, y, ReliefTexts.modName());
        y = drawLine(context, client, y, ReliefTexts.overlay("tooltip_hit_miss", metrics.tooltipHits(), metrics.tooltipMisses()));
        y = drawLine(context, client, y, ReliefTexts.overlay(
            "tooltip_path_calls_fast_expensive",
            metrics.tooltipPathInvocations(),
            metrics.tooltipFastPathChecksPerformed(),
            metrics.tooltipFastPathHits(),
            metrics.tooltipExpensivePathInvocations(),
            metrics.tooltipCacheLookupsPerformed()
        ));
        y = drawLine(context, client, y, ReliefTexts.overlay(
            "tooltip_fallback_reuse_forced",
            metrics.tooltipFallbackEvaluations(),
            metrics.tooltipReusedCount(),
            metrics.tooltipFallbackMisses(),
            metrics.tooltipForcedCount()
        ));
        y = drawLine(context, client, y, ReliefTexts.overlay(
            "tooltip_path_timing_core",
            metrics.tooltipFastPathCheckAverageNanos(),
            metrics.tooltipFingerprintPathAverageNanos(),
            metrics.tooltipCacheLookupAverageNanos()
        ));
        y = drawLine(context, client, y, ReliefTexts.overlay(
            "tooltip_path_timing_fallback",
            metrics.tooltipFallbackEvaluationAverageNanos(),
            metrics.tooltipFallbackReuseAverageNanos(),
            metrics.tooltipFallbackMissAverageNanos(),
            metrics.tooltipForcedRenderAverageNanos()
        ));
        y = drawLine(context, client, y, ReliefTexts.overlay(
            "invalidations",
            metrics.invalidations(),
            metrics.fullInvalidations(),
            metrics.tooltipCacheInvalidations(),
            metrics.tooltipContextResets()
        ));
        y = drawLine(context, client, y, ReliefTexts.overlay("hover_skip_defer_state", metrics.hoverSkips(), metrics.hoverDeferrals(), ReliefTexts.hoverState(metrics.hoverState())));
        drawLine(context, client, y, ReliefTexts.overlay("tooltip_cache", tooltipLayoutCache.size()));
    }

    private static int drawLine(DrawContext context, MinecraftClient client, int y, Text line) {
        context.drawText(client.textRenderer, line, 8, y, 0xE0E0E0, true);
        return y + 10;
    }
}
