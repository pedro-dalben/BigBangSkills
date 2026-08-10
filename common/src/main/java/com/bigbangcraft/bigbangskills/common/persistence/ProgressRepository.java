package com.bigbangcraft.bigbangskills.common.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import com.bigbangcraft.bigbangskills.api.*;

public interface ProgressRepository extends AutoCloseable {
    CompletionStage<Optional<ProgressRow>> load(UUID playerId, SkillId skillId, ProgressionScope scope);
    CompletionStage<Boolean> applyDelta(UUID eventId, UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal delta, XpSource source, String reason);
    @Override void close();
}
