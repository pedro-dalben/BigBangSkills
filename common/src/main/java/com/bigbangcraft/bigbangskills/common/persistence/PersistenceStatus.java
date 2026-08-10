package com.bigbangcraft.bigbangskills.common.persistence;

import java.time.Instant;

public record PersistenceStatus(
    boolean databaseHealthy,
    boolean acceptingMutations,
    String driver,
    int cachedPlayers,
    int loading,
    int dirty,
    int saving,
    int failed,
    int pendingOperations,
    long loads,
    long loadFailures,
    long saves,
    long saveFailures,
    long databaseLatencyMillis,
    Instant lastSuccessfulFlush,
    String lastFailedFlush) {}
