package com.bigbangcraft.bigbangskills.common.config;

import java.time.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.List;

public record RuntimePersistenceConfig(
    int flushIntervalSeconds,
    int shutdownFlushTimeoutSeconds,
    int maxPreloadXpPerPlayer,
    int maxPendingSaveEventsPerPlayer,
    List<Duration> retryBackoff) {
    public RuntimePersistenceConfig {
        if (flushIntervalSeconds < 1 || shutdownFlushTimeoutSeconds < 1 || maxPreloadXpPerPlayer < 1 || maxPendingSaveEventsPerPlayer < 1 || retryBackoff == null || retryBackoff.isEmpty()) {
            throw new IllegalArgumentException("Invalid runtime persistence config");
        }
        retryBackoff = List.copyOf(retryBackoff);
        if (retryBackoff.stream().anyMatch(duration -> duration.isZero() || duration.isNegative())) throw new IllegalArgumentException("Invalid retry backoff");
    }

    public static RuntimePersistenceConfig defaults() {
        return new RuntimePersistenceConfig(30, 15, 128, 2_048, List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(30)));
    }

    public static RuntimePersistenceConfig loadOrCreate(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().normalize().getParent());
        if (Files.notExists(file)) Files.writeString(file, "# BigBangSkills runtime persistence limits\n" +
            "persistence.flush_interval_seconds=30\n" +
            "persistence.shutdown_flush_timeout_seconds=15\n" +
            "persistence.max_preload_xp_per_player=128\n" +
            "persistence.max_pending_save_events_per_player=2048\n" +
            "persistence.retry_backoff_seconds=1,2,5,10,30\n");
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) { properties.load(reader); }
        return new RuntimePersistenceConfig(
            integer(properties, "persistence.flush_interval_seconds", 30),
            integer(properties, "persistence.shutdown_flush_timeout_seconds", 15),
            integer(properties, "persistence.max_preload_xp_per_player", 128),
            integer(properties, "persistence.max_pending_save_events_per_player", 2_048),
            properties.getProperty("persistence.retry_backoff_seconds", "1,2,5,10,30").lines().flatMap(value -> java.util.Arrays.stream(value.split(","))).map(String::trim).filter(value -> !value.isBlank()).map(RuntimePersistenceConfig::seconds).toList());
    }

    private static int integer(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid integer: " + key, failure); }
    }

    private static Duration seconds(String value) {
        try { return Duration.ofSeconds(Long.parseLong(value)); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid retry backoff: " + value, failure); }
    }
}
