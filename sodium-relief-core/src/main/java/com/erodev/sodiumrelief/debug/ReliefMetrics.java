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

    // Measurement-only (gated behind detailedMetricsEnabled): real cost of building a tooltip
    // on the miss path, and TextWidthCache effectiveness. Used to decide, with data, whether
    // the deferred structural optimizations (layout cache, TextWidthCache rework) are worth it.
    private long tooltipBuildNanos;
    private long tooltipBuildSamples;
    private long textWidthCacheHits;
    private long textWidthCacheMisses;
    private final long[] textWidthLengthBuckets = new long[5];

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

    public void tooltipBuild(long nanos) {
        if (detailedMetricsEnabled) {
            tooltipBuildNanos += Math.max(0L, nanos);
            tooltipBuildSamples++;
        }
    }

    public void textWidthCacheHit(int length) {
        if (detailedMetricsEnabled) {
            textWidthCacheHits++;
            recordTextWidthLength(length);
        }
    }

    public void textWidthCacheMiss(int length) {
        if (detailedMetricsEnabled) {
            textWidthCacheMisses++;
            recordTextWidthLength(length);
        }
    }

    private void recordTextWidthLength(int length) {
        int bucket = length <= 1 ? 0 : length == 2 ? 1 : length == 3 ? 2 : length <= 8 ? 3 : 4;
        textWidthLengthBuckets[bucket]++;
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
    public long tooltipBuildSamples() { return tooltipBuildSamples; }
    public long tooltipBuildAverageNanos() { return averageNanos(tooltipBuildNanos, tooltipBuildSamples); }

    /**
     * Estimated wall-clock nanos that layout reuse saved this run: the measured average cost
     * of one tooltip build multiplied by the number of builds avoided. Vanilla would build a
     * tooltip on every tooltip-path invocation; the mod actually built one only
     * {@code tooltipBuildSamples} times (the real {@code getTooltipFromItem} calls it timed —
     * note this is higher than {@code tooltipExpensivePathInvocations}, which only counts
     * fingerprint rebuilds, because the layout-cache TTL also forces real rebuilds). Only
     * meaningful while detailed metrics are enabled, since the build cost is sampled then.
     */
    public long estimatedTooltipNanosSaved() {
        long avoided = Math.max(0L, tooltipPathInvocations - tooltipBuildSamples);
        return tooltipBuildAverageNanos() * avoided;
    }

    public long textWidthCacheHits() { return textWidthCacheHits; }
    public long textWidthCacheMisses() { return textWidthCacheMisses; }

    /** Length-bucketed counts of strings measured through the text-width cache: 1, 2, 3, 4-8, 9+. */
    public long textWidthLengthBucket(int index) {
        return index >= 0 && index < textWidthLengthBuckets.length ? textWidthLengthBuckets[index] : 0L;
    }

    public String hoverState() { return currentHoverState; }

    private static long averageNanos(long totalNanos, long samples) {
        return samples <= 0L ? 0L : totalNanos / samples;
    }
}
