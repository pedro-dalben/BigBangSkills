package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.antiexploit.XpEligibilityService;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.xp.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameplayService {
    public record Outcome(boolean accepted, SkillId skillId, BigDecimal amount, String reason) {}
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final SkillId WOODCUTTING = SkillId.parse("bigbangskills:woodcutting");
    private final SkillRegistry registry;
    private final Map<UUID, PlayerProgress> players = new ConcurrentHashMap<>();
    private final XpService xp = new XpService();
    private final XpEligibilityService eligibility = new XpEligibilityService();

    public GameplayService(SkillRegistry registry) { this.registry = registry; }
    public Outcome blockBreak(BlockBreakAction action, BigDecimal miningXp, BigDecimal woodcuttingXp) {
        var decision = eligibility.check(action.realPlayer(), action.eventCancelled(), action.placed(), action.provenanceKnown());
        if (!decision.accepted()) return new Outcome(false, null, BigDecimal.ZERO, decision.reason());
        var skill = action.miningBlock() ? MINING : action.woodcuttingBlock() ? WOODCUTTING : null;
        var amount = action.miningBlock() ? miningXp : action.woodcuttingBlock() ? woodcuttingXp : BigDecimal.ZERO;
        if (skill == null) return new Outcome(false, null, BigDecimal.ZERO, "block_not_configured");
        var player = players.computeIfAbsent(action.playerId(), PlayerProgress::new);
        var request = new XpRequest(UUID.randomUUID(), action.playerId(), skill, amount, com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, action.blockId(), com.bigbangcraft.bigbangskills.api.ProgressionScope.server(action.worldId()), Instant.now());
        var result = xp.apply(player, request, registry, List.of());
        return new Outcome(result.accepted(), skill, result.amount(), result.reason());
    }
    public PlayerProgress progress(UUID playerId) { return players.get(playerId); }
    public Map<UUID, PlayerProgress> progressSnapshot() { return Map.copyOf(players); }
}
