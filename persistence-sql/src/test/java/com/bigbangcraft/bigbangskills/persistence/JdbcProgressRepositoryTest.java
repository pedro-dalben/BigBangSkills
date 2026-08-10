package com.bigbangcraft.bigbangskills.persistence;

import com.bigbangcraft.bigbangskills.api.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdbcProgressRepositoryTest {
    @Test void sqliteDeltaIsIdempotent() throws Exception {
        var db = Files.createTempFile("bigbangskills-", ".db");
        var ds = new org.sqlite.SQLiteDataSource(); ds.setUrl("jdbc:sqlite:" + db);
        try (var repository = new JdbcProgressRepository(ds)) {
            repository.initialize(); var player = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("test"); var event = UUID.randomUUID();
            assertTrue(repository.applyDelta(event, player, skill, scope, BigDecimal.TEN, XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            assertFalse(repository.applyDelta(event, player, skill, scope, BigDecimal.TEN, XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            var row = repository.load(player, skill, scope).toCompletableFuture().get().orElseThrow();
            assertEquals(0, BigDecimal.TEN.compareTo(row.totalXp())); assertEquals(1, row.revision());
        }
        Files.deleteIfExists(db);
    }
}
