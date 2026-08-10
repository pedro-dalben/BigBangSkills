package com.bigbangcraft.bigbangskills.common.ability;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.time.Duration;
import java.util.Objects;

public record AbilityDefinition(String id, SkillId skillId, AbilityType type, int unlockLevel, Duration cooldown, Duration duration) {
    public AbilityDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(skillId); Objects.requireNonNull(type); Objects.requireNonNull(cooldown); Objects.requireNonNull(duration);
        if (id.isBlank() || unlockLevel < 1 || cooldown.isNegative() || duration.isNegative()) throw new IllegalArgumentException("Invalid ability definition");
    }
}
