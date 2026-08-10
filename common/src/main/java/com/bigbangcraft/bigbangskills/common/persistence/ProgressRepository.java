package com.bigbangcraft.bigbangskills.common.persistence;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import com.bigbangcraft.bigbangskills.api.*;

public interface ProgressRepository extends AutoCloseable {
    CompletionStage<Optional<ProgressRow>> load(UUID playerId, SkillId skillId, ProgressionScope scope);
    default CompletionStage<List<ProgressRow>> loadAll(UUID playerId, Collection<SkillId> skills, ProgressionScope scope) {
        var futures = skills.stream().map(skill -> load(playerId, skill, scope).toCompletableFuture()).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .thenApply(ignored -> futures.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
    }
    default CompletionStage<List<LeaderboardRow>> leaderboard(SkillId skillId, ProgressionScope scope, int limit) { return CompletableFuture.completedFuture(List.of()); }
    CompletionStage<Boolean> applyDelta(UUID eventId, UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal delta, XpSource source, String reason);
    @Override void close();
}
