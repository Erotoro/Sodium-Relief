package com.erodev.sodiumrelief.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkSnapshotWriterTest {
    private static final Gson GSON = new Gson();

    @TempDir
    Path tempDir;

    @Test
    void snapshotCapturesCurrentMetricsValues() {
        ReliefMetrics metrics = new ReliefMetrics();
        metrics.applyDebugMode(true);
        metrics.tooltipHit();
        metrics.tooltipMiss();
        metrics.tooltipPathInvocation();
        metrics.tooltipFastPathCheckPerformed();
        metrics.tooltipFastPathHit();
        metrics.tooltipExpensivePathInvocation();
        metrics.tooltipCacheLookupPerformed();
        metrics.tooltipFallbackEvaluation();
        metrics.tooltipReused();
        metrics.tooltipFallbackMiss();
        metrics.tooltipForced();
        metrics.fullInvalidation();
        metrics.tooltipCacheInvalidation();
        metrics.tooltipContextReset();
        metrics.hoverState("PREDICTIVE");

        BenchmarkSnapshot snapshot = BenchmarkSnapshot.capture(
            "Chest Sweep",
            "minecraft:generic_9x6",
            metrics,
            12,
            Instant.parse("2026-06-16T22:00:00Z")
        );

        assertEquals("Chest Sweep", snapshot.label());
        assertEquals("minecraft:generic_9x6", snapshot.screenId());
        assertEquals(1L, snapshot.tooltipHits());
        assertEquals(1L, snapshot.tooltipMisses());
        assertEquals(1L, snapshot.tooltipPathInvocations());
        assertEquals(1L, snapshot.tooltipFastPathChecksPerformed());
        assertEquals(1L, snapshot.tooltipFastPathHits());
        assertEquals(1L, snapshot.tooltipExpensivePathInvocations());
        assertEquals(1L, snapshot.tooltipCacheLookupsPerformed());
        assertEquals(1L, snapshot.tooltipFallbackEvaluations());
        assertEquals(1L, snapshot.tooltipReusedCount());
        assertEquals(1L, snapshot.tooltipFallbackMisses());
        assertEquals(1L, snapshot.tooltipForcedCount());
        assertEquals(3L, snapshot.invalidations());
        assertEquals(1L, snapshot.fullInvalidations());
        assertEquals(1L, snapshot.tooltipCacheInvalidations());
        assertEquals(1L, snapshot.tooltipContextResets());
        assertEquals("PREDICTIVE", snapshot.hoverState());
        assertEquals(12, snapshot.tooltipCacheEntries());
    }

    @Test
    void writerCreatesJsonSnapshotWithSanitizedName() throws IOException {
        ReliefMetrics metrics = new ReliefMetrics();
        BenchmarkSnapshot snapshot = BenchmarkSnapshot.capture(
            "Chest Sweep / AE2",
            "minecraft:generic_9x6",
            metrics,
            0,
            Instant.parse("2026-06-16T22:00:00Z")
        );

        Path written = BenchmarkSnapshotWriter.write(tempDir, snapshot);

        assertTrue(Files.exists(written));
        assertTrue(written.getFileName().toString().contains("chest-sweep-ae2"));
        assertFalse(written.getFileName().toString().contains("/"));

        BenchmarkSnapshot restored = GSON.fromJson(Files.readString(written), BenchmarkSnapshot.class);
        assertEquals(snapshot.label(), restored.label());
        assertEquals(snapshot.exportedAt(), restored.exportedAt());
        assertEquals(snapshot.screenId(), restored.screenId());
    }

    @Test
    void snapshotCapturesMeasurementMetrics() {
        ReliefMetrics metrics = new ReliefMetrics();
        metrics.applyDebugMode(true);
        // 8 tooltip-path invocations, 2 of which actually rebuilt (expensive path).
        for (int i = 0; i < 8; i++) {
            metrics.tooltipPathInvocation();
        }
        metrics.tooltipExpensivePathInvocation();
        metrics.tooltipExpensivePathInvocation();
        // Two builds measured at 100 ns and 200 ns -> average 150 ns.
        metrics.tooltipBuild(100L);
        metrics.tooltipBuild(200L);
        // Text-width cache traffic across lengths 1, 2 and 12.
        metrics.textWidthCacheHit(2);
        metrics.textWidthCacheHit(1);
        metrics.textWidthCacheMiss(12);

        assertEquals(2L, metrics.tooltipBuildSamples());
        assertEquals(150L, metrics.tooltipBuildAverageNanos());
        // avoided = 8 - 2 = 6 builds; estimated saved = 150 ns * 6 = 900 ns.
        assertEquals(900L, metrics.estimatedTooltipNanosSaved());
        assertEquals(2L, metrics.textWidthCacheHits());
        assertEquals(1L, metrics.textWidthCacheMisses());
        assertEquals(1L, metrics.textWidthLengthBucket(0)); // length 1
        assertEquals(1L, metrics.textWidthLengthBucket(1)); // length 2
        assertEquals(1L, metrics.textWidthLengthBucket(4)); // length 12 -> bucket "9+"

        BenchmarkSnapshot snapshot = BenchmarkSnapshot.capture(
            "Measurement", "minecraft:generic_9x6", metrics, 0, Instant.parse("2026-06-16T22:00:00Z"));
        assertEquals(2L, snapshot.tooltipBuildSamples());
        assertEquals(150L, snapshot.tooltipBuildAverageNanos());
        assertEquals(900L, snapshot.estimatedTooltipNanosSaved());
        assertEquals(2L, snapshot.textWidthCacheHits());
        assertEquals(1L, snapshot.textWidthCacheMisses());
        assertEquals(1L, snapshot.textWidthLen1());
        assertEquals(1L, snapshot.textWidthLen2());
        assertEquals(1L, snapshot.textWidthLen9plus());
    }
}
