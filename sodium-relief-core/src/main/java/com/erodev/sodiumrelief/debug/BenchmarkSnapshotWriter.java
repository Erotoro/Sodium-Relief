package com.erodev.sodiumrelief.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class BenchmarkSnapshotWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BenchmarkSnapshotWriter() {
    }

    public static Path write(Path directory, BenchmarkSnapshot snapshot) throws IOException {
        Files.createDirectories(directory);
        String slug = sanitize(snapshot.label());
        String timestamp = snapshot.exportedAt().replace(":", "-");
        Path target = directory.resolve(timestamp + "-" + slug + ".json");
        Path temporary = directory.resolve(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(snapshot, writer);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String sanitize(String label) {
        String normalized = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized.isEmpty() ? "snapshot" : normalized;
    }
}
