package com.erodev.sodiumrelief.config;

import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;

public final class ReliefConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve(SodiumReliefClient.MOD_ID + ".json");
    private ReliefConfig config = new ReliefConfig();

    public void load() {
        if (Files.notExists(path)) {
            config.normalize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            config = Objects.requireNonNullElseGet(read(reader), ReliefConfig::new);
            config.normalize();
        } catch (IOException | JsonParseException exception) {
            backupBrokenConfig();
            ReliefLogger.warn("Failed to load Sodium Relief config, using defaults", exception);
            config = new ReliefConfig();
            config.normalize();
            save();
        }
    }

    public void save() {
        try {
            config.normalize();
            Files.createDirectories(path.getParent());
            Path tempPath = temporaryPath();
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            ReliefLogger.warn("Failed to save Sodium Relief config", exception);
        }
    }

    public ReliefConfig config() {
        return config;
    }

    public void normalizeInMemory() {
        config.normalize();
    }

    @SuppressWarnings({"DataFlowIssue", "NullableProblems"})
    private static ReliefConfig read(Reader reader) {
        return GSON.fromJson(reader, ReliefConfig.class);
    }

    private void backupBrokenConfig() {
        if (Files.notExists(path)) {
            return;
        }
        Path backupPath = path.resolveSibling(path.getFileName() + ".broken-" + BACKUP_TIMESTAMP.format(LocalDateTime.now()));
        try {
            Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException backupException) {
            ReliefLogger.warn("Failed to preserve broken Sodium Relief config", backupException);
        }
    }

    private Path temporaryPath() {
        return path.resolveSibling(path.getFileName() + ".tmp");
    }
}
