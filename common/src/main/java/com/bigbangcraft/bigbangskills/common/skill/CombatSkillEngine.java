package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.api.XpSource;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/** One deterministic combat pipeline for all weapon skills. */
public final class CombatSkillEngine {
    private static final SkillId ARCHERY = id("archery");
    private static final SkillId AXES = id("axes");
    private static final SkillId CROSSBOWS = id("crossbows");
    private static final SkillId MACES = id("maces");
    private static final SkillId SPEARS = id("spears");
    private static final SkillId SWORDS = id("swords");
    private static final SkillId TRIDENTS = id("tridents");
    private static final SkillId UNARMED = id("unarmed");
    private static final SkillId TAMING = id("taming");
    private final DoubleSupplier randomUnit;
    private final SkillFormulaConfig formulas;
    private final SkillConfig skillConfig;

    public CombatSkillEngine() { this(SkillFormulaConfig.defaults(), SkillConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public CombatSkillEngine(DoubleSupplier randomUnit) { this(SkillFormulaConfig.defaults(), SkillConfig.defaults(), randomUnit); }
    public CombatSkillEngine(SkillFormulaConfig formulas) { this(formulas, SkillConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public CombatSkillEngine(SkillFormulaConfig formulas, DoubleSupplier randomUnit) { this(formulas, SkillConfig.defaults(), randomUnit); }
    public CombatSkillEngine(SkillFormulaConfig formulas, SkillConfig skillConfig) { this(formulas, skillConfig, java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public CombatSkillEngine(SkillFormulaConfig formulas, SkillConfig skillConfig, DoubleSupplier randomUnit) { this.formulas = java.util.Objects.requireNonNull(formulas); this.skillConfig = java.util.Objects.requireNonNull(skillConfig); this.randomUnit = java.util.Objects.requireNonNull(randomUnit); }

    public CombatResolution resolve(PlayerProgress progress, CombatAction action) {
        var state = progress.get(action.skillId());
        var level = state == null ? 1 : state.level();
        var rule = skillConfig.rule(action.skillId());
        var allowed = rule.enabled() && (action.pvp() ? rule.pvp() : rule.pve());
        var effect = !allowed ? CombatEffect.none() : switch (action.skillId().path()) {
            case "archery" -> archery(action, level);
            case "axes" -> axes(action, level);
            case "crossbows" -> powered(action, "powered_shot", level);
            case "maces" -> maces(action, level);
            case "spears" -> spears(action, level);
            case "swords" -> swords(action, level);
            case "tridents" -> tridents(action, level);
            case "unarmed" -> unarmed(action, level);
            case "taming" -> taming(action, level);
            default -> CombatEffect.none();
        };
        var limitBreak = allowed ? limitBreakBonus(action, level) : 0;
        if (limitBreak > 0) effect = effect.withBonusDamage(effect.bonusDamage() + limitBreak);
        var award = new SkillAwardAction(action.playerId(), action.skillId(), allowed ? action.baseXp() : BigDecimal.ZERO, XpSource.INTEGRATION,
                "combat_hit", action.scope(), true, false, action.pvp(), !action.pvp());
        return new CombatResolution(award, effect);
    }

    public static boolean secondaryTargetAllowed(boolean player, boolean pvpAllowed, boolean spectator,
                                                 boolean ownedByAttacker) {
        return !ownedByAttacker && (!player || (pvpAllowed && !spectator));
    }

    public boolean arrowDeflect(PlayerProgress progress) {
        var level = level(progress, UNARMED);
        return unlocked(UNARMED, "arrow_deflect", level)
                && succeeds(linear(level, (int) formulas.value("combat.unarmed.arrow_deflect_max_level"), formulas.value("combat.unarmed.arrow_deflect_max_percent")));
    }

    public boolean arrowRetrieval(PlayerProgress progress) {
        var level = level(progress, ARCHERY);
        return unlocked(ARCHERY, "arrow_retrieval", level)
                && succeeds(SkillChance.linearPercent(level, (int) formulas.value("combat.archery.arrow_retrieval_max_level"),
                formulas.value("combat.archery.arrow_retrieval_max_percent")));
    }

    public int trickShotBounces(PlayerProgress progress) {
        var level = level(progress, CROSSBOWS);
        return unlocked(CROSSBOWS, "trick_shot", level) ? rank(CROSSBOWS, "trick_shot", level) : 0;
    }

    public double archeryDistanceXpMultiplier(double distance) {
        return 1.0 + Math.min(Math.max(0.0, distance), 50.0) * formulas.value("combat.archery.distance_xp_multiplier");
    }

    public boolean ironGrip(PlayerProgress progress) {
        var level = level(progress, UNARMED);
        return unlocked(UNARMED, "iron_grip", level)
                && succeeds(linear(level, (int) formulas.value("combat.unarmed.iron_grip_max_level"), formulas.value("combat.unarmed.iron_grip_max_percent")));
    }

    public double counterAttackDamage(PlayerProgress progress, double damage) {
        if (damage <= 0) return 0;
        var level = level(progress, SWORDS);
        return unlocked(SWORDS, "counter_attack", level)
                && succeeds(linear(level, (int) formulas.value("combat.swords.counter_attack_max_level"), formulas.value("combat.swords.counter_attack_max_percent")))
                ? damage / formulas.value("combat.swords.counter_attack_damage_divisor") : 0;
    }

    private static int level(PlayerProgress progress, SkillId skill) {
        var state = progress.get(skill);
        return state == null ? 1 : state.level();
    }

    private CombatEffect archery(CombatAction action, int level) {
        var rank = rank(ARCHERY, "skill_shot", level);
        var bonus = unlocked(ARCHERY, "skill_shot", level) ? Math.min(formulas.value("combat.archery.skill_shot_max_bonus"), action.damage() * rank * formulas.value("combat.archery.skill_shot_percent_per_rank") / 100) : 0;
        var daze = unlocked(ARCHERY, "daze", level) && action.pvp()
                && succeeds(linear(level, (int) formulas.value("combat.archery.daze_max_level"), formulas.value("combat.archery.daze_max_percent")) * Math.min(1, action.attackStrength()));
        return new CombatEffect(1, bonus + (daze ? formulas.value("combat.archery.daze_bonus_damage") : 0), 0, daze, false, false, false, 0,
                false, false, 200, 10, false, false, 0, 0, false);
    }

    private CombatEffect axes(CombatAction action, int level) {
        var rank = rank(AXES, "axe_mastery", level);
        var bonus = unlocked(AXES, "axe_mastery", level) ? rank * formulas.value("combat.axes.axe_mastery_damage_per_rank") * Math.min(1, action.attackStrength()) : 0;
        var strength = Math.min(1, action.attackStrength());
        var critical = unlocked(AXES, "critical_strikes", level) && succeeds(linear(level, (int) formulas.value("combat.axes.critical_max_level"), formulas.value("combat.axes.critical_max_percent")) * strength);
        var multiplier = critical ? (action.pvp() ? formulas.value("combat.axes.critical_pvp_multiplier") : formulas.value("combat.axes.critical_pve_multiplier")) : 1;
        var greater = unlocked(AXES, "greater_impact", level)
                && succeeds(formulas.value("combat.axes.greater_impact_percent") * strength);
        var aoe = action.abilityActive() && unlocked(AXES, "skull_splitter", level) ? action.damage() / 2 * strength : 0;
        var armorImpact = unlocked(AXES, "armor_impact", level) && action.targetHasArmor()
                && succeeds(formulas.value("combat.axes.armor_impact_percent") * strength);
        return new CombatEffect(multiplier, bonus + (greater ? formulas.value("combat.axes.greater_impact_bonus_damage") * strength : 0), aoe, false, false, false, false,
                armorImpact ? Math.max(1, (int) (rank * formulas.value("combat.axes.armor_damage_per_rank"))) : 0,
                false, false, 40, 0, false, false, 0, 0, greater);
    }

    private CombatEffect powered(CombatAction action, String ability, int level) {
        var rank = rank(CROSSBOWS, ability, level);
        var bonus = unlocked(CROSSBOWS, ability, level) ? Math.min(formulas.value("combat.crossbows.powered_shot_max_bonus"), action.damage() * rank * formulas.value("combat.crossbows.powered_shot_percent_per_rank") / 100) : 0;
        return new CombatEffect(1, bonus, 0, false, false, false, false, 0);
    }

    private CombatEffect maces(CombatAction action, int level) {
        var rank = rank(MACES, "crush", level);
        var bonus = unlocked(MACES, "crush", level) ? formulas.value("combat.maces.crush_base_damage") + rank * formulas.value("combat.maces.crush_damage_per_rank") : 0;
        var crippleRank = rank(MACES, "cripple", level);
        var cripple = unlocked(MACES, "cripple", level)
                && succeeds(Math.min(formulas.value("combat.maces.cripple_max_percent"), formulas.value("combat.maces.cripple_chance_rank_" + Math.min(4, crippleRank))) * Math.min(1, action.attackStrength()));
        return new CombatEffect(1, bonus, 0, false, false, false, false, 0, cripple, false, action.pvp() ? 20 : 30, action.pvp() ? 1 : 2);
    }

    private CombatEffect spears(CombatAction action, int level) {
        var rank = rank(SPEARS, "spear_mastery", level);
        var bonus = unlocked(SPEARS, "spear_mastery", level) ? rank * formulas.value("combat.spears.mastery_damage_per_rank") * Math.min(1, action.attackStrength()) : 0;
        var momentumRank = rank(SPEARS, "momentum", level);
        var momentum = unlocked(SPEARS, "momentum", level)
                && succeeds(Math.min(formulas.value("combat.spears.momentum_max_percent"), momentumRank * 5) * Math.min(1, action.attackStrength()));
        return new CombatEffect(1, bonus, 0, false, false, false, false, 0, false, momentum, Math.max(0, momentumRank * 40), 2);
    }

    private CombatEffect swords(CombatAction action, int level) {
        var rank = rank(SWORDS, "stab", level);
        var strength = Math.min(1, action.attackStrength());
        var bonus = unlocked(SWORDS, "stab", level) ? (formulas.value("combat.swords.stab_base_damage") + rank * formulas.value("combat.swords.stab_damage_per_rank")) * strength : 0;
        var ruptureRank = rank(SWORDS, "rupture", level);
        var ruptureChance = formulas.value("combat.swords.rupture_chance_rank_" + Math.min(4, ruptureRank)) * strength;
        var rupture = unlocked(SWORDS, "rupture", level) && succeeds(ruptureChance);
        var aoe = action.abilityActive() && unlocked(SWORDS, "serrated_strikes", level) ? action.damage() / 4 * strength : 0;
        var tickPrefix = action.pvp() ? "combat.swords.rupture_pvp_tick_rank_" : "combat.swords.rupture_pve_tick_rank_";
        var tickDamage = formulas.value(tickPrefix + Math.min(4, ruptureRank));
        var ruptureDuration = (int) formulas.value(action.pvp()
                ? "combat.swords.rupture_duration_ticks_pvp" : "combat.swords.rupture_duration_ticks_pve");
        return new CombatEffect(1, bonus, aoe, false, rupture, false, false, 0, false, false,
                100, 0, false, false, rupture ? tickDamage : 0, rupture ? ruptureDuration : 0, false);
    }

    private CombatEffect tridents(CombatAction action, int level) {
        var rank = rank(TRIDENTS, "impale", level);
        var bonus = unlocked(TRIDENTS, "impale", level) ? (formulas.value("combat.tridents.impale_base_damage") + rank * formulas.value("combat.tridents.impale_damage_per_rank")) * Math.min(1, action.attackStrength()) : 0;
        return new CombatEffect(1, bonus, 0, false, false, false, false, 0);
    }

    private CombatEffect unarmed(CombatAction action, int level) {
        var rank = rank(UNARMED, "steel_arm_style", level);
        var bonus = unlocked(UNARMED, "steel_arm_style", level) ? steelArmDamage(rank) : 0;
        var multiplier = action.abilityActive() && unlocked(UNARMED, "berserk", level) ? formulas.value("combat.unarmed.berserk_multiplier") * Math.min(1, action.attackStrength()) : 1;
        var disarm = unlocked(UNARMED, "disarm", level) && action.pvp()
                && succeeds(linear(level, 100, formulas.value("combat.unarmed.disarm_max_percent")) * Math.min(1, action.attackStrength()));
        return new CombatEffect(multiplier, bonus, 0, false, false, disarm, false, 0);
    }

    private double steelArmDamage(int rank) {
        if (formulas.value("combat.unarmed.steel_arm_damage_override") > 0) {
            return formulas.value("combat.unarmed.steel_arm_override_rank_" + Math.max(1, Math.min(20, rank)));
        }
        return .5 + rank / 2.0 + Math.max(0, rank - 17);
    }

    private CombatEffect taming(CombatAction action, int level) {
        var gore = unlocked(TAMING, "gore", level) && succeeds(linear(level, (int) formulas.value("combat.taming.gore_max_level"), formulas.value("combat.taming.gore_max_percent")));
        var claws = unlocked(TAMING, "sharpened_claws", level);
        var fastFood = unlocked(TAMING, "fast_food_service", level) && succeeds(formulas.value("taming.fast_food_chance"));
        var pummel = unlocked(TAMING, "pummel", level) && succeeds(formulas.value("taming.pummel_chance"));
        return new CombatEffect(gore ? formulas.value("combat.taming.gore_multiplier") : 1,
                claws ? formulas.value("taming.sharpened_claws_bonus") : 0, 0, false, false, false, false, 0,
                false, false, 40, 0, fastFood, pummel);
    }

    private boolean unlocked(SkillId skill, String ability, int level) {
        return DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .anyMatch(definition -> definition.id().equals(skill.path() + "." + ability) && level >= definition.unlockLevel());
    }
    private double limitBreakBonus(CombatAction action, int level) {
        if (!action.pvp() && formulas.value("combat.limit_break_allow_pve") <= 0) return 0;
        var skill = action.skillId();
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(value -> value.id().equals(skill.path() + "." + skill.path() + "_limit_break"))
                .findFirst().orElse(null);
        if (ability == null || level < ability.unlockLevel()) return 0;
        var raw = ability.rankForLevel(level);
        var quality = action.pvp() ? action.targetArmorQuality() : 1000;
        var multiplier = quality <= 4 ? .25 : quality <= 8 ? .50 : quality <= 12 ? .75 : 1.0;
        return (int) (raw * multiplier) * action.attackStrength();
    }
    private static int rank(SkillId skill, String ability, int level) {
        return DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(definition -> definition.id().equals(skill.path() + "." + ability))
                .findFirst().map(definition -> definition.rankForLevel(level)).orElse(0);
    }
    private boolean succeeds(double percent) { return SkillChance.succeeds(percent, randomUnit); }
    private static double linear(int level, int cap, double max) { return SkillChance.linearPercent(level, cap, max); }
    private static SkillId id(String path) { return SkillId.parse("bigbangskills:" + path); }
}
