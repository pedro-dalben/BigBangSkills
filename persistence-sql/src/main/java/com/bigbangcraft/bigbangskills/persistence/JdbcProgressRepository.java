package com.bigbangcraft.bigbangskills.persistence;

import com.bigbangcraft.bigbangskills.api.*;
import com.bigbangcraft.bigbangskills.common.persistence.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;

public final class JdbcProgressRepository implements ProgressRepository {
    private final DataSource dataSource;
    private final ExecutorService executor;
    private final String serverId;

    public JdbcProgressRepository(DataSource dataSource) {
        this(dataSource, "unknown");
    }

    public JdbcProgressRepository(DataSource dataSource, String serverId) {
        this.dataSource = dataSource;
        this.serverId = serverId == null || serverId.isBlank() ? "unknown" : serverId;
        this.executor = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "bigbangskills-sql"); t.setDaemon(true); return t; });
    }

    public void initialize() throws SQLException {
        try (var c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INT NOT NULL PRIMARY KEY, checksum VARCHAR(128) NOT NULL, applied_at TIMESTAMP NOT NULL)");
                if (currentVersion(c) < 1) {
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS player_progress (player_uuid VARCHAR(36) NOT NULL, skill_id VARCHAR(200) NOT NULL, scope_type VARCHAR(16) NOT NULL, scope_id VARCHAR(128) NOT NULL, total_xp DECIMAL(20,4) NOT NULL, revision BIGINT NOT NULL, definition_version INT NOT NULL, updated_at TIMESTAMP NOT NULL, PRIMARY KEY(player_uuid, skill_id, scope_type, scope_id))");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS xp_ledger (event_id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, skill_id VARCHAR(200) NOT NULL, scope_type VARCHAR(16) NOT NULL, scope_id VARCHAR(128) NOT NULL, delta_xp DECIMAL(20,4) NOT NULL, source VARCHAR(32) NOT NULL, reason VARCHAR(255) NOT NULL, server_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL)");
                    try { s.executeUpdate("CREATE INDEX idx_progress_skill_scope ON player_progress (skill_id, scope_type, scope_id, total_xp)"); } catch (SQLException alreadyExists) { if (!isAlreadyExists(alreadyExists)) throw alreadyExists; }
                    try (var insert = c.prepareStatement("INSERT INTO schema_version(version, checksum, applied_at) VALUES (?,?,?)")) {
                        insert.setInt(1, 1); insert.setString(2, "initial-player-progress-ledger"); insert.setTimestamp(3, Timestamp.from(Instant.now())); insert.executeUpdate();
                    }
                }
            }
            c.commit();
        }
    }

    public CompletionStage<Void> initializeAsync() { return CompletableFuture.runAsync(() -> { try { initialize(); } catch (SQLException failure) { throw new CompletionException(failure); } }, executor); }

    private static int currentVersion(Connection connection) throws SQLException {
        try (var query = connection.createStatement(); var rows = query.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) { rows.next(); return rows.getInt(1); }
    }

    @Override public CompletionStage<Optional<ProgressRow>> load(UUID playerId, SkillId skillId, ProgressionScope scope) {
        return CompletableFuture.supplyAsync(() -> {
            try (var c = dataSource.getConnection(); var p = c.prepareStatement("SELECT total_xp, revision, definition_version, updated_at FROM player_progress WHERE player_uuid=? AND skill_id=? AND scope_type=? AND scope_id=?")) {
                p.setString(1, playerId.toString()); p.setString(2, skillId.toString()); p.setString(3, scope.type().name()); p.setString(4, scope.id());
                try (var r = p.executeQuery()) { if (!r.next()) return Optional.empty(); return Optional.of(new ProgressRow(playerId, skillId, scope, r.getBigDecimal(1), r.getLong(2), r.getInt(3), r.getTimestamp(4).toInstant())); }
            } catch (SQLException e) { throw new CompletionException(e); }
        }, executor);
    }

    @Override public CompletionStage<List<ProgressRow>> loadAll(UUID playerId, Collection<SkillId> skills, ProgressionScope scope) {
        if (skills.isEmpty()) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.supplyAsync(() -> {
            var wanted = skills.stream().collect(java.util.stream.Collectors.toMap(SkillId::toString, skill -> skill));
            try (var c = dataSource.getConnection(); var p = c.prepareStatement("SELECT skill_id, total_xp, revision, definition_version, updated_at FROM player_progress WHERE player_uuid=? AND scope_type=? AND scope_id=?")) {
                p.setString(1, playerId.toString()); p.setString(2, scope.type().name()); p.setString(3, scope.id());
                try (var r = p.executeQuery()) {
                    var rows = new java.util.ArrayList<ProgressRow>();
                    while (r.next()) {
                        var skill = wanted.get(r.getString(1));
                        if (skill != null) rows.add(new ProgressRow(playerId, skill, scope, r.getBigDecimal(2), r.getLong(3), r.getInt(4), r.getTimestamp(5).toInstant()));
                    }
                    return List.copyOf(rows);
                }
            } catch (SQLException e) { throw new CompletionException(e); }
        }, executor);
    }

    @Override public CompletionStage<List<LeaderboardRow>> leaderboard(SkillId skillId, ProgressionScope scope, int limit) {
        if (limit < 1) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.supplyAsync(() -> {
            try (var c = dataSource.getConnection(); var p = c.prepareStatement("SELECT player_uuid, total_xp, updated_at FROM player_progress WHERE skill_id=? AND scope_type=? AND scope_id=? ORDER BY total_xp DESC, updated_at DESC, player_uuid ASC LIMIT ?")) {
                p.setString(1, skillId.toString()); p.setString(2, scope.type().name()); p.setString(3, scope.id()); p.setInt(4, Math.min(limit, 100));
                try (var rows = p.executeQuery()) {
                    var result = new java.util.ArrayList<LeaderboardRow>();
                    while (rows.next()) result.add(new LeaderboardRow(UUID.fromString(rows.getString(1)), rows.getBigDecimal(2), rows.getTimestamp(3).toInstant()));
                    return List.copyOf(result);
                }
            } catch (SQLException failure) { throw new CompletionException(failure); }
        }, executor);
    }

    @Override public CompletionStage<Boolean> applyDelta(UUID eventId, UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal delta, XpSource source, String reason) {
        if (delta == null || delta.signum() < 0) return CompletableFuture.failedFuture(new IllegalArgumentException("delta must be non-negative"));
        return CompletableFuture.supplyAsync(() -> {
            try (var c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (var ledger = c.prepareStatement("INSERT INTO xp_ledger(event_id, player_uuid, skill_id, scope_type, scope_id, delta_xp, source, reason, server_id, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ledger.setString(1, eventId.toString()); ledger.setString(2, playerId.toString()); ledger.setString(3, skillId.toString()); ledger.setString(4, scope.type().name()); ledger.setString(5, scope.id()); ledger.setBigDecimal(6, delta); ledger.setString(7, source.name()); ledger.setString(8, reason); ledger.setString(9, serverId); ledger.setTimestamp(10, Timestamp.from(Instant.now())); ledger.executeUpdate();
                } catch (SQLException duplicate) {
                    if (isConstraint(duplicate)) { c.rollback(); return false; }
                    throw duplicate;
                }
                var updated = 0;
                try (var update = c.prepareStatement("UPDATE player_progress SET total_xp=total_xp+?, revision=revision+1, updated_at=? WHERE player_uuid=? AND skill_id=? AND scope_type=? AND scope_id=?")) {
                    update.setBigDecimal(1, delta); update.setTimestamp(2, Timestamp.from(Instant.now())); update.setString(3, playerId.toString()); update.setString(4, skillId.toString()); update.setString(5, scope.type().name()); update.setString(6, scope.id()); updated = update.executeUpdate();
                }
                if (updated == 0) try (var insert = c.prepareStatement("INSERT INTO player_progress(player_uuid, skill_id, scope_type, scope_id, total_xp, revision, definition_version, updated_at) VALUES (?,?,?,?,?,?,?,?)")) {
                    insert.setString(1, playerId.toString()); insert.setString(2, skillId.toString()); insert.setString(3, scope.type().name()); insert.setString(4, scope.id()); insert.setBigDecimal(5, delta); insert.setLong(6, 1); insert.setInt(7, 1); insert.setTimestamp(8, Timestamp.from(Instant.now())); insert.executeUpdate();
                } catch (SQLException concurrentInsert) {
                    if (!isConstraint(concurrentInsert)) throw concurrentInsert;
                    try (var retry = c.prepareStatement("UPDATE player_progress SET total_xp=total_xp+?, revision=revision+1, updated_at=? WHERE player_uuid=? AND skill_id=? AND scope_type=? AND scope_id=?")) {
                        retry.setBigDecimal(1, delta); retry.setTimestamp(2, Timestamp.from(Instant.now())); retry.setString(3, playerId.toString()); retry.setString(4, skillId.toString()); retry.setString(5, scope.type().name()); retry.setString(6, scope.id()); retry.executeUpdate();
                    }
                }
                c.commit(); return true;
            } catch (SQLException e) { throw new CompletionException(e); }
        }, executor);
    }

    private static boolean isConstraint(SQLException failure) { return failure.getSQLState() != null && failure.getSQLState().startsWith("23") || String.valueOf(failure.getMessage()).toUpperCase(Locale.ROOT).matches(".*(UNIQUE|PRIMARY KEY|CONSTRAINT).*" ); }
    private static boolean isAlreadyExists(SQLException failure) { return String.valueOf(failure.getMessage()).toLowerCase(Locale.ROOT).contains("already exists") || String.valueOf(failure.getMessage()).toLowerCase(Locale.ROOT).contains("duplicate key name"); }
    @Override public void close() { executor.shutdown(); if (dataSource instanceof AutoCloseable closeable) try { closeable.close(); } catch (Exception ignored) {} }
}
