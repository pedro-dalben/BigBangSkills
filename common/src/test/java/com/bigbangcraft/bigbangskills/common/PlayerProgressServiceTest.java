package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.api.XpSource;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.persistence.PlayerProgressService;
import com.bigbangcraft.bigbangskills.common.persistence.ProgressRepository;
import com.bigbangcraft.bigbangskills.common.persistence.ProgressRow;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerProgressServiceTest {
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final ProgressionScope SCOPE = ProgressionScope.server("test");

    @Test void queuesXpBeforeLoadAndKeepsNewMutationDuringSave() {
        var repository = new ControlledRepository();
        var registry = DefaultSkills.registry();
        var service = new PlayerProgressService(repository, registry, new GameplayService(registry), config(), Runnable::run, Executors.newSingleThreadScheduledExecutor(), ignored -> {});
        service.start(() -> CompletableFuture.completedFuture(null));
        var player = UUID.randomUUID();
        service.load(player, SCOPE);
        var first = service.blockBreak(action(player), BigDecimal.ONE, BigDecimal.ONE);
        assertEquals("profile_loading_queued", first.reason());
        repository.load.complete(Optional.empty());
        assertEquals(BigDecimal.ONE, service.progress(player).orElseThrow().get(MINING).totalXp());

        service.flush();
        assertEquals(1, repository.saved.size());
        var second = service.blockBreak(action(player), BigDecimal.ONE, BigDecimal.ONE);
        assertTrue(second.accepted());
        repository.save.complete(true);
        assertEquals(1, service.status().pendingOperations());
        service.flush();
        assertEquals(2, repository.saved.size());
        service.shutdown();
    }

    private static RuntimePersistenceConfig config() { return new RuntimePersistenceConfig(60, 1, 4, 8, List.of(Duration.ofMillis(1))); }
    private static BlockBreakAction action(UUID player) { return new BlockBreakAction(player, "minecraft:iron_ore", "minecraft:overworld", true, false, true, false, false, true); }

    private static final class ControlledRepository implements ProgressRepository {
        private final CompletableFuture<Optional<ProgressRow>> load = new CompletableFuture<>();
        private final CompletableFuture<Boolean> save = new CompletableFuture<>();
        private final List<UUID> saved = new ArrayList<>();

        @Override public CompletionStage<Optional<ProgressRow>> load(UUID playerId, SkillId skillId, ProgressionScope scope) { return load; }
        @Override public CompletionStage<Boolean> applyDelta(UUID eventId, UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal delta, XpSource source, String reason) { saved.add(eventId); return save; }
        @Override public void close() {}
    }
}
