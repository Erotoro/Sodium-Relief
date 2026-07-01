package com.erodev.sodiumrelief.config;

public final class ReliefConfig {
    public boolean enableMod = true;
    public boolean debugLogging = false;
    public boolean debugOverlay = false;

    public AdaptiveMode adaptiveMode = AdaptiveMode.BALANCED;

    public boolean enableTooltipLayoutCache = true;
    public int tooltipCacheSize = 256;
    // The fingerprint (item + components patch + count + advanced + language) is exhaustive for
    // a vanilla tooltip, so a cached layout can only be stale for tooltips that vary on state the
    // fingerprint cannot see (e.g. a mod drawing a live timer). The TTL is the safety valve for
    // exactly that rare case. In-game measurement showed a short TTL forced a real ~260 us rebuild
    // every 1.5 s on a sustained hover (a periodic micro-stutter) and broke reuse when comparing
    // items back and forth, so the default is generous; lower it if you rely on animated tooltips.
    public long tooltipCacheTtlMs = 30_000L;
    public boolean strictTooltipInvalidation = true;

    public boolean enableHoverSmoothing = true;
    public boolean skipRedundantHoverRecomputations = true;
    public boolean enableHoverPrediction = true;
    public int hoverPredictionDelayMillis = 28;
    /** Legacy key from configs written before the millisecond migration. */
    @Deprecated
    public Integer hoverPredictionDelayMicros;
    public ReliefMode hoverMode = ReliefMode.BALANCED;

    public boolean enableLazyEvaluation = true;
    public boolean enableUiOptimization = true;
    public boolean optimizeInventoryScreens = true;

    public boolean enableTextWidthCache = true;
    public int textWidthCacheSize = 4096;

    public boolean extraSafetyChecks = true;

    public void normalize() {
        if (hoverPredictionDelayMicros != null) {
            hoverPredictionDelayMillis = Math.max(1, Math.round(hoverPredictionDelayMicros / 1_000.0F));
            hoverPredictionDelayMicros = null;
        }
        tooltipCacheSize = Math.max(1, tooltipCacheSize);
        tooltipCacheTtlMs = Math.max(1L, tooltipCacheTtlMs);
        hoverPredictionDelayMillis = Math.max(1, hoverPredictionDelayMillis);
        textWidthCacheSize = Math.max(1, textWidthCacheSize);
    }
}
