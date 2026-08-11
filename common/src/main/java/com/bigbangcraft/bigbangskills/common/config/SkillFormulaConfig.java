package com.bigbangcraft.bigbangskills.common.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Server-tunable proc/formula constants shared by both loaders. */
public final class SkillFormulaConfig {
    private final Map<String, Double> values;
    private final String salvageAnvilBlock;

    private SkillFormulaConfig(Map<String, Double> values, String salvageAnvilBlock) { this.values = Map.copyOf(values); this.salvageAnvilBlock = salvageAnvilBlock; }

    public static SkillFormulaConfig defaults() {
        var values = new HashMap<String, Double>();
        values.put("mining.mother_lode_max_percent", 50.0);
        values.put("salvage.confirmation_required", 1.0);
        values.put("mining.mother_lode_max_level", 1000.0);
        values.put("mining.double_drops_max_percent", 100.0);
        values.put("mining.double_drops_max_level", 100.0);
        values.put("mining.double_drops_silk_touch", 1.0);
        values.put("mining.super_breaker_allow_triple_drops", 1.0);
        values.put("mining.blast_bonus_drops_enabled", 1.0);
        values.put("mining.blast_bonus_drop_chance", 50.0);
        values.put("mining.blast_remote_detonation_distance", 100.0);
        values.put("woodcutting.clean_cuts_max_percent", 50.0);
        values.put("woodcutting.harvest_lumber_max_percent", 100.0);
        values.put("woodcutting.tree_feller_max_blocks", 1000.0);
        values.put("woodcutting.tree_feller_reduced_xp", 1.0);
        values.put("woodcutting.knock_on_wood_xp_orb_enabled", 1.0);
        values.put("herbalism.double_drops_max_percent", 100.0);
        values.put("herbalism.green_thumb_max_percent", 100.0);
        values.put("herbalism.verdant_bounty_max_percent", 50.0);
        values.put("herbalism.verdant_bounty_max_level", 1000.0);
        values.put("herbalism.hylian_luck_max_percent", 10.0);
        values.put("herbalism.shroom_thumb_max_percent", 50.0);
        values.put("combat.archery.skill_shot_percent_per_rank", 10.0);
        values.put("combat.limit_break_allow_pve", 0.0);
        values.put("combat.pvp_base_xp", 20.0);
        values.put("combat.archery.skill_shot_max_bonus", 9.0);
        values.put("combat.archery.daze_max_percent", 50.0);
        values.put("combat.archery.daze_bonus_damage", 4.0);
        values.put("combat.archery.arrow_retrieval_max_percent", 100.0);
        values.put("combat.archery.arrow_retrieval_max_level", 1000.0);
        values.put("combat.archery.distance_xp_multiplier", 0.025);
        values.put("combat.axes.critical_max_percent", 37.5);
        values.put("combat.axes.critical_pvp_multiplier", 1.5);
        values.put("combat.axes.critical_pve_multiplier", 2.0);
        values.put("combat.axes.axe_mastery_damage_per_rank", 1.0);
        values.put("combat.axes.greater_impact_percent", 25.0);
        values.put("combat.axes.greater_impact_bonus_damage", 2.0);
        values.put("combat.axes.greater_impact_knockback", 1.5);
        values.put("combat.axes.armor_impact_percent", 25.0);
        values.put("combat.axes.armor_damage_per_rank", 6.5);
        values.put("combat.axes.armor_impact_max_percent", 20.0);
        values.put("combat.crossbows.powered_shot_percent_per_rank", 10.0);
        values.put("combat.crossbows.powered_shot_max_bonus", 9.0);
        values.put("combat.maces.cripple_max_percent", 25.0);
        values.put("combat.maces.cripple_chance_rank_1", 10.0);
        values.put("combat.maces.cripple_chance_rank_2", 15.0);
        values.put("combat.maces.cripple_chance_rank_3", 20.0);
        values.put("combat.maces.cripple_chance_rank_4", 25.0);
        values.put("combat.maces.crush_base_damage", 0.5);
        values.put("combat.maces.crush_damage_per_rank", 1.0);
        values.put("combat.spears.momentum_max_percent", 50.0);
        values.put("combat.spears.mastery_damage_per_rank", 0.4);
        values.put("combat.swords.rupture_max_percent", 66.0);
        values.put("combat.swords.stab_base_damage", 1.0);
        values.put("combat.swords.stab_damage_per_rank", 1.5);
        values.put("combat.tridents.impale_base_damage", 1.0);
        values.put("combat.tridents.impale_damage_per_rank", 0.5);
        values.put("combat.unarmed.disarm_max_percent", 33.0);
        values.put("combat.unarmed.arrow_deflect_max_percent", 50.0);
        values.put("combat.unarmed.berserk_multiplier", 1.5);
        values.put("combat.unarmed.iron_grip_max_percent", 100.0);
        values.put("combat.swords.counter_attack_max_percent", 30.0);
        values.put("combat.swords.counter_attack_damage_divisor", 2.0);
        values.put("combat.swords.rupture_chance_rank_1", 15.0);
        values.put("combat.swords.rupture_chance_rank_2", 33.0);
        values.put("combat.swords.rupture_chance_rank_3", 40.0);
        values.put("combat.swords.rupture_chance_rank_4", 66.0);
        values.put("combat.swords.rupture_pvp_tick_rank_1", 0.1);
        values.put("combat.swords.rupture_pvp_tick_rank_2", 0.15);
        values.put("combat.swords.rupture_pvp_tick_rank_3", 0.2);
        values.put("combat.swords.rupture_pvp_tick_rank_4", 0.3);
        values.put("combat.swords.rupture_pve_tick_rank_1", 0.5);
        values.put("combat.swords.rupture_pve_tick_rank_2", 0.75);
        values.put("combat.swords.rupture_pve_tick_rank_3", 0.9);
        values.put("combat.swords.rupture_pve_tick_rank_4", 1.0);
        values.put("combat.taming.gore_max_percent", 100.0);
        values.put("combat.taming.gore_multiplier", 2.0);
        values.put("taming.fast_food_chance", 50.0);
        values.put("taming.pummel_chance", 10.0);
        values.put("taming.sharpened_claws_bonus", 2.0);
        values.put("taming.thick_fur_divisor", 2.0);
        values.put("taming.shock_proof_divisor", 6.0);
        values.put("fishing.exploit_move_range", 3.0);
        values.put("fishing.exploit_over_fish_limit", 10.0);
        values.put("fishing.master_angler_min_wait_per_rank", 10.0);
        values.put("fishing.master_angler_max_wait_per_rank", 30.0);
        values.put("fishing.master_angler_boat_min_wait", 10.0);
        values.put("fishing.master_angler_boat_max_wait", 30.0);
        values.put("fishing.master_angler_lure_wait", 100.0);
        values.put("fishing.master_angler_min_wait_cap", 40.0);
        values.put("fishing.master_angler_max_wait_cap", 100.0);
        values.put("fishing.shake_chance_rank_1", 15.0);
        values.put("fishing.shake_chance_rank_2", 20.0);
        values.put("fishing.shake_chance_rank_3", 25.0);
        values.put("fishing.shake_chance_rank_4", 35.0);
        values.put("fishing.shake_chance_rank_5", 45.0);
        values.put("fishing.shake_chance_rank_6", 55.0);
        values.put("fishing.shake_chance_rank_7", 65.0);
        values.put("fishing.shake_chance_rank_8", 75.0);
        values.put("fishing.vanilla_xp_multiplier_rank_1", 1.0);
        values.put("fishing.vanilla_xp_multiplier_rank_2", 2.0);
        values.put("fishing.vanilla_xp_multiplier_rank_3", 3.0);
        values.put("fishing.vanilla_xp_multiplier_rank_4", 3.0);
        values.put("fishing.vanilla_xp_multiplier_rank_5", 4.0);
        values.put("fishing.vanilla_xp_multiplier_rank_6", 4.0);
        values.put("fishing.vanilla_xp_multiplier_rank_7", 5.0);
        values.put("fishing.vanilla_xp_multiplier_rank_8", 5.0);
        values.put("acrobatics.roll_chance_max", 100.0);
        values.put("acrobatics.roll_max_level", 100.0);
        values.put("acrobatics.roll_damage_threshold", 7.0);
        values.put("acrobatics.graceful_roll_damage_threshold", 14.0);
        values.put("acrobatics.dodge_chance_max", 20.0);
        values.put("acrobatics.dodge_max_level", 100.0);
        values.put("acrobatics.dodge_damage_divisor", 2.0);
        values.put("smelting.second_smelt_max_percent", 50.0);
        values.put("smelting.second_smelt_max_level", 100.0);
        values.put("alchemy.catalysis_min_speed", 1.0);
        values.put("alchemy.catalysis_max_speed", 4.0);
        values.put("alchemy.catalysis_max_level", 100.0);
        values.put("repair.mastery_max_percent", 200.0);
        values.put("repair.mastery_max_level", 100.0);
        values.put("repair.super_repair_max_percent", 100.0);
        values.put("repair.arcane_forging_keep_rank_1", 10.0);
        values.put("repair.arcane_forging_keep_rank_2", 20.0);
        values.put("repair.arcane_forging_keep_rank_3", 30.0);
        values.put("repair.arcane_forging_keep_rank_4", 40.0);
        values.put("repair.arcane_forging_keep_rank_5", 50.0);
        values.put("repair.arcane_forging_keep_rank_6", 50.0);
        values.put("repair.arcane_forging_keep_rank_7", 60.0);
        values.put("repair.arcane_forging_keep_rank_8", 60.0);
        values.put("repair.arcane_forging_downgrade_rank_1", 75.0);
        values.put("repair.arcane_forging_downgrade_rank_2", 50.0);
        values.put("repair.arcane_forging_downgrade_rank_3", 40.0);
        values.put("repair.arcane_forging_downgrade_rank_4", 30.0);
        values.put("repair.arcane_forging_downgrade_rank_5", 25.0);
        values.put("repair.arcane_forging_downgrade_rank_6", 20.0);
        values.put("repair.arcane_forging_downgrade_rank_7", 15.0);
        values.put("repair.arcane_forging_downgrade_rank_8", 10.0);
        values.put("salvage.arcane_salvage_enchant_loss_enabled", 1.0);
        values.put("salvage.arcane_salvage_downgrade_enabled", 1.0);
        values.put("salvage.arcane_salvage_max_enchant", 5.0);
        values.put("salvage.arcane_salvage_full_rank_1", 2.5);
        values.put("salvage.arcane_salvage_full_rank_2", 5.0);
        values.put("salvage.arcane_salvage_full_rank_3", 7.5);
        values.put("salvage.arcane_salvage_full_rank_4", 10.0);
        values.put("salvage.arcane_salvage_full_rank_5", 12.5);
        values.put("salvage.arcane_salvage_full_rank_6", 17.5);
        values.put("salvage.arcane_salvage_full_rank_7", 25.0);
        values.put("salvage.arcane_salvage_full_rank_8", 32.5);
        values.put("salvage.arcane_salvage_partial_rank_1", 2.0);
        values.put("salvage.arcane_salvage_partial_rank_2", 2.5);
        values.put("salvage.arcane_salvage_partial_rank_3", 5.0);
        values.put("salvage.arcane_salvage_partial_rank_4", 7.5);
        values.put("salvage.arcane_salvage_partial_rank_5", 10.0);
        values.put("salvage.arcane_salvage_partial_rank_6", 12.5);
        values.put("salvage.arcane_salvage_partial_rank_7", 15.0);
        values.put("salvage.arcane_salvage_partial_rank_8", 17.5);
        return new SkillFormulaConfig(values, "minecraft:gold_block");
    }

    public static SkillFormulaConfig loadOrCreate(Path file) {
        var defaults = defaults();
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, defaults.serialize(), StandardCharsets.UTF_8);
                return defaults;
            }
            var values = new HashMap<>(defaults.values);
            var salvageAnvilBlock = defaults.salvageAnvilBlock;
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                if (separator <= 0 || separator == valueLine.length() - 1) throw new IllegalArgumentException("Invalid formula config line: " + line);
                var key = valueLine.substring(0, separator).trim();
                if (key.equals("salvage.anvil_block")) {
                    salvageAnvilBlock = valueLine.substring(separator + 1).trim();
                    if (!salvageAnvilBlock.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid salvage anvil block: " + salvageAnvilBlock);
                    continue;
                }
                var value = new BigDecimal(valueLine.substring(separator + 1).trim()).doubleValue();
                if (!defaults.values.containsKey(key) || !Double.isFinite(value) || value < 0
                        || (key.endsWith("_divisor") && value <= 0)
                        || ((key.startsWith("fishing.exploit_") || key.endsWith("_max_blocks") || key.equals("mining.blast_remote_detonation_distance")) && value != Math.rint(value))) throw new IllegalArgumentException("Unknown or invalid formula: " + key);
                values.put(key, value);
            }
            return new SkillFormulaConfig(values, salvageAnvilBlock);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load formula config: " + file, failure);
        }
    }

    public double value(String key) { return values.getOrDefault(key, 0.0); }
    public String salvageAnvilBlock() { return salvageAnvilBlock; }
    public Map<String, Double> values() { return values; }

    private String serialize() {
        var output = new StringBuilder("# BigBangSkills formula/proc constants; values are validated on startup.\n");
        output.append("salvage.anvil_block=").append(salvageAnvilBlock).append('\n');
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> output.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));
        return output.toString();
    }
}
