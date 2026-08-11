package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.antiexploit.XpEligibilityService;
import com.bigbangcraft.bigbangskills.common.config.SkillXpTables;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.config.DiminishedReturnsConfig;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.xp.*;
import com.bigbangcraft.bigbangskills.api.XpSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.DoubleSupplier;

public final class GameplayService {
    public record Outcome(boolean accepted, SkillId skillId, BigDecimal amount, String reason, UUID requestId, com.bigbangcraft.bigbangskills.api.ProgressionScope scope, int previousLevel, int currentLevel, BlockBreakEffect blockEffect) {
        public Outcome(boolean accepted, SkillId skillId, BigDecimal amount, String reason, UUID requestId, com.bigbangcraft.bigbangskills.api.ProgressionScope scope, int previousLevel, int currentLevel) {
            this(accepted, skillId, amount, reason, requestId, scope, previousLevel, currentLevel, BlockBreakEffect.none());
        }
    }
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final SkillId WOODCUTTING = SkillId.parse("bigbangskills:woodcutting");
    private static final SkillId EXCAVATION = SkillId.parse("bigbangskills:excavation");
    private static final SkillId HERBALISM = SkillId.parse("bigbangskills:herbalism");
    private final SkillRegistry registry;
    private final SkillXpTables xpTables;
    private final SkillConfig config;
    private final SkillFormulaConfig formulas;
    private final DoubleSupplier randomUnit;
    private final DiminishedReturnsConfig diminishedReturns;
    // ponytail: weak player keys bound the optional throttle cache without a cleanup task.
    private final java.util.Map<UUID, java.util.Map<SkillId, RecentGain>> recentXp = new java.util.WeakHashMap<>();
    private final XpService xp = new XpService();
    private final XpEligibilityService eligibility = new XpEligibilityService();

    public GameplayService(SkillRegistry registry) { this(registry, SkillXpTables.defaults(), SkillConfig.defaults(), SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables) { this(registry, xpTables, SkillConfig.defaults(), SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillConfig config, SkillXpTables xpTables) { this(registry, xpTables, config, SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config) { this(registry, xpTables, config, SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config, SkillFormulaConfig formulas) { this(registry, xpTables, config, formulas, DiminishedReturnsConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config, SkillFormulaConfig formulas, DiminishedReturnsConfig diminishedReturns) { this(registry, xpTables, config, formulas, diminishedReturns, java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config, DoubleSupplier randomUnit) {
        this(registry, xpTables, config, SkillFormulaConfig.defaults(), randomUnit);
    }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config, SkillFormulaConfig formulas, DoubleSupplier randomUnit) {
        this(registry, xpTables, config, formulas, DiminishedReturnsConfig.defaults(), randomUnit);
    }
    public GameplayService(SkillRegistry registry, SkillXpTables xpTables, SkillConfig config, SkillFormulaConfig formulas, DiminishedReturnsConfig diminishedReturns, DoubleSupplier randomUnit) {
        this.registry = registry;
        this.xpTables = xpTables;
        this.config = config;
        this.formulas = formulas;
        this.randomUnit = java.util.Objects.requireNonNull(randomUnit);
        this.diminishedReturns = java.util.Objects.requireNonNull(diminishedReturns);
    }
    public Outcome blockBreak(PlayerProgress player, BlockBreakAction action, com.bigbangcraft.bigbangskills.api.ProgressionScope scope) {
        var skill = skillFor(action);
        var amount = skill == null ? BigDecimal.ZERO : xpTables.xpFor(skill, action.blockId());
        return blockBreak(player, action, amount, amount, scope);
    }

    public Outcome award(PlayerProgress player, SkillAwardAction action) {
        var decision = eligibility.check(action.realPlayer(), action.eventCancelled(), false, true);
        if (!decision.accepted()) return rejected(decision.reason());
        var configRejection = configurationRejection(action);
        if (configRejection != null) return rejected(configRejection);
        var rule = config.rule(action.skillId());
        var before = player.get(action.skillId());
        if (before == null) before = new com.bigbangcraft.bigbangskills.common.progression.SkillProgress(action.skillId(), BigDecimal.ZERO, 1, 0);
        var request = new XpRequest(UUID.randomUUID(), action.playerId(), action.skillId(), diminish(action.playerId(), action.skillId(), action.amount(), rule.xpMultiplier()), action.source(), action.reason(), action.scope(), Instant.now());
        var result = xp.apply(player, request, registry, xpModifiers(rule, action.pvp()));
        return new Outcome(result.accepted(), action.skillId(), result.amount(), result.reason(), result.accepted() ? request.requestId() : null,
                action.scope(), before.level(), result.after() == null ? before.level() : result.after().level(), BlockBreakEffect.none());
    }

    public Outcome award(PlayerProgress player, SkillId skill, String action, XpSource source, String reason,
                         com.bigbangcraft.bigbangskills.api.ProgressionScope scope, boolean pvp, boolean pve, UUID playerId) {
        return award(player, new SkillAwardAction(playerId, skill, xpTables.xpForAction(skill, action), source, reason, scope, true, false, pvp, pve));
    }
    public BigDecimal xpForAction(SkillId skill, String action) { return xpTables.xpForAction(skill, action); }
    public BigDecimal xpForBlock(SkillId skill, String blockId) { return xpTables.xpFor(skill, blockId); }
    public boolean hasBlockXp(SkillId skill, String blockId) { return xpForBlock(skill, blockId).signum() > 0; }
    public int woodcuttingBonusDropCopies(String blockId, int level, DoubleSupplier random) {
        return new WoodcuttingEngine().bonusDropCopies(level, xpTables.woodcuttingBonusDropsEnabled(blockId),
                unlocked(WOODCUTTING, "clean_cuts", level), unlocked(WOODCUTTING, "harvest_lumber", level),
                formulas.value("woodcutting.clean_cuts_max_percent"), (int) formulas.value("woodcutting.clean_cuts_max_level"),
                formulas.value("woodcutting.harvest_lumber_max_percent"), (int) formulas.value("woodcutting.harvest_lumber_max_level"), random);
    }
    public String configurationRejection(SkillAwardAction action) {
        var rule = config.rule(action.skillId());
        if (!rule.enabled()) return "skill_disabled";
        if (action.pvp() && !config.pvpRewards()) return "pvp_rewards_disabled";
        if (action.pvp() && !rule.pvp()) return "pvp_disabled";
        if (action.pve() && !rule.pve()) return "pve_disabled";
        return null;
    }
    public BigDecimal combatXp(String targetPath, boolean pvp) {
        if (pvp) return BigDecimal.valueOf(formulas.value("combat.pvp_base_xp"));
        var specific = xpTables.xpForAction(SkillId.parse("bigbangskills:combat"), "multiplier." + targetPath);
        if (specific.signum() == 0 && targetPath.contains(":")) {
            specific = xpTables.xpForAction(SkillId.parse("bigbangskills:combat"), "multiplier." + targetPath.substring(targetPath.indexOf(':') + 1));
        }
        if (specific.signum() > 0) return specific;
        var animals = xpTables.xpForAction(SkillId.parse("bigbangskills:combat"), "multiplier.animals");
        return animals.signum() > 0 ? animals : BigDecimal.ONE;
    }
    public Outcome blockBreak(PlayerProgress player, BlockBreakAction action, BigDecimal miningXp, BigDecimal woodcuttingXp, com.bigbangcraft.bigbangskills.api.ProgressionScope scope) {
        var decision = eligibility.check(action.realPlayer(), action.eventCancelled(), action.placed(), action.provenanceKnown());
        if (!decision.accepted()) return rejected(decision.reason());
        var skill = skillFor(action);
        var amount = action.miningBlock() ? miningXp : action.woodcuttingBlock() ? woodcuttingXp : xpTables.xpFor(skill == EXCAVATION ? EXCAVATION : HERBALISM, action.blockId());
        if (skill == null) return rejected("block_not_configured");
        if (skill == HERBALISM && action.insideVehicle() && formulas.value("herbalism.prevent_afk_leveling") > 0) return rejected("afk_leveling_disabled");
        if (!config.rule(skill).enabled()) return rejected("skill_disabled");
        if (!config.rule(skill).pve()) return rejected("pve_disabled");
        var request = new XpRequest(UUID.randomUUID(), action.playerId(), skill, diminish(action.playerId(), skill, amount, config.rule(skill).xpMultiplier()), com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, action.blockId(), scope, Instant.now());
        var result = xp.apply(player, request, registry, xpModifiers(config.rule(skill), false));
        var beforeLevel = result.before() == null ? 0 : result.before().level();
        return new Outcome(result.accepted(), skill, result.amount(), result.reason(), result.accepted() ? request.requestId() : null, scope, beforeLevel, result.after() == null ? 0 : result.after().level(), result.accepted() ? blockEffect(skill, action, beforeLevel) : BlockBreakEffect.none());
    }

    private static SkillId skillFor(BlockBreakAction action) {
        return action.miningBlock() ? MINING : action.woodcuttingBlock() ? WOODCUTTING : action.excavationBlock() ? EXCAVATION : action.herbalismBlock() ? HERBALISM : null;
    }

    private List<XpModifier> xpModifiers(SkillConfig.Rule rule, boolean pvp) {
        var modifiers = new java.util.ArrayList<XpModifier>();
        modifiers.add(new XpModifier("global", 10, config.globalXpMultiplier()));
        if (pvp) modifiers.add(new XpModifier("pvp", 20, config.pvpXpMultiplier()));
        modifiers.add(new XpModifier("skill_config", 100, rule.xpMultiplier()));
        return modifiers;
    }

    private BlockBreakEffect blockEffect(SkillId skill, BlockBreakAction action, int level) {
        var abilityDurabilityCost = formulas.value("abilities.durability_loss") > 0;
        if (skill.equals(EXCAVATION) && action.abilityActive() && unlocked(skill, "giga_drill_breaker", level)) {
            return new BlockBreakEffect(0, abilityDurabilityCost, 8, true);
        }
        if (skill.equals(WOODCUTTING) && action.abilityActive() && unlocked(skill, "tree_feller", level)) {
            return new BlockBreakEffect(0, abilityDurabilityCost, (int) formulas.value("woodcutting.tree_feller_max_blocks"), true, unlocked(skill, "leaf_blower", level));
        }
        if (skill.equals(MINING) && action.abilityActive() && unlocked(skill, "blast_mining", level)) {
            return BlockBreakEffect.none();
        }
        if (skill.equals(MINING) && action.abilityActive() && unlocked(skill, "super_breaker", level)) {
            return new BlockBreakEffect(1, abilityDurabilityCost);
        }
        if (skill.equals(HERBALISM) && action.abilityActive() && unlocked(skill, "green_terra", level)) {
            return new BlockBreakEffect(1, abilityDurabilityCost);
        }
        if (skill.equals(MINING)) {
            var silkTouchAllowed = formulas.value("mining.double_drops_silk_touch") > 0;
            var doubleDropsEnabled = xpTables.miningBonusDropsEnabled(action.blockId())
                    && unlocked(skill, "double_drops", level) && (!action.silkTouch() || silkTouchAllowed);
            var motherLode = doubleDropsEnabled && unlocked(skill, "mother_lode", level)
                    && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("mining.mother_lode_max_level"), formulas.value("mining.mother_lode_max_percent")), randomUnit);
            var doubleDrops = doubleDropsEnabled
                    && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("mining.double_drops_max_level"), formulas.value("mining.double_drops_max_percent")), randomUnit);
            if (motherLode) return new BlockBreakEffect(2, action.abilityActive());
            if (doubleDrops) {
                var triple = formulas.value("mining.super_breaker_allow_triple_drops") > 0 && action.abilityActive() && unlocked(skill, "super_breaker", level);
                return new BlockBreakEffect(triple ? 2 : 1, action.abilityActive());
            }
        } else if (skill.equals(WOODCUTTING) && xpTables.woodcuttingBonusDropsEnabled(action.blockId())) {
            if (unlocked(skill, "clean_cuts", level)
                    && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("woodcutting.clean_cuts_max_level"), formulas.value("woodcutting.clean_cuts_max_percent")), randomUnit)) {
                return new BlockBreakEffect(2, action.abilityActive());
            }
            if (unlocked(skill, "harvest_lumber", level)
                    && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("woodcutting.harvest_lumber_max_level"), formulas.value("woodcutting.harvest_lumber_max_percent")), randomUnit)) {
                return new BlockBreakEffect(1, action.abilityActive());
            }
        } else if (skill.equals(HERBALISM) && unlocked(skill, "verdant_bounty", level)
                && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("herbalism.verdant_bounty_max_level"), formulas.value("herbalism.verdant_bounty_max_percent")), randomUnit)) {
            return new BlockBreakEffect(2, action.abilityActive());
        } else if (skill.equals(HERBALISM) && unlocked(skill, "double_drops", level)
                && SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("herbalism.double_drops_max_level"), formulas.value("herbalism.double_drops_max_percent")), randomUnit)) {
            return new BlockBreakEffect(1, action.abilityActive());
        }
        return BlockBreakEffect.none();
    }

    private static boolean unlocked(SkillId skill, String ability, int level) {
        return DefaultAbilityCatalog.all().getOrDefault(skill, List.of()).stream()
                .anyMatch(definition -> definition.id().equals(skill.path() + "." + ability)
                        && level >= definition.unlockLevel());
    }

    private synchronized BigDecimal diminish(UUID playerId, SkillId skill, BigDecimal raw, BigDecimal multiplier) {
        var threshold = diminishedReturns.threshold(skill);
        if (!diminishedReturns.enabled() || threshold.signum() <= 0 || raw.signum() <= 0 || SkillRelationships.isChild(skill)) return raw;
        var now = Instant.now();
        var bySkill = recentXp.computeIfAbsent(playerId, ignored -> new java.util.HashMap<>());
        var previous = bySkill.get(skill);
        var registered = previous == null || previous.expires().isBefore(now) ? BigDecimal.ZERO : previous.amount();
        var adjustedThreshold = threshold.divide(multiplier.max(BigDecimal.ONE), 8, java.math.RoundingMode.DOWN);
        var difference = registered.subtract(adjustedThreshold).divide(adjustedThreshold, 8, java.math.RoundingMode.DOWN);
        var adjusted = difference.signum() <= 0 ? raw : raw.subtract(raw.multiply(difference));
        adjusted = adjusted.max(raw.multiply(diminishedReturns.guaranteedMinimumFraction())).max(BigDecimal.ZERO);
        bySkill.put(skill, new RecentGain(registered.add(adjusted), now.plusSeconds(diminishedReturns.intervalMinutes() * 60L)));
        return adjusted;
    }

    private record RecentGain(BigDecimal amount, Instant expires) {}

    private static Outcome rejected(String reason) {
        return new Outcome(false, null, BigDecimal.ZERO, reason, null, null, 0, 0, BlockBreakEffect.none());
    }
}
