package com.bigbangcraft.bigbangskills.common.persistence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class PersistenceStatusFormatter {
    private PersistenceStatusFormatter() {}

    public static List<String> format(PersistenceStatus status, Clock clock) {
        var lastFlush = status.lastSuccessfulFlush() == null ? "none" : age(status.lastSuccessfulFlush(), clock.instant());
        return List.of(
            "Database: " + (status.databaseHealthy() ? "HEALTHY" : "UNAVAILABLE"),
            "Driver: " + status.driver(),
            "Cached players: " + status.cachedPlayers(),
            "Loading: " + status.loading(),
            "Dirty: " + status.dirty(),
            "Saving: " + status.saving(),
            "Failed: " + status.failed(),
            "Pending persistence operations: " + status.pendingOperations(),
            "Loads: " + status.loads() + " (failures " + status.loadFailures() + ")",
            "Saves: " + status.saves() + " (failures " + status.saveFailures() + ")",
            "Database latency: " + status.databaseLatencyMillis() + "ms",
            "Leaderboard cache age: n/a",
            "Last successful flush: " + lastFlush,
            "Last failed flush: " + status.lastFailedFlush());
    }

    private static String age(Instant timestamp, Instant now) {
        return Math.max(0, Duration.between(timestamp, now).toSeconds()) + "s ago";
    }
}
