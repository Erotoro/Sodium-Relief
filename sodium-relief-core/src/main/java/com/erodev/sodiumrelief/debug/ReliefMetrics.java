package com.erodev.sodiumrelief.debug;

/**
 * Lightweight counters for the tooltip/hover path. Plain counters are always
 * incremented (a single {@code long++} is negligible); the more expensive
 * timing accumulators are gated behind {@link #detailedMetricsEnabled()} so the
 * hot path pays nothing for measurement when the debug overlay/logging is off.
 */
public final class ReliefMetrics {
    private boolean detailedMetricsEnabled;

    private long tooltipHits;
    private long tooltipMisses;
    private long invalidations;
    private long fullInvalidations;
    private long tooltipCacheInvalidations;
    private long tooltipContextResets;
    private long hoverSkips;
    private long hoverDeferrals;
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

    private volatile String currentHoverState = "STABLE";

    public boolean detailedMetricsEnabled() { return detailedMetricsEnabled; }

    public void tooltipHit() { tooltipHits++; }
    public void tooltipMiss() { tooltipMisses++; }
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

    public void hoverState(String state) {
        if (!detailedMetricsEnabled) {
            return;
        }
        currentHoverState = state;
    }

    public void applyDebugMode(boolean enabled) {
        detailedMetricsEnabled = enabled;
    }

    public long tooltipHits() { return tooltipHits; }
    public long tooltipMisses() { return tooltipMisses; }
    public long invalidations() { return invalidations; }
    public long fullInvalidations() { return fullInvalidations; }
    public long tooltipCacheInvalidations() { return tooltipCacheInvalidations; }
    public long tooltipContextResets() { return tooltipContextResets; }
    public long hoverSkips() { return hoverSkips; }
    public long hoverDeferrals() { return hoverDeferrals; }
    public long tooltipSuppressedCount() { return tooltipSuppressed; }
    public long tooltipReusedCount() { return tooltipReused; }
    public long tooltipForcedCount() { return tooltipForced; }
    public long tooltipPathInvocations() { return tooltipPathInvocations; }
    public long tooltipFastPathHits() { return tooltipFastPathHits; }
    public long tooltipFastPathChecksPerformed() { return tooltipFastPathChecksPerformed; }
    public long tooltipExpensivePathInvocations() { return tooltipExpensivePathInvocations; }
    public long tooltipCacheLookupsPerformed() { return tooltipCacheLookupsPerformed; }
    public long tooltipFallbackEvaluations() { return tooltipFallbackEvaluations; }
    public long tooltipFallbackMisses() { return tooltipFallbackMisses; }
    public long tooltipForcedRenders() { return tooltipForcedRenders; }
    public long tooltipFastPathCheckAverageNanos() { return averageNanos(tooltipFastPathCheckNanos, tooltipFastPathChecksPerformed); }
    public long tooltipFingerprintPathAverageNanos() { return averageNanos(tooltipFingerprintPathNanos, tooltipExpensivePathInvocations); }
    public long tooltipCacheLookupAverageNanos() { return averageNanos(tooltipCacheLookupNanos, tooltipCacheLookupsPerformed); }
    public long tooltipFallbackEvaluationAverageNanos() { return averageNanos(tooltipFallbackEvaluationNanos, tooltipFallbackEvaluations); }
    public long tooltipFallbackReuseAverageNanos() { return averageNanos(tooltipFallbackReuseNanos, tooltipReused); }
    public long tooltipFallbackMissAverageNanos() { return averageNanos(tooltipFallbackMissNanos, tooltipFallbackMisses); }
    public long tooltipForcedRenderAverageNanos() { return averageNanos(tooltipForcedRenderNanos, tooltipForcedRenders); }
    public String hoverState() { return currentHoverState; }

    private static long averageNanos(long totalNanos, long samples) {
        return samples <= 0L ? 0L : totalNanos / samples;
    }
}
