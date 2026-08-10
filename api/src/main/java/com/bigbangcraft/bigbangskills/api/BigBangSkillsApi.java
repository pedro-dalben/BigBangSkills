package com.bigbangcraft.bigbangskills.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface BigBangSkillsApi {
    Optional<PlayerProgressView> getPlayerProgress(UUID playerId);
    Optional<SkillState> getSkillState(UUID playerId, SkillId skillId, ProgressionScope scope);
    int getPowerLevel(UUID playerId, ProgressionScope scope);
    void addXp(UUID playerId, SkillId skillId, BigDecimal amount, XpSource source, String reason, ProgressionScope scope);
}
