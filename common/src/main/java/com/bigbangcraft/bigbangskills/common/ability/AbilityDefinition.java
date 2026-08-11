package com.bigbangcraft.bigbangskills.common.ability;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record AbilityDefinition(String id, SkillId skillId, AbilityType type, int unlockLevel, Duration cooldown, Duration duration, List<Integer> rankUnlockLevels) {
    public AbilityDefinition(String id, SkillId skillId, AbilityType type, int unlockLevel, Duration cooldown, Duration duration) {
        this(id, skillId, type, unlockLevel, cooldown, duration, List.of(unlockLevel));
    }
    public AbilityDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(skillId); Objects.requireNonNull(type); Objects.requireNonNull(cooldown); Objects.requireNonNull(duration);
        Objects.requireNonNull(rankUnlockLevels);
        rankUnlockLevels = List.copyOf(rankUnlockLevels);
        if (id.isBlank() || unlockLevel < 0 || cooldown.isNegative() || duration.isNegative() || rankUnlockLevels.isEmpty()
                || rankUnlockLevels.stream().anyMatch(level -> level < 0) || rankUnlockLevels.stream().sorted().toList().equals(rankUnlockLevels) == false) {
            throw new IllegalArgumentException("Invalid ability definition");
        }
    }
    public int rankForLevel(int level) { return (int) rankUnlockLevels.stream().filter(unlock -> unlock <= level).count(); }
}
