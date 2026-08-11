package com.bigbangcraft.bigbangskills.common.skill;

import java.math.BigDecimal;

public record CombatResolution(SkillAwardAction award, CombatEffect effect) {
    public CombatResolution withAwardAmount(BigDecimal amount) {
        var current = award;
        return new CombatResolution(new SkillAwardAction(current.playerId(), current.skillId(), amount, current.source(),
                current.reason(), current.scope(), current.realPlayer(), current.eventCancelled(), current.pvp(), current.pve()), effect);
    }
}
