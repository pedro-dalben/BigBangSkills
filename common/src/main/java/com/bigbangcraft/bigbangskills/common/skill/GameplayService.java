package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.antiexploit.XpEligibilityService;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.xp.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GameplayService {
    public record Outcome(boolean accepted, SkillId skillId, BigDecimal amount, String reason, UUID requestId, com.bigbangcraft.bigbangskills.api.ProgressionScope scope) {}
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final SkillId WOODCUTTING = SkillId.parse("bigbangskills:woodcutting");
    private final SkillRegistry registry;
    private final XpService xp = new XpService();
    private final XpEligibilityService eligibility = new XpEligibilityService();

    public GameplayService(SkillRegistry registry) { this.registry = registry; }
    public Outcome blockBreak(PlayerProgress player, BlockBreakAction action, BigDecimal miningXp, BigDecimal woodcuttingXp, com.bigbangcraft.bigbangskills.api.ProgressionScope scope) {
        var decision = eligibility.check(action.realPlayer(), action.eventCancelled(), action.placed(), action.provenanceKnown());
        if (!decision.accepted()) return rejected(decision.reason());
        var skill = action.miningBlock() ? MINING : action.woodcuttingBlock() ? WOODCUTTING : null;
        var amount = action.miningBlock() ? miningXp : action.woodcuttingBlock() ? woodcuttingXp : BigDecimal.ZERO;
        if (skill == null) return rejected("block_not_configured");
        var request = new XpRequest(UUID.randomUUID(), action.playerId(), skill, amount, com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, action.blockId(), scope, Instant.now());
        var result = xp.apply(player, request, registry, List.of());
        return new Outcome(result.accepted(), skill, result.amount(), result.reason(), result.accepted() ? request.requestId() : null, scope);
    }
    private static Outcome rejected(String reason) { return new Outcome(false, null, BigDecimal.ZERO, reason, null, null); }
}
