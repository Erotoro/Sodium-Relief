package com.erodev.sodiumrelief.client;

import com.erodev.sodiumrelief.cache.CacheInvalidationManager;
import com.erodev.sodiumrelief.cache.TooltipLayoutCache;
import com.erodev.sodiumrelief.compat.sodium.SodiumCompat;
import com.erodev.sodiumrelief.config.ReliefConfigManager;
import com.erodev.sodiumrelief.debug.ReliefDebugOverlay;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.hover.HoverSmoothingService;
import com.erodev.sodiumrelief.hover.HoverTracker;
import com.erodev.sodiumrelief.tooltip.TooltipPresentationService;
import com.erodev.sodiumrelief.ui.ScreenStateTracker;
import com.erodev.sodiumrelief.ui.UiOptimizationService;
import java.util.Objects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class SodiumReliefRuntime {
    private final ReliefConfigManager configManager = new ReliefConfigManager();
    private final ReliefMetrics metrics = new ReliefMetrics();
    private final HoverTracker hoverTracker = new HoverTracker();
    private final HoverSmoothingService hoverSmoothingService = new HoverSmoothingService(metrics, hoverTracker);
    private final TooltipPresentationService tooltipPresentationService = new TooltipPresentationService();
    private final TooltipLayoutCache tooltipLayoutCache = new TooltipLayoutCache(metrics);
    private final CacheInvalidationManager cacheInvalidationManager = new CacheInvalidationManager(tooltipLayoutCache, hoverTracker, tooltipPresentationService, metrics);
    private final UiOptimizationService uiOptimizationService = new UiOptimizationService();
    private final ScreenStateTracker screenStateTracker = new ScreenStateTracker();
    private final SodiumCompat sodiumCompat = new SodiumCompat();
    private final ReliefDebugOverlay debugOverlay = new ReliefDebugOverlay(configManager, metrics, tooltipLayoutCache);

    public void initialize() {
        configManager.load();
        reapplyConfig();
        debugOverlay.initialize();
        sodiumCompat.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(
            Objects.requireNonNull(Identifier.of(SodiumReliefClient.MOD_ID, "cache_reset")),
            new CacheResetReloader()
        );
    }

    public void reapplyConfig() {
        configManager.normalizeInMemory();
        metrics.applyDebugMode(configManager.config().debugOverlay || configManager.config().debugLogging);
        tooltipLayoutCache.applyConfig(configManager.config());
        ReliefLogger.debug(configManager.config().debugLogging, "Applied Sodium Relief configuration");
    }

    public void saveAndReapplyConfig() {
        configManager.normalizeInMemory();
        configManager.save();
        reapplyConfig();
    }

    private void onEndTick(MinecraftClient client) {
        if (client.options == null) {
            return;
        }

        sodiumCompat.tick();

        String language = client.options.language;
        if (!language.equals(screenStateTracker.lastLanguage())) {
            screenStateTracker.lastLanguage(language);
            cacheInvalidationManager.invalidateTooltipCache();
        }

        String screenClass = client.currentScreen == null ? "" : client.currentScreen.getClass().getName();
        if (!screenClass.equals(screenStateTracker.lastScreenClass())) {
            screenStateTracker.lastScreenClass(screenClass);
            cacheInvalidationManager.resetTooltipContext();
        }
    }

    public ReliefConfigManager configManager() { return configManager; }
    public ReliefMetrics metrics() { return metrics; }
    public HoverSmoothingService hoverSmoothingService() { return hoverSmoothingService; }
    public TooltipLayoutCache tooltipLayoutCache() { return tooltipLayoutCache; }
    public TooltipPresentationService tooltipPresentationService() { return tooltipPresentationService; }
    public CacheInvalidationManager cacheInvalidationManager() { return cacheInvalidationManager; }
    public UiOptimizationService uiOptimizationService() { return uiOptimizationService; }
    public SodiumCompat sodiumCompat() { return sodiumCompat; }

    private final class CacheResetReloader implements SynchronousResourceReloader {
        @Override
        public void reload(ResourceManager manager) {
            cacheInvalidationManager.invalidateAll();
        }
    }
}
