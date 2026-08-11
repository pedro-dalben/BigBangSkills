package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.McMmoStandardLinearXpCurve;
import com.bigbangcraft.bigbangskills.common.progression.McMmoStandardExponentialXpCurve;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;

public final class DefaultSkills {
    private DefaultSkills() {}

    public static SkillRegistry registry() {
        return registry(SkillConfig.defaults());
    }

    public static SkillRegistry registry(SkillConfig config) {
        var registry = new SkillRegistry();
        for (var skill : new String[]{
                "acrobatics", "alchemy", "archery", "axes", "crossbows", "excavation", "fishing",
                "herbalism", "maces", "mining", "repair", "salvage", "smelting", "spears", "swords",
                "taming", "tridents", "unarmed", "woodcutting"}) {
            var id = SkillId.parse("bigbangskills:" + skill);
            var rule = config.rule(id);
            var curve = config.experienceCurve().equals("EXPONENTIAL")
                    ? new McMmoStandardExponentialXpCurve(config.exponentialBase(), config.exponentialMultiplier(), config.exponentialExponent())
                    : new McMmoStandardLinearXpCurve(config.linearBase(), config.linearMultiplier());
            registry.register(new SkillDefinition(
                    id,
                    "bigbangskills.skill." + skill,
                    rule.levelCap() == 0 ? Integer.MAX_VALUE : rule.levelCap(),
                    curve,
                    rule.enabled()));
        }
        return registry;
    }
}
