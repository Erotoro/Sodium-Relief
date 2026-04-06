package com.erodev.sodiumrelief.debug;

import com.erodev.sodiumrelief.config.RefreshMode;
import com.erodev.sodiumrelief.performance.BudgetState;
import com.erodev.sodiumrelief.scene.SceneDensityState;

public final class ReliefMetrics {
    private boolean detailedMetricsEnabled;
    private long tooltipHits;
    private long tooltipMisses;
    private long textHits;
    private long textMisses;
    private long invalidations;
    private long fullInvalidations;
    private long tooltipCacheInvalidations;
    private long tooltipContextResets;
    private long hoverSkips;
    private long hoverDeferrals;
    private long deferredTasksRun;
    private long deferredTasksQueued;
    private long deferredTasksSkipped;
    private long overBudgetFrames;
    private long spikeDetections;
    private long stabilizationEntries;
    private long taskReschedules;
    private long sceneWorkDeferrals;
    private long deferredTaskFailures;
    private long tooltipSuppressed;
    private long tooltipReused;
    private long tooltipForced;
    private long tooltipPathInvocations;
    private long tooltipFastPathHits;
    private long tooltipFastPathChecksPerformed;
    private long tooltipExpensivePathInvocations;
    private long tooltipCacheLookupsPerformed;
    private long tooltipFastPathCheckNanos;
    private long tooltipFingerprintPathNanos;
    private long tooltipCacheLookupNanos;
    private long tooltipFallbackEvaluations;
    private long tooltipFallbackMisses;
    private long tooltipForcedRenders;
    private long tooltipFallbackEvaluationNanos;
    private long tooltipFallbackReuseNanos;
    private long tooltipFallbackMissNanos;
    private long tooltipForcedRenderNanos;

    private volatile double averageFrametimeMs;
    private volatile double worstFrametimeMs;
    private volatile double currentFrametimeMs;
    private volatile long budgetPressureMicros;
    private volatile long targetBudgetMicros;
    private volatile long rawBudgetMicros;
    private volatile long effectiveBudgetMicros;
    private volatile long safetyMarginMicros;
    private volatile int stabilizationFramesRemaining;
    private volatile int targetRefreshRate;
    private volatile boolean budgetFloorActive;
    private volatile RefreshMode currentRefreshMode = RefreshMode.AUTO;
    private volatile BudgetState currentBudgetState = BudgetState.UNDER;
    private volatile String currentHoverState = "STABLE";
    private volatile SceneDensityState currentSceneDensityState = SceneDensityState.LOW;
    private volatile int tooltipTaskCostMicros;
    private volatile int textTaskCostMicros;
    private volatile int cacheTaskCostMicros;
    private volatile boolean autoRefreshFallback;
    private volatile boolean foliageHeavyScene;
    private volatile boolean transparencyHeavyScene;
    private volatile boolean cameraSweepActive;
    private volatile boolean denseSceneStabilizationActive;
    private volatile int denseSceneRecoveryFramesRemaining;
    private volatile boolean sceneBudgetClamped;

    public void tooltipHit() { tooltipHits++; }
    public void tooltipMiss() { tooltipMisses++; }
    public void textHit() { textHits++; }
    public void textMiss() { textMisses++; }
    public void fullInvalidation() {
        invalidations++;
        fullInvalidations++;
    }
    public void tooltipCacheInvalidation() {
        invalidations++;
        tooltipCacheInvalidations++;
    }
    public void tooltipContextReset() {
        invalidations++;
        tooltipContextResets++;
    }
    public void hoverSkip() { if (detailedMetricsEnabled) hoverSkips++; }
    public void hoverDeferred() { if (detailedMetricsEnabled) hoverDeferrals++; }
    public void deferredQueued() { if (detailedMetricsEnabled) deferredTasksQueued++; }
    public void deferredRun() { if (detailedMetricsEnabled) deferredTasksRun++; }
    public void deferredSkipped() { if (detailedMetricsEnabled) deferredTasksSkipped++; }
    public void overBudgetFrame() { overBudgetFrames++; }
    public void spikeDetected() { if (detailedMetricsEnabled) spikeDetections++; }
    public void stabilizationEntry() { if (detailedMetricsEnabled) stabilizationEntries++; }
    public void taskRescheduled() { if (detailedMetricsEnabled) taskReschedules++; }
    public void sceneWorkDeferred() { if (detailedMetricsEnabled) sceneWorkDeferrals++; }
    public void deferredFailure() { deferredTaskFailures++; }
    public void tooltipSuppressed() { if (detailedMetricsEnabled) tooltipSuppressed++; }
    public void tooltipReused() { tooltipReused++; }
    public void tooltipForced() { tooltipForced++; }
    public void tooltipPathInvocation() { tooltipPathInvocations++; }
    public void tooltipFastPathHit() { tooltipFastPathHits++; }
    public void tooltipFastPathCheckPerformed() { tooltipFastPathChecksPerformed++; }
    public void tooltipExpensivePathInvocation() { tooltipExpensivePathInvocations++; }
    public void tooltipCacheLookupPerformed() { tooltipCacheLookupsPerformed++; }
    public void tooltipFastPathCheckTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipFastPathCheckNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipFingerprintPathTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipFingerprintPathNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipCacheLookupTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipCacheLookupNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipFallbackEvaluation() { tooltipFallbackEvaluations++; }
    public void tooltipFallbackMiss() { tooltipFallbackMisses++; }
    public void tooltipForcedRender() { tooltipForcedRenders++; }
    public void tooltipFallbackEvaluationTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipFallbackEvaluationNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipFallbackReuseTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipFallbackReuseNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipFallbackMissTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipFallbackMissNanos += Math.max(0L, nanos);
        }
    }
    public void tooltipForcedRenderTime(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipForcedRenderNanos += Math.max(0L, nanos);
        }
    }

    public void frametimeSample(double currentMs, double averageMs, double worstMs) {
        if (!detailedMetricsEnabled) {
            return;
        }
        currentFrametimeMs = currentMs;
        averageFrametimeMs = averageMs;
        worstFrametimeMs = worstMs;
    }

    public void budgetState(long targetMicros, long pressureMicros) {
        targetBudgetMicros = targetMicros;
        budgetPressureMicros = pressureMicros;
    }

    public void stabilizationState(int framesRemaining) {
        if (!detailedMetricsEnabled) {
            return;
        }
        stabilizationFramesRemaining = framesRemaining;
    }

    public void refreshState(RefreshMode mode, int refreshRate, boolean fallbackActive) {
        currentRefreshMode = mode;
        targetRefreshRate = refreshRate;
        autoRefreshFallback = fallbackActive;
    }

    public void budgetDetails(long rawBudgetMicros, long effectiveBudgetMicros, long safetyMarginMicros, boolean floorActive, BudgetState state) {
        if (!detailedMetricsEnabled) {
            return;
        }
        this.effectiveBudgetMicros = effectiveBudgetMicros;
        this.safetyMarginMicros = safetyMarginMicros;
        budgetFloorActive = floorActive;
        currentBudgetState = state;
        this.rawBudgetMicros = rawBudgetMicros;
    }

    public void hoverState(String state) {
        if (!detailedMetricsEnabled) {
            return;
        }
        currentHoverState = state;
    }

    public void taskCostState(int tooltipMicros, int textMicros, int cacheMicros) {
        if (!detailedMetricsEnabled) {
            return;
        }
        tooltipTaskCostMicros = tooltipMicros;
        textTaskCostMicros = textMicros;
        cacheTaskCostMicros = cacheMicros;
    }

    public void sceneState(
        SceneDensityState densityState,
        boolean foliageHeavy,
        boolean transparencyHeavy,
        boolean cameraSweep,
        boolean sceneStabilization,
        int recoveryFrames,
        boolean budgetClamped
    ) {
        if (!detailedMetricsEnabled) {
            return;
        }
        currentSceneDensityState = densityState;
        foliageHeavyScene = foliageHeavy;
        transparencyHeavyScene = transparencyHeavy;
        cameraSweepActive = cameraSweep;
        denseSceneStabilizationActive = sceneStabilization;
        denseSceneRecoveryFramesRemaining = recoveryFrames;
        sceneBudgetClamped = budgetClamped;
    }

    public void sceneBudgetClamped(boolean clamped) {
        if (!detailedMetricsEnabled) {
            return;
        }
        sceneBudgetClamped = clamped;
    }

    public void applyDebugMode(boolean enabled) {
        detailedMetricsEnabled = enabled;
    }

    public long tooltipHits() { return tooltipHits; }
    public long tooltipMisses() { return tooltipMisses; }
    public long textHits() { return textHits; }
    public long textMisses() { return textMisses; }
    public long invalidations() { return invalidations; }
    public long fullInvalidations() { return fullInvalidations; }
    public long tooltipCacheInvalidations() { return tooltipCacheInvalidations; }
    public long tooltipContextResets() { return tooltipContextResets; }
    public long hoverSkips() { return hoverSkips; }
    public long hoverDeferrals() { return hoverDeferrals; }
    public long deferredTasksRun() { return deferredTasksRun; }
    public long deferredTasksQueued() { return deferredTasksQueued; }
    public long deferredTasksSkipped() { return deferredTasksSkipped; }
    public long overBudgetFrames() { return overBudgetFrames; }
    public long spikeDetections() { return spikeDetections; }
    public long stabilizationEntries() { return stabilizationEntries; }
    public long taskReschedules() { return taskReschedules; }
    public long sceneWorkDeferrals() { return sceneWorkDeferrals; }
    public long deferredTaskFailures() { return deferredTaskFailures; }
    public long tooltipSuppressedCount() { return tooltipSuppressed; }
    public long tooltipReusedCount() { return tooltipReused; }
    public long tooltipForcedCount() { return tooltipForced; }
    public long tooltipPathInvocations() { return tooltipPathInvocations; }
    public long tooltipFastPathHits() { return tooltipFastPathHits; }
    public long tooltipFastPathChecksPerformed() { return tooltipFastPathChecksPerformed; }
    public long tooltipExpensivePathInvocations() { return tooltipExpensivePathInvocations; }
    public long tooltipCacheLookupsPerformed() { return tooltipCacheLookupsPerformed; }
    public long tooltipFastPathCheckNanos() { return tooltipFastPathCheckNanos; }
    public long tooltipFingerprintPathNanos() { return tooltipFingerprintPathNanos; }
    public long tooltipCacheLookupNanos() { return tooltipCacheLookupNanos; }
    public long tooltipFallbackEvaluations() { return tooltipFallbackEvaluations; }
    public long tooltipFallbackMisses() { return tooltipFallbackMisses; }
    public long tooltipForcedRenders() { return tooltipForcedRenders; }
    public long tooltipFallbackEvaluationNanos() { return tooltipFallbackEvaluationNanos; }
    public long tooltipFallbackReuseNanos() { return tooltipFallbackReuseNanos; }
    public long tooltipFallbackMissNanos() { return tooltipFallbackMissNanos; }
    public long tooltipForcedRenderNanos() { return tooltipForcedRenderNanos; }
    public long tooltipFastPathCheckAverageNanos() { return averageNanos(tooltipFastPathCheckNanos, tooltipFastPathChecksPerformed); }
    public long tooltipFingerprintPathAverageNanos() { return averageNanos(tooltipFingerprintPathNanos, tooltipExpensivePathInvocations); }
    public long tooltipCacheLookupAverageNanos() { return averageNanos(tooltipCacheLookupNanos, tooltipCacheLookupsPerformed); }
    public long tooltipFallbackEvaluationAverageNanos() { return averageNanos(tooltipFallbackEvaluationNanos, tooltipFallbackEvaluations); }
    public long tooltipFallbackReuseAverageNanos() { return averageNanos(tooltipFallbackReuseNanos, tooltipReused); }
    public long tooltipFallbackMissAverageNanos() { return averageNanos(tooltipFallbackMissNanos, tooltipFallbackMisses); }
    public long tooltipForcedRenderAverageNanos() { return averageNanos(tooltipForcedRenderNanos, tooltipForcedRenders); }
    public double currentFrametimeMs() { return currentFrametimeMs; }
    public double averageFrametimeMs() { return averageFrametimeMs; }
    public double worstFrametimeMs() { return worstFrametimeMs; }
    public long budgetPressureMicros() { return budgetPressureMicros; }
    public long targetBudgetMicros() { return targetBudgetMicros; }
    public int stabilizationFramesRemaining() { return stabilizationFramesRemaining; }
    public long rawBudgetMicros() { return rawBudgetMicros; }
    public long effectiveBudgetMicros() { return effectiveBudgetMicros; }
    public long safetyMarginMicros() { return safetyMarginMicros; }
    public int targetRefreshRate() { return targetRefreshRate; }
    public boolean budgetFloorActive() { return budgetFloorActive; }
    public RefreshMode refreshMode() { return currentRefreshMode; }
    public BudgetState budgetState() { return currentBudgetState; }
    public String hoverState() { return currentHoverState; }
    public SceneDensityState sceneDensityState() { return currentSceneDensityState; }
    public int tooltipTaskCostMicros() { return tooltipTaskCostMicros; }
    public int textTaskCostMicros() { return textTaskCostMicros; }
    public int cacheTaskCostMicros() { return cacheTaskCostMicros; }
    public boolean autoRefreshFallback() { return autoRefreshFallback; }
    public boolean foliageHeavyScene() { return foliageHeavyScene; }
    public boolean transparencyHeavyScene() { return transparencyHeavyScene; }
    public boolean cameraSweepActive() { return cameraSweepActive; }
    public boolean denseSceneStabilizationActive() { return denseSceneStabilizationActive; }
    public int denseSceneRecoveryFramesRemaining() { return denseSceneRecoveryFramesRemaining; }
    public boolean sceneBudgetClamped() { return sceneBudgetClamped; }

    private static long averageNanos(long totalNanos, long samples) {
        return samples <= 0L ? 0L : totalNanos / samples;
    }
}
