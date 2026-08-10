package com.bigbangcraft.bigbangskills.api;

import java.util.Map;
import java.util.UUID;

public record PlayerProgressView(UUID playerId, Map<SkillId, SkillState> skills, int powerLevel) {
    public PlayerProgressView { skills = Map.copyOf(skills); }
}
