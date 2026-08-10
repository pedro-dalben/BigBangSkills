package com.bigbangcraft.bigbangskills.common.progression;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.math.BigDecimal;

public record SkillProgress(SkillId skillId, BigDecimal totalXp, int level, long revision) {
    public SkillProgress add(BigDecimal amount, int maxLevel, XpCurve curve) {
        if (amount.signum() < 0) throw new IllegalArgumentException("XP cannot be negative");
        var next = totalXp.add(amount);
        return new SkillProgress(skillId, next, curve.levelAt(next, maxLevel), revision + 1);
    }
}
