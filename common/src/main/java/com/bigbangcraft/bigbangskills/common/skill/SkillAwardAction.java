package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.api.XpSource;

import java.math.BigDecimal;
import java.util.UUID;

/** Server-side event normalized before it reaches progression/persistence. */
public record SkillAwardAction(UUID playerId, SkillId skillId, BigDecimal amount, XpSource source,
                               String reason, ProgressionScope scope, boolean realPlayer,
                               boolean eventCancelled, boolean pvp, boolean pve) {
    public SkillAwardAction {
        if (playerId == null || skillId == null || amount == null || source == null || scope == null
                || reason == null || reason.isBlank() || amount.signum() < 0) {
            throw new IllegalArgumentException("Invalid skill award");
        }
    }
}
