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
}
