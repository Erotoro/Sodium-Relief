package com.erodev.sodiumrelief.client;

import com.erodev.sodiumrelief.cache.CacheInvalidationManager;
import com.erodev.sodiumrelief.cache.TextWidthCache;
import com.erodev.sodiumrelief.cache.TooltipLayoutCache;
import com.erodev.sodiumrelief.debug.BenchmarkSnapshot;
import com.erodev.sodiumrelief.debug.BenchmarkSnapshotWriter;
import com.erodev.sodiumrelief.compat.sodium.SodiumCompat;
import com.erodev.sodiumrelief.config.ReliefConfigManager;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.hover.HoverSmoothingService;
import com.erodev.sodiumrelief.hover.HoverTracker;
import com.erodev.sodiumrelief.tooltip.TooltipPresentationService;
import com.erodev.sodiumrelief.ui.ScreenStateTracker;
import com.erodev.sodiumrelief.ui.UiOptimizationService;
import java.io.IOException;
import java.time.Instant;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SodiumReliefRuntime {
    private final ReliefConfigManager configManager = new ReliefConfigManager();
    private final ReliefMetrics metrics = new ReliefMetrics();
    private final HoverTracker hoverTracker = new HoverTracker();
    private final HoverSmoothingService hoverSmoothingService = new HoverSmoothingService(metrics, hoverTracker);
    private final TooltipPresentationService tooltipPresentationService = new TooltipPresentationService();
    private final TooltipLayoutCache tooltipLayoutCache = new TooltipLayoutCache(metrics);
    private final TextWidthCache textWidthCache = new TextWidthCache(4096);
    private final CacheInvalidationManager cacheInvalidationManager = new CacheInvalidationManager(tooltipLayoutCache, hoverTracker, tooltipPresentationService, metrics);
    private final UiOptimizationService uiOptimizationService = new UiOptimizationService();
    private final ScreenStateTracker screenStateTracker = new ScreenStateTracker();
    private final SodiumCompat sodiumCompat = new SodiumCompat();

    public void initialize() {
        configManager.load();
        reapplyConfig();
        sodiumCompat.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new CacheResetReloader());
    }

    public void reapplyConfig() {
        configManager.normalizeInMemory();
        metrics.applyDebugMode(configManager.config().debugOverlay || configManager.config().debugLogging);
        tooltipLayoutCache.applyConfig(configManager.config());
        textWidthCache.applyConfig(
            configManager.config().enableMod && configManager.config().enableTextWidthCache,
            configManager.config().textWidthCacheSize
        );
        ReliefLogger.debug(configManager.config().debugLogging, "Applied Sodium Relief configuration");
    }

    public void saveAndReapplyConfig() {
        configManager.normalizeInMemory();
        configManager.save();
        reapplyConfig();
    }

    private void onEndTick(Minecraft client) {
        if (client.options == null) {
            return;
        }

        sodiumCompat.tick();

        String language = client.options.languageCode;
        if (!language.equals(screenStateTracker.lastLanguage())) {
            screenStateTracker.lastLanguage(language);
            cacheInvalidationManager.invalidateTooltipCache();
        }

        String screenClass = client.screen == null ? "" : client.screen.getClass().getName();
        if (!screenClass.equals(screenStateTracker.lastScreenClass())) {
            screenStateTracker.lastScreenClass(screenClass);
            cacheInvalidationManager.resetTooltipContext();
        }
    }

    public ReliefConfigManager configManager() { return configManager; }
    public ReliefMetrics metrics() { return metrics; }
    public HoverSmoothingService hoverSmoothingService() { return hoverSmoothingService; }
    public TooltipLayoutCache tooltipLayoutCache() { return tooltipLayoutCache; }
    public TextWidthCache textWidthCache() { return textWidthCache; }
    public TooltipPresentationService tooltipPresentationService() { return tooltipPresentationService; }
    public CacheInvalidationManager cacheInvalidationManager() { return cacheInvalidationManager; }
    public UiOptimizationService uiOptimizationService() { return uiOptimizationService; }
    public SodiumCompat sodiumCompat() { return sodiumCompat; }

    public void exportBenchmarkSnapshot(String label) {
        Minecraft client = Minecraft.getInstance();
        String screenId = client.screen == null ? "none" : client.screen.getClass().getName();
        BenchmarkSnapshot snapshot = BenchmarkSnapshot.capture(
            label,
            screenId,
            metrics,
            tooltipLayoutCache.size(),
            Instant.now()
        );
        try {
            ReliefLogger.info("Exporting benchmark snapshot: " + BenchmarkSnapshotWriter.write(configManager.benchmarkDirectory(), snapshot));
        } catch (IOException exception) {
            ReliefLogger.warn("Failed to export benchmark snapshot", exception);
        }
    }

    private final class CacheResetReloader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return Identifier.fromNamespaceAndPath(SodiumReliefClient.MOD_ID, "cache_reset");
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            cacheInvalidationManager.invalidateAll();
            textWidthCache.clear();
        }
    }
}
