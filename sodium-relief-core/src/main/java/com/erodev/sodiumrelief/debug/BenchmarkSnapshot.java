package com.erodev.sodiumrelief.debug;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Immutable benchmark export payload for comparing tooltip-path behavior between
 * baseline and optimized runs.
 */
public record BenchmarkSnapshot(
    int schemaVersion,
    String exportedAt,
    String label,
    String screenId,
    long tooltipHits,
    long tooltipMisses,
    long tooltipPathInvocations,
    long tooltipFastPathChecksPerformed,
    long tooltipFastPathHits,
    long tooltipExpensivePathInvocations,
    long tooltipCacheLookupsPerformed,
    long tooltipFallbackEvaluations,
    long tooltipReusedCount,
    long tooltipFallbackMisses,
    long tooltipForcedCount,
    long invalidations,
    long fullInvalidations,
    long tooltipCacheInvalidations,
    long tooltipContextResets,
    String hoverState,
    int tooltipCacheEntries,
    long tooltipBuildSamples,
    long tooltipBuildAverageNanos,
    long estimatedTooltipNanosSaved,
    long textWidthCacheHits,
    long textWidthCacheMisses,
    long textWidthLen1,
    long textWidthLen2,
    long textWidthLen3,
    long textWidthLen4to8,
    long textWidthLen9plus
) {
    private static final int SCHEMA_VERSION = 2;

    public static BenchmarkSnapshot capture(
        String label,
        String screenId,
        ReliefMetrics metrics,
        int tooltipCacheEntries,
        Instant exportedAt
    ) {
        return new BenchmarkSnapshot(
            SCHEMA_VERSION,
            DateTimeFormatter.ISO_INSTANT.format(exportedAt),
            label,
            screenId,
            metrics.tooltipHits(),
            metrics.tooltipMisses(),
            metrics.tooltipPathInvocations(),
            metrics.tooltipFastPathChecksPerformed(),
            metrics.tooltipFastPathHits(),
            metrics.tooltipExpensivePathInvocations(),
            metrics.tooltipCacheLookupsPerformed(),
            metrics.tooltipFallbackEvaluations(),
            metrics.tooltipReusedCount(),
            metrics.tooltipFallbackMisses(),
            metrics.tooltipForcedCount(),
            metrics.invalidations(),
            metrics.fullInvalidations(),
            metrics.tooltipCacheInvalidations(),
            metrics.tooltipContextResets(),
            metrics.hoverState(),
            tooltipCacheEntries,
            metrics.tooltipBuildSamples(),
            metrics.tooltipBuildAverageNanos(),
            metrics.estimatedTooltipNanosSaved(),
            metrics.textWidthCacheHits(),
            metrics.textWidthCacheMisses(),
            metrics.textWidthLengthBucket(0),
            metrics.textWidthLengthBucket(1),
            metrics.textWidthLengthBucket(2),
            metrics.textWidthLengthBucket(3),
            metrics.textWidthLengthBucket(4)
        );
    }
}
