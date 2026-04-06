package com.erodev.sodiumrelief.config;

public final class ReliefConfig {
    public boolean enableMod = true;
    public boolean debugLogging = false;
    public boolean debugOverlay = false;

    public transient RefreshMode refreshMode = RefreshMode.AUTO;
    public transient RefreshPreset refreshPreset = RefreshPreset.HZ_144;
    public transient int customRefreshRate = 144;
    public transient int minimumRefreshRate = 30;
    public transient int maximumRefreshRate = 500;
    public transient double safetyMarginRatio = 0.18D;
    @Deprecated
    public Double budgetSafetyRatio;
    public transient boolean enableBudgetFloor = true;
    public transient int minimumEffectiveBudgetMicros = 4_200;

    public AdaptiveMode adaptiveMode = AdaptiveMode.BALANCED;

    public boolean enableTooltipLayoutCache = true;
    public int tooltipCacheSize = 256;
    public long tooltipCacheTtlMs = 1_500L;
    public boolean strictTooltipInvalidation = true;

    public boolean enableHoverSmoothing = true;
    public boolean skipRedundantHoverRecomputations = true;
    public boolean enableHoverPrediction = true;
    public int hoverPredictionDelayMillis = 28;
    @Deprecated
    public Integer hoverPredictionDelayMicros;
    public ReliefMode hoverMode = ReliefMode.BALANCED;

    public transient boolean enableTextMeasurementCache = true;
    public transient int textCacheSize = 512;
    public transient boolean resetTextCacheOnLanguageReload = true;

    public boolean enableLazyEvaluation = true;
    public transient boolean enableUiOptimization = true;
    public boolean optimizeInventoryScreens = true;
    public transient boolean enableSmoothShaderTransition = true;
    public transient boolean enableShaderStatusOverlay = true;
    public transient int shaderTransitionFadeMillis = 120;
    public transient boolean enableSmoothResourcePackTransition = true;
    public transient boolean enableResourcePackStatusOverlay = true;
    public transient int resourcePackTransitionFadeMillis = 100;
    public transient boolean optimizeKnownVanillaScreens = true;
    public transient boolean optimizeOptionLikeScreens = true;
    public transient boolean optimizeUnknownModdedScreens = false;
    @Deprecated
    public transient boolean optimizeOptionScreens = true;
    @Deprecated
    public transient boolean conservativeCompatibilityMode = true;
    @Deprecated
    public transient boolean disableOnUnknownScreens = true;

    public transient boolean enableFrametimeStabilizer = true;
    public transient boolean enableFrameBudgetManager = true;
    public transient boolean enableTaskScheduler = true;
    public transient boolean enableWorkDistributionSystem = true;
    public transient boolean enableMicroStutterDetector = true;
    public transient boolean enableFramePacingStabilizer = true;
    public transient int targetFrameBudgetMicros = 16_667;
    public transient int nonCriticalWorkBudgetMicros = 1_500;
    public transient int maxDeferredTasksPerTick = 4;
    public transient int spikeThresholdMicros = 5_000;
    public transient int stabilizationFrames = 6;

    public transient boolean enableDenseSceneRelief = true;
    public transient boolean enableDenseSceneDetector = true;
    public transient boolean enableCameraMotionRelief = true;
    public transient boolean enableFoliageSceneStabilizer = true;
    public transient boolean enableTransparencySceneRelief = true;
    public transient int denseSceneScanRadius = 6;
    public transient int denseSceneScanIntervalTicks = 3;
    public transient int cameraSweepThresholdDegrees = 16;
    public transient int sceneStabilizationFrames = 4;

    public boolean extraSafetyChecks = true;

    public void normalize() {
        if (budgetSafetyRatio != null) {
            double legacyValue = budgetSafetyRatio;
            safetyMarginRatio = legacyValue > 0.49D ? 1.0D - legacyValue : legacyValue;
            budgetSafetyRatio = null;
        }
        if (hoverPredictionDelayMicros != null) {
            hoverPredictionDelayMillis = Math.max(1, Math.round(hoverPredictionDelayMicros / 1_000.0F));
            hoverPredictionDelayMicros = null;
        }
        minimumRefreshRate = Math.max(1, minimumRefreshRate);
        maximumRefreshRate = Math.max(minimumRefreshRate, maximumRefreshRate);
        customRefreshRate = clamp(customRefreshRate, minimumRefreshRate, maximumRefreshRate);
        safetyMarginRatio = Math.max(0.05D, Math.min(0.40D, safetyMarginRatio));
        minimumEffectiveBudgetMicros = Math.max(2_000, minimumEffectiveBudgetMicros);
        tooltipCacheSize = Math.max(1, tooltipCacheSize);
        tooltipCacheTtlMs = Math.max(1L, tooltipCacheTtlMs);
        hoverPredictionDelayMillis = Math.max(1, hoverPredictionDelayMillis);
        shaderTransitionFadeMillis = clamp(shaderTransitionFadeMillis, 40, 300);
        resourcePackTransitionFadeMillis = clamp(resourcePackTransitionFadeMillis, 40, 240);
        textCacheSize = Math.max(1, textCacheSize);
        targetFrameBudgetMicros = Math.max(1, targetFrameBudgetMicros);
        nonCriticalWorkBudgetMicros = Math.max(0, nonCriticalWorkBudgetMicros);
        maxDeferredTasksPerTick = Math.max(1, maxDeferredTasksPerTick);
        spikeThresholdMicros = Math.max(1, spikeThresholdMicros);
        stabilizationFrames = Math.max(1, stabilizationFrames);
        denseSceneScanRadius = Math.max(1, denseSceneScanRadius);
        denseSceneScanIntervalTicks = Math.max(1, denseSceneScanIntervalTicks);
        cameraSweepThresholdDegrees = Math.max(1, cameraSweepThresholdDegrees);
        sceneStabilizationFrames = Math.max(1, sceneStabilizationFrames);
    }

    public int hoverPredictionDelayMicros() {
        return Math.max(1, hoverPredictionDelayMillis) * 1_000;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
