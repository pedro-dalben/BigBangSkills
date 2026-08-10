package com.bigbangcraft.bigbangskills.persistence;

import com.bigbangcraft.bigbangskills.api.*;
import com.bigbangcraft.bigbangskills.common.persistence.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;

public final class JdbcProgressRepository implements ProgressRepository {
    private final DataSource dataSource;
    private final ExecutorService executor;

    public JdbcProgressRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.executor = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "bigbangskills-sql"); t.setDaemon(true); return t; });
    }

    public void initialize() throws SQLException {
        try (var c = dataSource.getConnection(); var s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player_progress (player_uuid VARCHAR(36) NOT NULL, skill_id VARCHAR(200) NOT NULL, scope_type VARCHAR(16) NOT NULL, scope_id VARCHAR(128) NOT NULL, total_xp DECIMAL(20,4) NOT NULL, revision BIGINT NOT NULL, definition_version INT NOT NULL, updated_at TIMESTAMP NOT NULL, PRIMARY KEY(player_uuid, skill_id, scope_type, scope_id))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS xp_ledger (event_id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, skill_id VARCHAR(200) NOT NULL, scope_type VARCHAR(16) NOT NULL, scope_id VARCHAR(128) NOT NULL, delta_xp DECIMAL(20,4) NOT NULL, source VARCHAR(32) NOT NULL, reason VARCHAR(255) NOT NULL, server_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL)");
        }
    }

    @Override public CompletionStage<Optional<ProgressRow>> load(UUID playerId, SkillId skillId, ProgressionScope scope) {
        return CompletableFuture.supplyAsync(() -> {
            try (var c = dataSource.getConnection(); var p = c.prepareStatement("SELECT total_xp, revision, definition_version, updated_at FROM player_progress WHERE player_uuid=? AND skill_id=? AND scope_type=? AND scope_id=?")) {
                p.setString(1, playerId.toString()); p.setString(2, skillId.toString()); p.setString(3, scope.type().name()); p.setString(4, scope.id());
                try (var r = p.executeQuery()) { if (!r.next()) return Optional.empty(); return Optional.of(new ProgressRow(playerId, skillId, scope, r.getBigDecimal(1), r.getLong(2), r.getInt(3), r.getTimestamp(4).toInstant())); }
            } catch (SQLException e) { throw new CompletionException(e); }
        }, executor);
    }

    @Override public CompletionStage<Boolean> applyDelta(UUID eventId, UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal delta, XpSource source, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try (var c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (var ledger = c.prepareStatement("INSERT INTO xp_ledger VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ledger.setString(1, eventId.toString()); ledger.setString(2, playerId.toString()); ledger.setString(3, skillId.toString()); ledger.setString(4, scope.type().name()); ledger.setString(5, scope.id()); ledger.setBigDecimal(6, delta); ledger.setString(7, source.name()); ledger.setString(8, reason); ledger.setString(9, "unknown"); ledger.setTimestamp(10, Timestamp.from(Instant.now())); ledger.executeUpdate();
                } catch (SQLException duplicate) {
                    if (duplicate.getMessage() != null && duplicate.getMessage().toUpperCase().contains("UNIQUE")) { c.rollback(); return false; }
                    throw duplicate;
                }
                var updated = 0;
                try (var update = c.prepareStatement("UPDATE player_progress SET total_xp=total_xp+?, revision=revision+1, updated_at=? WHERE player_uuid=? AND skill_id=? AND scope_type=? AND scope_id=?")) {
                    update.setBigDecimal(1, delta); update.setTimestamp(2, Timestamp.from(Instant.now())); update.setString(3, playerId.toString()); update.setString(4, skillId.toString()); update.setString(5, scope.type().name()); update.setString(6, scope.id()); updated = update.executeUpdate();
                }
                if (updated == 0) try (var insert = c.prepareStatement("INSERT INTO player_progress VALUES (?,?,?,?,?,?,?,?)")) {
                    insert.setString(1, playerId.toString()); insert.setString(2, skillId.toString()); insert.setString(3, scope.type().name()); insert.setString(4, scope.id()); insert.setBigDecimal(5, delta); insert.setLong(6, 1); insert.setInt(7, 1); insert.setTimestamp(8, Timestamp.from(Instant.now())); insert.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException e) { throw new CompletionException(e); }
        }, executor);
    }

    @Override public void close() { executor.shutdown(); }
}
