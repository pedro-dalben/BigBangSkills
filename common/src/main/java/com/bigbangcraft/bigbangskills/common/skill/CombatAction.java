package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;

import java.math.BigDecimal;
import java.util.UUID;

public record CombatAction(UUID playerId, SkillId skillId, String weaponId, BigDecimal baseXp,
                           double damage, double attackStrength, boolean pvp, boolean targetHasArmor,
                           int targetArmorQuality,
                           boolean abilityActive, ProgressionScope scope) {
    public CombatAction(UUID playerId, SkillId skillId, String weaponId, BigDecimal baseXp,
                         double damage, double attackStrength, boolean pvp, boolean targetHasArmor,
                         boolean abilityActive, ProgressionScope scope) {
        this(playerId, skillId, weaponId, baseXp, damage, attackStrength, pvp, targetHasArmor,
                targetHasArmor ? 13 : 0, abilityActive, scope);
    }

    public CombatAction {
        if (playerId == null || skillId == null || weaponId == null || weaponId.isBlank() || baseXp == null
                || baseXp.signum() < 0 || !Double.isFinite(damage) || damage < 0
                || !Double.isFinite(attackStrength) || attackStrength < 0 || attackStrength > 1 || scope == null) {
            throw new IllegalArgumentException("Invalid combat action");
        }
        if (targetArmorQuality < 0) throw new IllegalArgumentException("Invalid armor quality");
    }
}
