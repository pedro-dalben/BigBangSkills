package com.bigbangcraft.bigbangskills.common.xp;

import com.bigbangcraft.bigbangskills.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record XpRequest(UUID requestId, UUID playerId, SkillId skillId, BigDecimal baseAmount, XpSource source, String reason, ProgressionScope scope, Instant createdAt) {
    public XpRequest {
        if (baseAmount.signum() < 0) throw new IllegalArgumentException("XP cannot be negative");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason required");
    }
}
