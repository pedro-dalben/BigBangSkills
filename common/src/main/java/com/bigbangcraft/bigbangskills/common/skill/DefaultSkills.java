package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.LinearXpCurve;
import java.math.BigDecimal;

public final class DefaultSkills {
    private DefaultSkills() {}

    public static SkillRegistry registry() {
        var registry = new SkillRegistry();
        registry.register(new SkillDefinition(SkillId.parse("bigbangskills:mining"), "bigbangskills.skill.mining", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        registry.register(new SkillDefinition(SkillId.parse("bigbangskills:woodcutting"), "bigbangskills.skill.woodcutting", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        return registry;
    }
}
