package com.erodev.sodiumrelief.cache;

import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.hover.HoverTracker;
import com.erodev.sodiumrelief.tooltip.TooltipPresentationService;

public final class CacheInvalidationManager {
    private final TooltipLayoutCache tooltipLayoutCache;
    private final HoverTracker hoverTracker;
    private final TooltipPresentationService tooltipPresentationService;
    private final ReliefMetrics metrics;

    public CacheInvalidationManager(
        TooltipLayoutCache tooltipLayoutCache,
        HoverTracker hoverTracker,
        TooltipPresentationService tooltipPresentationService,
        ReliefMetrics metrics
    ) {
        this.tooltipLayoutCache = tooltipLayoutCache;
        this.hoverTracker = hoverTracker;
        this.tooltipPresentationService = tooltipPresentationService;
        this.metrics = metrics;
    }

    public void invalidateAll() {
        metrics.fullInvalidation();
        tooltipLayoutCache.clear();
        hoverTracker.reset();
        tooltipPresentationService.clear();
    }

    public void invalidateTooltipCache() {
        metrics.tooltipCacheInvalidation();
        tooltipLayoutCache.clear();
        hoverTracker.reset();
        tooltipPresentationService.clear();
    }

    public void resetTooltipContext() {
        metrics.tooltipContextReset();
        hoverTracker.reset();
        tooltipPresentationService.clear();
    }
}
