package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.ability.AbilityDefinition;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

/** Validated per-skill settings shared by both loader adapters. */
public final class SkillConfig {
    private static final int DEFAULT_ABILITY_COOLDOWN_SECONDS = 240;
    /** Per-skill settings; abilitiesEnabled gates active ability activation, not passive effects. */
    public record Rule(boolean enabled, int levelCap, BigDecimal xpMultiplier, boolean pvp, boolean pve,
                       boolean abilitiesEnabled, int abilityCooldownSeconds, int abilityDurationSeconds) {
        public Rule {
            if (levelCap < 0 || xpMultiplier.signum() < 0 || abilityCooldownSeconds < 0 || abilityDurationSeconds < 0) {
                throw new IllegalArgumentException("Invalid skill config rule");
            }
        }
    }

    private final Map<SkillId, Rule> rules;
    private final String experienceCurve;
    private final int linearBase;
    private final int linearMultiplier;
    private final int exponentialBase;
    private final BigDecimal exponentialMultiplier;
    private final BigDecimal exponentialExponent;
    private final BigDecimal globalXpMultiplier;
    private final BigDecimal pvpXpMultiplier;
    private final boolean pvpRewards;
    private final boolean abilityOnlyWhenSneaking;
    private final boolean alchemyEnabledForHoppers;
    private final boolean alchemyPreventHopperIngredients;
    private final boolean alchemyPreventHopperBottles;
    private final boolean fishingDropsEnabled;
    private final boolean fishingOverrideVanillaTreasures;
    private final boolean fishingExtraFish;
    private final BigDecimal fishingLureModifier;
    private final boolean fishingAllowConflictingEnchants;
    private final Map<SkillId, Integer> abilityCooldownOverrides;

    private SkillConfig(Map<SkillId, Rule> rules, String experienceCurve, int linearBase, int linearMultiplier,
                        int exponentialBase, BigDecimal exponentialMultiplier, BigDecimal exponentialExponent,
                        BigDecimal globalXpMultiplier, BigDecimal pvpXpMultiplier, boolean pvpRewards,
                        boolean abilityOnlyWhenSneaking, boolean alchemyEnabledForHoppers,
                        boolean alchemyPreventHopperIngredients, boolean alchemyPreventHopperBottles,
                        boolean fishingDropsEnabled, boolean fishingOverrideVanillaTreasures, boolean fishingExtraFish,
                        BigDecimal fishingLureModifier, boolean fishingAllowConflictingEnchants,
                        Map<SkillId, Integer> abilityCooldownOverrides) {
        if ((!experienceCurve.equals("LINEAR") && !experienceCurve.equals("EXPONENTIAL")) || linearBase <= 0 || linearMultiplier <= 0
                || exponentialBase <= 0 || exponentialMultiplier.signum() <= 0 || exponentialExponent.signum() <= 0
                || globalXpMultiplier.signum() < 0 || pvpXpMultiplier.signum() < 0 || fishingLureModifier.signum() < 0) {
            throw new IllegalArgumentException("Invalid XP progression config");
        }
        this.rules = Map.copyOf(rules);
        this.experienceCurve = experienceCurve;
        this.linearBase = linearBase;
        this.linearMultiplier = linearMultiplier;
        this.exponentialBase = exponentialBase;
        this.exponentialMultiplier = exponentialMultiplier;
        this.exponentialExponent = exponentialExponent;
        this.globalXpMultiplier = globalXpMultiplier;
        this.pvpXpMultiplier = pvpXpMultiplier;
        this.pvpRewards = pvpRewards;
        this.abilityOnlyWhenSneaking = abilityOnlyWhenSneaking;
        this.alchemyEnabledForHoppers = alchemyEnabledForHoppers;
        this.alchemyPreventHopperIngredients = alchemyPreventHopperIngredients;
        this.alchemyPreventHopperBottles = alchemyPreventHopperBottles;
        this.fishingDropsEnabled = fishingDropsEnabled;
        this.fishingOverrideVanillaTreasures = fishingOverrideVanillaTreasures;
        this.fishingExtraFish = fishingExtraFish;
        this.fishingLureModifier = fishingLureModifier;
        this.fishingAllowConflictingEnchants = fishingAllowConflictingEnchants;
        this.abilityCooldownOverrides = Map.copyOf(abilityCooldownOverrides);
    }

    public static SkillConfig defaults() {
        var rules = new HashMap<SkillId, Rule>();
        for (var name : new String[]{
                "acrobatics", "alchemy", "archery", "axes", "crossbows", "excavation", "fishing",
                "herbalism", "maces", "mining", "repair", "salvage", "smelting", "spears", "swords",
                "taming", "tridents", "unarmed", "woodcutting"}) {
            rules.put(SkillId.parse("bigbangskills:" + name), new Rule(true, 0, BigDecimal.ONE, true, true, true, 240, 0));
        }
        return new SkillConfig(rules, "LINEAR", 1020, 20, 2000, new BigDecimal("0.1"), new BigDecimal("1.80"), BigDecimal.ONE, BigDecimal.ONE, true, false, true, false, false, true, true, false, new BigDecimal("4.0"), false, Map.of());
    }

    public static SkillConfig loadOrCreate(Path file) {
        var defaults = defaults();
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, defaults.serialize(), StandardCharsets.UTF_8);
                return defaults;
            }
            var mutable = new HashMap<>(defaults.rules);
            var legacyDefaultCaps = true;
            var legacyCaps = 0;
            var versioned = false;
            var experienceCurve = defaults.experienceCurve;
            var linearBase = defaults.linearBase;
            var linearMultiplier = defaults.linearMultiplier;
            var exponentialBase = defaults.exponentialBase;
            var exponentialMultiplier = defaults.exponentialMultiplier;
            var exponentialExponent = defaults.exponentialExponent;
            var hasCurve = false;
            var hasLinearBase = false;
            var hasLinearMultiplier = false;
            var hasExponentialBase = false;
            var hasExponentialMultiplier = false;
            var hasExponentialExponent = false;
            var globalXpMultiplier = defaults.globalXpMultiplier;
            var pvpXpMultiplier = defaults.pvpXpMultiplier;
            var pvpRewards = defaults.pvpRewards;
            var abilityOnlyWhenSneaking = defaults.abilityOnlyWhenSneaking;
            var alchemyEnabledForHoppers = defaults.alchemyEnabledForHoppers;
            var alchemyPreventHopperIngredients = defaults.alchemyPreventHopperIngredients;
            var alchemyPreventHopperBottles = defaults.alchemyPreventHopperBottles;
            var fishingDropsEnabled = defaults.fishingDropsEnabled;
            var fishingOverrideVanillaTreasures = defaults.fishingOverrideVanillaTreasures;
            var fishingExtraFish = defaults.fishingExtraFish;
            var fishingLureModifier = defaults.fishingLureModifier;
            var fishingAllowConflictingEnchants = defaults.fishingAllowConflictingEnchants;
            var hasAbilityOnlyWhenSneaking = false;
            var hasAlchemyEnabledForHoppers = false;
            var hasAlchemyPreventHopperIngredients = false;
            var hasAlchemyPreventHopperBottles = false;
            var hasFishingDropsEnabled = false;
            var hasFishingOverrideVanillaTreasures = false;
            var hasFishingExtraFish = false;
            var hasFishingLureModifier = false;
            var hasFishingAllowConflictingEnchants = false;
            var abilityCooldownOverrides = new HashMap<SkillId, Integer>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                if (separator <= 0) throw new IllegalArgumentException("Invalid skill config line: " + line);
                if (valueLine.startsWith("schema_version=")) { versioned = true; continue; }
                if (valueLine.matches("skill\\.[^.]+\\.level_cap=.+")) {
                    legacyCaps++;
                    legacyDefaultCaps &= Integer.parseInt(valueLine.substring(separator + 1).trim()) == 100;
                }
                var key = valueLine.substring(0, separator).trim();
                var value = valueLine.substring(separator + 1).trim();
                if (key.matches("skill\\.[^.]+\\.ability_cooldown_override_seconds")) {
                    var skill = SkillId.parse("bigbangskills:" + key.split("\\.", -1)[1]);
                    if (!mutable.containsKey(skill)) throw new IllegalArgumentException("Unknown skill: " + skill.path());
                    var seconds = Integer.parseInt(value);
                    if (seconds < 0) throw new IllegalArgumentException("Invalid ability cooldown override: " + value);
                    abilityCooldownOverrides.put(skill, seconds);
                    continue;
                }
                switch (key) {
                    case "experience.curve" -> { experienceCurve = curve(value); hasCurve = true; }
                    case "experience.linear_base" -> { linearBase = positiveInt(value, "experience.linear_base"); hasLinearBase = true; }
                    case "experience.linear_multiplier" -> { linearMultiplier = positiveInt(value, "experience.linear_multiplier"); hasLinearMultiplier = true; }
                    case "experience.exponential_base" -> { exponentialBase = positiveInt(value, "experience.exponential_base"); hasExponentialBase = true; }
                    case "experience.exponential_multiplier" -> { exponentialMultiplier = positiveDecimal(value, "experience.exponential_multiplier"); hasExponentialMultiplier = true; }
                    case "experience.exponential_exponent" -> { exponentialExponent = positiveDecimal(value, "experience.exponential_exponent"); hasExponentialExponent = true; }
                    case "experience.global_xp_multiplier" -> globalXpMultiplier = multiplier(value);
                    case "experience.pvp_xp_multiplier" -> pvpXpMultiplier = multiplier(value);
                    case "experience.pvp_rewards" -> pvpRewards = bool(value);
                    case "abilities.only_activate_when_sneaking" -> { abilityOnlyWhenSneaking = bool(value); hasAbilityOnlyWhenSneaking = true; }
                    case "alchemy.enabled_for_hoppers" -> { alchemyEnabledForHoppers = bool(value); hasAlchemyEnabledForHoppers = true; }
                    case "alchemy.prevent_hopper_transfer_ingredients" -> { alchemyPreventHopperIngredients = bool(value); hasAlchemyPreventHopperIngredients = true; }
                    case "alchemy.prevent_hopper_transfer_bottles" -> { alchemyPreventHopperBottles = bool(value); hasAlchemyPreventHopperBottles = true; }
                    case "fishing.drops_enabled" -> { fishingDropsEnabled = bool(value); hasFishingDropsEnabled = true; }
                    case "fishing.override_vanilla_treasures" -> { fishingOverrideVanillaTreasures = bool(value); hasFishingOverrideVanillaTreasures = true; }
                    case "fishing.extra_fish" -> { fishingExtraFish = bool(value); hasFishingExtraFish = true; }
                    case "fishing.lure_modifier" -> { fishingLureModifier = nonNegativeDecimal(value, "fishing.lure_modifier"); hasFishingLureModifier = true; }
                    case "fishing.allow_conflicting_enchants" -> { fishingAllowConflictingEnchants = bool(value); hasFishingAllowConflictingEnchants = true; }
                    default -> apply(mutable, key, value);
                }
            }
            if (!versioned && legacyCaps == mutable.size() && legacyDefaultCaps) {
                mutable.replaceAll((skill, rule) -> new Rule(rule.enabled(), 0, rule.xpMultiplier(), rule.pvp(), rule.pve(), rule.abilitiesEnabled(), rule.abilityCooldownSeconds(), rule.abilityDurationSeconds()));
                var migrated = new SkillConfig(mutable, experienceCurve, linearBase, linearMultiplier, exponentialBase, exponentialMultiplier, exponentialExponent, globalXpMultiplier, pvpXpMultiplier, pvpRewards, abilityOnlyWhenSneaking, alchemyEnabledForHoppers, alchemyPreventHopperIngredients, alchemyPreventHopperBottles, fishingDropsEnabled, fishingOverrideVanillaTreasures, fishingExtraFish, fishingLureModifier, fishingAllowConflictingEnchants, abilityCooldownOverrides);
                Files.writeString(file, migrated.serialize(), StandardCharsets.UTF_8);
                return migrated;
            }
            var loaded = new SkillConfig(mutable, experienceCurve, linearBase, linearMultiplier, exponentialBase, exponentialMultiplier, exponentialExponent, globalXpMultiplier, pvpXpMultiplier, pvpRewards, abilityOnlyWhenSneaking, alchemyEnabledForHoppers, alchemyPreventHopperIngredients, alchemyPreventHopperBottles, fishingDropsEnabled, fishingOverrideVanillaTreasures, fishingExtraFish, fishingLureModifier, fishingAllowConflictingEnchants, abilityCooldownOverrides);
            if (!hasCurve || !hasLinearBase || !hasLinearMultiplier || !hasExponentialBase || !hasExponentialMultiplier || !hasExponentialExponent || !hasAbilityOnlyWhenSneaking || !hasAlchemyEnabledForHoppers || !hasAlchemyPreventHopperIngredients || !hasAlchemyPreventHopperBottles || !hasFishingDropsEnabled || !hasFishingOverrideVanillaTreasures || !hasFishingExtraFish || !hasFishingLureModifier || !hasFishingAllowConflictingEnchants) {
                Files.writeString(file, loaded.serialize(), StandardCharsets.UTF_8);
            }
            return loaded;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load skill config: " + file, failure);
        }
    }

    public Rule rule(SkillId skill) { return rules.getOrDefault(skill, new Rule(true, 0, BigDecimal.ONE, true, true, true, 240, 0)); }
    public Map<SkillId, Rule> rules() { return rules; }
    public String experienceCurve() { return experienceCurve; }
    public int linearBase() { return linearBase; }
    public int linearMultiplier() { return linearMultiplier; }
    public int exponentialBase() { return exponentialBase; }
    public BigDecimal exponentialMultiplier() { return exponentialMultiplier; }
    public BigDecimal exponentialExponent() { return exponentialExponent; }
    public BigDecimal globalXpMultiplier() { return globalXpMultiplier; }
    public BigDecimal pvpXpMultiplier() { return pvpXpMultiplier; }
    public boolean pvpRewards() { return pvpRewards; }
    public boolean abilityOnlyWhenSneaking() { return abilityOnlyWhenSneaking; }
    public boolean alchemyEnabledForHoppers() { return alchemyEnabledForHoppers; }
    public boolean alchemyPreventHopperIngredients() { return alchemyPreventHopperIngredients; }
    public boolean alchemyPreventHopperBottles() { return alchemyPreventHopperBottles; }
    public boolean fishingDropsEnabled() { return fishingDropsEnabled; }
    public boolean fishingOverrideVanillaTreasures() { return fishingOverrideVanillaTreasures; }
    public boolean fishingExtraFish() { return fishingExtraFish; }
    public BigDecimal fishingLureModifier() { return fishingLureModifier; }
    public boolean fishingAllowConflictingEnchants() { return fishingAllowConflictingEnchants; }
    public boolean blocksAlchemyHopperTransfer(String itemId) {
        var bottle = itemId.equals("minecraft:potion") || itemId.equals("minecraft:splash_potion") || itemId.equals("minecraft:lingering_potion");
        return bottle ? alchemyPreventHopperBottles : alchemyPreventHopperIngredients;
    }
    public Map<SkillId, Integer> abilityCooldownOverrides() { return abilityCooldownOverrides; }

    public Duration abilityCooldown(AbilityDefinition ability) {
        var override = abilityCooldownOverrides.get(ability.skillId());
        if (override != null) return Duration.ofSeconds(override);
        var configured = rule(ability.skillId()).abilityCooldownSeconds();
        return Duration.ofSeconds(configured == DEFAULT_ABILITY_COOLDOWN_SECONDS
                ? ability.cooldown().getSeconds() : configured);
    }

    private static void apply(Map<SkillId, Rule> rules, String key, String value) {
        var parts = key.split("\\.", -1);
        if (parts.length != 3 || !parts[0].equals("skill")) throw new IllegalArgumentException("Unknown skill config key: " + key);
        var skill = SkillId.parse("bigbangskills:" + parts[1]);
        var current = rules.get(skill);
        if (current == null) throw new IllegalArgumentException("Unknown skill: " + parts[1]);
        var rule = switch (parts[2]) {
            case "enabled" -> new Rule(bool(value), current.levelCap(), current.xpMultiplier(), current.pvp(), current.pve(), current.abilitiesEnabled(), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "level_cap" -> new Rule(current.enabled(), Integer.parseInt(value), current.xpMultiplier(), current.pvp(), current.pve(), current.abilitiesEnabled(), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "xp_multiplier" -> new Rule(current.enabled(), current.levelCap(), new BigDecimal(value), current.pvp(), current.pve(), current.abilitiesEnabled(), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "pvp" -> new Rule(current.enabled(), current.levelCap(), current.xpMultiplier(), bool(value), current.pve(), current.abilitiesEnabled(), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "pve" -> new Rule(current.enabled(), current.levelCap(), current.xpMultiplier(), current.pvp(), bool(value), current.abilitiesEnabled(), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "abilities_enabled" -> new Rule(current.enabled(), current.levelCap(), current.xpMultiplier(), current.pvp(), current.pve(), bool(value), current.abilityCooldownSeconds(), current.abilityDurationSeconds());
            case "ability_cooldown_seconds" -> new Rule(current.enabled(), current.levelCap(), current.xpMultiplier(), current.pvp(), current.pve(), current.abilitiesEnabled(), Integer.parseInt(value), current.abilityDurationSeconds());
            case "ability_duration_seconds" -> new Rule(current.enabled(), current.levelCap(), current.xpMultiplier(), current.pvp(), current.pve(), current.abilitiesEnabled(), current.abilityCooldownSeconds(), Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Unknown skill config key: " + key);
        };
        rules.put(skill, rule);
    }

    private static boolean bool(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Expected boolean, got: " + value);
    }

    private static BigDecimal multiplier(String value) {
        var multiplier = new BigDecimal(value);
        if (multiplier.signum() < 0) throw new IllegalArgumentException("Expected non-negative multiplier, got: " + value);
        return multiplier;
    }

    private String serialize() {
        var output = new StringBuilder("# BigBangSkills skill settings; values are validated on startup.\nschema_version=6\n");
        output.append("experience.curve=").append(experienceCurve).append('\n');
        output.append("experience.linear_base=").append(linearBase).append('\n');
        output.append("experience.linear_multiplier=").append(linearMultiplier).append('\n');
        output.append("experience.exponential_base=").append(exponentialBase).append('\n');
        output.append("experience.exponential_multiplier=").append(exponentialMultiplier).append('\n');
        output.append("experience.exponential_exponent=").append(exponentialExponent).append('\n');
        output.append("experience.global_xp_multiplier=").append(globalXpMultiplier).append('\n');
        output.append("experience.pvp_xp_multiplier=").append(pvpXpMultiplier).append('\n');
        output.append("experience.pvp_rewards=").append(pvpRewards).append('\n');
        output.append("abilities.only_activate_when_sneaking=").append(abilityOnlyWhenSneaking).append('\n');
        output.append("alchemy.enabled_for_hoppers=").append(alchemyEnabledForHoppers).append('\n');
        output.append("alchemy.prevent_hopper_transfer_ingredients=").append(alchemyPreventHopperIngredients).append('\n');
        output.append("alchemy.prevent_hopper_transfer_bottles=").append(alchemyPreventHopperBottles).append('\n');
        output.append("fishing.drops_enabled=").append(fishingDropsEnabled).append('\n');
        output.append("fishing.override_vanilla_treasures=").append(fishingOverrideVanillaTreasures).append('\n');
        output.append("fishing.extra_fish=").append(fishingExtraFish).append('\n');
        output.append("fishing.lure_modifier=").append(fishingLureModifier).append('\n');
        output.append("fishing.allow_conflicting_enchants=").append(fishingAllowConflictingEnchants).append('\n');
        rules.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(SkillId::toString))).forEach(entry -> {
            var path = entry.getKey().path();
            var rule = entry.getValue();
            output.append("skill.").append(path).append(".enabled=").append(rule.enabled()).append('\n');
            output.append("skill.").append(path).append(".level_cap=").append(rule.levelCap()).append('\n');
            output.append("skill.").append(path).append(".xp_multiplier=").append(rule.xpMultiplier()).append('\n');
            output.append("skill.").append(path).append(".pvp=").append(rule.pvp()).append('\n');
            output.append("skill.").append(path).append(".pve=").append(rule.pve()).append('\n');
            output.append("skill.").append(path).append(".abilities_enabled=").append(rule.abilitiesEnabled()).append('\n');
            output.append("skill.").append(path).append(".ability_cooldown_seconds=").append(rule.abilityCooldownSeconds()).append('\n');
            output.append("skill.").append(path).append(".ability_duration_seconds=").append(rule.abilityDurationSeconds()).append('\n');
            var override = abilityCooldownOverrides.get(entry.getKey());
            if (override != null) output.append("skill.").append(path).append(".ability_cooldown_override_seconds=").append(override).append('\n');
        });
        return output.toString();
    }

    private static String curve(String value) {
        var normalized = value.toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("LINEAR") && !normalized.equals("EXPONENTIAL")) throw new IllegalArgumentException("Unsupported progression curve: " + value);
        return normalized;
    }

    private static int positiveInt(String value, String key) {
        var parsed = Integer.parseInt(value);
        if (parsed <= 0) throw new IllegalArgumentException(key + " must be positive");
        return parsed;
    }

    private static BigDecimal positiveDecimal(String value, String key) {
        var parsed = new BigDecimal(value);
        if (parsed.signum() <= 0) throw new IllegalArgumentException(key + " must be positive");
        return parsed;
    }

    private static BigDecimal nonNegativeDecimal(String value, String key) {
        var parsed = new BigDecimal(value);
        if (parsed.signum() < 0) throw new IllegalArgumentException(key + " must be non-negative");
        return parsed;
    }
}
