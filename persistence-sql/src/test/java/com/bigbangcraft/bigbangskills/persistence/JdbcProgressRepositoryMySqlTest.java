package com.bigbangcraft.bigbangskills.persistence;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.api.XpSource;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JdbcProgressRepositoryMySqlTest {
    @Test void mysqlContractIsReproducibleWhenConfigured() throws Exception {
        var url = System.getenv("BIGBANGSKILLS_MYSQL_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "Set BIGBANGSKILLS_MYSQL_JDBC_URL to run the real MySQL/MariaDB contract");
        var dataSource = new DriverManagerDataSource(url).property("user", System.getenv().getOrDefault("BIGBANGSKILLS_MYSQL_USER", "root")).property("password", System.getenv().getOrDefault("BIGBANGSKILLS_MYSQL_PASSWORD", ""));
        try (var repository = new JdbcProgressRepository(dataSource, "integration")) {
            repository.initialize();
            var player = UUID.randomUUID(); var other = UUID.randomUUID(); var skill = SkillId.parse("bigbangskills:mining"); var scope = ProgressionScope.server("integration");
            var event = UUID.randomUUID();
            assertTrue(repository.applyDelta(event, player, skill, scope, BigDecimal.valueOf(50), XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            assertFalse(repository.applyDelta(event, player, skill, scope, BigDecimal.valueOf(50), XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            assertTrue(repository.applyDelta(UUID.randomUUID(), other, skill, scope, BigDecimal.valueOf(30), XpSource.BLOCK_BREAK, "ore").toCompletableFuture().get());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(repository.load(player, skill, scope).toCompletableFuture().get().orElseThrow().totalXp()));
            assertEquals(player, repository.leaderboard(skill, scope, 1).toCompletableFuture().get().getFirst().playerId());
        }
    }
}
