package com.bigbangcraft.bigbangskills.persistence;

import com.bigbangcraft.bigbangskills.api.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdbcProgressRepositoryTest {
    @Test void sqliteDeltaIsIdempotent() throws Exception {
        var db = Files.createTempFile("bigbangskills-", ".db");
        var ds = new org.sqlite.SQLiteDataSource(); ds.setUrl("jdbc:sqlite:" + db);
        try (var repository = new JdbcProgressRepository(ds)) {
            repository.initialize(); repository.initialize(); var player = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("test"); var event = UUID.randomUUID();
            assertTrue(repository.applyDelta(event, player, skill, scope, BigDecimal.TEN, XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            assertFalse(repository.applyDelta(event, player, skill, scope, BigDecimal.TEN, XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            var row = repository.load(player, skill, scope).toCompletableFuture().get().orElseThrow();
            assertEquals(0, BigDecimal.TEN.compareTo(row.totalXp())); assertEquals(1, row.revision());
            assertEquals(player, repository.leaderboard(skill, scope, 1).toCompletableFuture().get().getFirst().playerId());
        }
        Files.deleteIfExists(db);
    }

    @Test void concurrentWritersAddBothDeltas() throws Exception {
        var db = Files.createTempFile("bigbangskills-concurrent-", ".db");
        var firstDataSource = new org.sqlite.SQLiteDataSource(); firstDataSource.setUrl("jdbc:sqlite:" + db + "?busy_timeout=5000");
        var secondDataSource = new org.sqlite.SQLiteDataSource(); secondDataSource.setUrl("jdbc:sqlite:" + db + "?busy_timeout=5000");
        var first = new JdbcProgressRepository(firstDataSource, "a"); var second = new JdbcProgressRepository(secondDataSource, "b");
        try (first; second) {
            first.initialize();
            var player = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("test");
            var a = first.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(50), XpSource.BLOCK_BREAK, "a").toCompletableFuture();
            var b = second.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(30), XpSource.BLOCK_BREAK, "b").toCompletableFuture();
            CompletableFuture.allOf(a, b).get();
            assertTrue(a.get()); assertTrue(b.get());
            assertEquals(0, BigDecimal.valueOf(80).compareTo(first.load(player, skill, scope).toCompletableFuture().get().orElseThrow().totalXp()));
        } finally { Files.deleteIfExists(db); }
    }

    @Test void sqliteProgressSurvivesRepositoryRestart() throws Exception {
        var db = Files.createTempFile("bigbangskills-restart-", ".db");
        var player = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("test");
        try {
            var firstDataSource = new org.sqlite.SQLiteDataSource(); firstDataSource.setUrl("jdbc:sqlite:" + db);
            try (var first = new JdbcProgressRepository(firstDataSource, "first")) {
                first.initialize();
                assertTrue(first.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(7), XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            }
            var secondDataSource = new org.sqlite.SQLiteDataSource(); secondDataSource.setUrl("jdbc:sqlite:" + db);
            try (var second = new JdbcProgressRepository(secondDataSource, "second")) {
                second.initialize();
                assertEquals(0, BigDecimal.valueOf(7).compareTo(second.load(player, skill, scope).toCompletableFuture().get().orElseThrow().totalXp()));
            }
        } finally { Files.deleteIfExists(db); }
    }

    @Test void adminRemovalCannotUnderflowAndPersists() throws Exception {
        var db = Files.createTempFile("bigbangskills-admin-", ".db");
        try {
            var dataSource = new org.sqlite.SQLiteDataSource(); dataSource.setUrl("jdbc:sqlite:" + db);
            try (var repository = new JdbcProgressRepository(dataSource, "admin-test")) {
                repository.initialize();
                var player = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("test");
                assertTrue(repository.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(50), XpSource.ADMIN, "admin_add").toCompletableFuture().get());
                assertTrue(repository.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(-20), XpSource.ADMIN, "admin_remove").toCompletableFuture().get());
                assertFalse(repository.applyDelta(UUID.randomUUID(), player, skill, scope, BigDecimal.valueOf(-40), XpSource.ADMIN, "admin_remove").toCompletableFuture().get());
                assertEquals(0, BigDecimal.valueOf(30).compareTo(repository.load(player, skill, scope).toCompletableFuture().get().orElseThrow().totalXp()));
            }
        } finally { Files.deleteIfExists(db); }
    }
}
