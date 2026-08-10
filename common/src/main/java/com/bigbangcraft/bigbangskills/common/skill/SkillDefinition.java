package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.XpCurve;
import java.util.Objects;

public record SkillDefinition(SkillId id, String nameKey, int maxLevel, XpCurve curve, boolean enabled) {
    public SkillDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(nameKey); Objects.requireNonNull(curve);
        if (maxLevel < 1 || nameKey.isBlank()) throw new IllegalArgumentException("Invalid skill definition");
    }
}
