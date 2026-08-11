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
    private final String repairAnvilBlock;
    private final String salvageAnvilBlock;
    private final String miningDetonatorItem;

    private SkillFormulaConfig(Map<String, Double> values, String repairAnvilBlock, String salvageAnvilBlock, String miningDetonatorItem) { this.values = Map.copyOf(values); this.repairAnvilBlock = repairAnvilBlock; this.salvageAnvilBlock = salvageAnvilBlock; this.miningDetonatorItem = miningDetonatorItem; }

    public static SkillFormulaConfig defaults() {
        var values = new HashMap<String, Double>();
        values.put("mining.mother_lode_max_percent", 50.0);
        values.put("abilities.durability_loss", 1.0);
        values.put("abilities.duration_cap_level", 50.0);
        values.put("abilities.duration_increase_level", 5.0);
        values.put("salvage.confirmation_required", 1.0);
        values.put("repair.confirmation_required", 1.0);
        values.put("mining.mother_lode_max_level", 1000.0);
        values.put("mining.double_drops_max_percent", 100.0);
        values.put("mining.double_drops_max_level", 100.0);
        values.put("mining.double_drops_silk_touch", 1.0);
        values.put("mining.super_breaker_allow_triple_drops", 1.0);
        values.put("mining.blast_bonus_drops_enabled", 1.0);
        values.put("mining.blast_bonus_drop_chance", 50.0);
        values.put("mining.blast_base_radius", 4.0);
        var blastRadius = new double[]{1, 1, 2, 2, 3, 3, 4, 4};
        var blastDamage = new double[]{0, 0, 0, 25, 25, 50, 50, 100};
        var blastOre = new double[]{35, 40, 45, 50, 55, 60, 65, 70};
        for (var i = 1; i <= 8; i++) {
            values.put("mining.blast_radius_bonus_rank_" + i, blastRadius[i - 1]);
            values.put("mining.blast_damage_reduction_rank_" + i, blastDamage[i - 1]);
            values.put("mining.blast_ore_bonus_rank_" + i, blastOre[i - 1]);
        }
        values.put("mining.blast_remote_detonation_distance", 100.0);
        values.put("woodcutting.clean_cuts_max_percent", 50.0);
        values.put("woodcutting.clean_cuts_max_level", 1000.0);
        values.put("woodcutting.harvest_lumber_max_percent", 100.0);
        values.put("woodcutting.harvest_lumber_max_level", 100.0);
        values.put("woodcutting.tree_feller_max_blocks", 1000.0);
        values.put("woodcutting.tree_feller_reduced_xp", 1.0);
        values.put("woodcutting.knock_on_wood_xp_orb_enabled", 1.0);
        values.put("herbalism.double_drops_max_percent", 100.0);
        values.put("herbalism.double_drops_max_level", 100.0);
        values.put("herbalism.green_thumb_max_percent", 100.0);
        values.put("herbalism.green_thumb_max_level", 100.0);
        values.put("herbalism.verdant_bounty_max_percent", 50.0);
        values.put("herbalism.verdant_bounty_max_level", 1000.0);
        values.put("herbalism.hylian_luck_max_percent", 10.0);
        values.put("herbalism.hylian_luck_max_level", 100.0);
        values.put("herbalism.shroom_thumb_max_percent", 50.0);
        values.put("herbalism.shroom_thumb_max_level", 100.0);
        values.put("herbalism.prevent_afk_leveling", 1.0);
        values.put("combat.archery.skill_shot_percent_per_rank", 10.0);
        values.put("combat.limit_break_allow_pve", 0.0);
        values.put("combat.pvp_base_xp", 20.0);
        values.put("combat.tamed_mob_xp_multiplier", 0.0);
        values.put("combat.spawner_mob_xp_multiplier", 0.0);
        values.put("combat.egg_mob_xp_multiplier", 0.0);
        values.put("combat.bred_mob_xp_multiplier", 1.0);
        values.put("combat.xp_ceiling_enabled", 1.0);
        values.put("combat.xp_damage_ceiling", 100.0);
        values.put("combat.archery.skill_shot_max_bonus", 9.0);
        values.put("combat.archery.daze_max_percent", 50.0);
        values.put("combat.archery.daze_max_level", 100.0);
        values.put("combat.archery.daze_bonus_damage", 4.0);
        values.put("combat.archery.arrow_retrieval_max_percent", 100.0);
        values.put("combat.archery.arrow_retrieval_max_level", 100.0);
        values.put("combat.archery.distance_xp_multiplier", 0.025);
        values.put("combat.archery.force_multiplier", 2.0);
        values.put("combat.axes.critical_max_percent", 37.5);
        values.put("combat.axes.critical_max_level", 100.0);
        values.put("combat.axes.critical_pvp_multiplier", 1.5);
        values.put("combat.axes.critical_pve_multiplier", 2.0);
        values.put("combat.axes.axe_mastery_damage_per_rank", 1.0);
        values.put("combat.axes.greater_impact_percent", 25.0);
        values.put("combat.axes.greater_impact_bonus_damage", 2.0);
        values.put("combat.axes.greater_impact_knockback", 1.5);
        values.put("combat.axes.armor_impact_percent", 25.0);
        values.put("combat.axes.armor_damage_per_rank", 6.5);
        values.put("combat.axes.armor_impact_max_percent", 20.0);
        values.put("combat.axes.skull_splitter_damage_divisor", 2.0);
        values.put("combat.crossbows.powered_shot_percent_per_rank", 10.0);
        values.put("combat.crossbows.powered_shot_max_bonus", 9.0);
        values.put("combat.maces.cripple_max_percent", 33.0);
        values.put("combat.maces.cripple_chance_rank_1", 10.0);
        values.put("combat.maces.cripple_chance_rank_2", 15.0);
        values.put("combat.maces.cripple_chance_rank_3", 20.0);
        values.put("combat.maces.cripple_chance_rank_4", 33.0);
        values.put("combat.maces.crush_base_damage", 0.5);
        values.put("combat.maces.crush_damage_per_rank", 1.0);
        values.put("combat.spears.momentum_max_percent", 50.0);
        values.put("combat.spears.mastery_damage_per_rank", 0.4);
        var momentumChance = new double[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        for (var i = 1; i <= momentumChance.length; i++) values.put("combat.spears.momentum_chance_rank_" + i, momentumChance[i - 1]);
        values.put("combat.swords.rupture_max_percent", 66.0);
        values.put("combat.swords.stab_base_damage", 1.0);
        values.put("combat.swords.stab_damage_per_rank", 1.5);
        values.put("combat.swords.serrated_strikes_damage_divisor", 4.0);
        values.put("combat.tridents.impale_base_damage", 1.0);
        values.put("combat.tridents.impale_damage_per_rank", 0.5);
        values.put("combat.unarmed.disarm_max_percent", 33.0);
        values.put("combat.unarmed.disarm_max_level", 100.0);
        values.put("combat.unarmed.disarm_anti_theft", 0.0);
        values.put("combat.unarmed.arrow_deflect_max_percent", 50.0);
        values.put("combat.unarmed.arrow_deflect_max_level", 100.0);
        values.put("combat.unarmed.berserk_multiplier", 1.5);
        values.put("combat.unarmed.steel_arm_damage_override", 0.0);
        var steelArmOverride = new double[]{1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 10.5, 12, 13.5};
        for (var i = 1; i <= steelArmOverride.length; i++) values.put("combat.unarmed.steel_arm_override_rank_" + i, steelArmOverride[i - 1]);
        values.put("combat.unarmed.iron_grip_max_percent", 100.0);
        values.put("combat.unarmed.iron_grip_max_level", 100.0);
        values.put("combat.unarmed.block_cracker_enabled", 1.0);
        values.put("combat.unarmed.items_as_unarmed", 0.0);
        values.put("combat.swords.counter_attack_max_percent", 30.0);
        values.put("combat.swords.counter_attack_max_level", 100.0);
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
        values.put("combat.swords.rupture_duration_ticks_pvp", 100.0);
        values.put("combat.swords.rupture_duration_ticks_pve", 100.0);
        values.put("combat.taming.gore_max_percent", 100.0);
        values.put("combat.taming.gore_max_level", 100.0);
        values.put("combat.taming.gore_multiplier", 2.0);
        values.put("taming.fast_food_chance", 50.0);
        values.put("taming.pummel_chance", 10.0);
        values.put("taming.sharpened_claws_bonus", 2.0);
        values.put("taming.thick_fur_divisor", 2.0);
        values.put("taming.shock_proof_divisor", 6.0);
        values.put("taming.call_of_wild_min_horse_jump_strength", 0.7);
        values.put("taming.call_of_wild_max_horse_jump_strength", 2.0);
        values.put("taming.cotw_breeding_prevented", 1.0);
        values.put("fishing.exploit_move_range", 3.0);
        values.put("fishing.exploit_over_fish_limit", 10.0);
        values.put("fishing.master_angler_min_wait_per_rank", 10.0);
        values.put("fishing.master_angler_max_wait_per_rank", 30.0);
        values.put("fishing.master_angler_boat_min_wait", 10.0);
        values.put("fishing.master_angler_boat_max_wait", 30.0);
        values.put("fishing.master_angler_lure_wait", 100.0);
        values.put("fishing.master_angler_min_wait_cap", 40.0);
        values.put("fishing.master_angler_max_wait_cap", 100.0);
        values.put("fishing.fishermans_diet_rank_change", 20.0);
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
        values.put("acrobatics.prevent_dodge_lightning", 0.0);
        values.put("acrobatics.xp_after_teleport_cooldown_seconds", 5.0);
        values.put("smelting.second_smelt_max_percent", 50.0);
        values.put("smelting.second_smelt_max_level", 100.0);
        values.put("smelting.vanilla_xp_multiplier_rank_1", 1.0);
        values.put("smelting.vanilla_xp_multiplier_rank_2", 2.0);
        values.put("smelting.vanilla_xp_multiplier_rank_3", 3.0);
        values.put("smelting.vanilla_xp_multiplier_rank_4", 3.0);
        values.put("smelting.vanilla_xp_multiplier_rank_5", 4.0);
        values.put("smelting.vanilla_xp_multiplier_rank_6", 4.0);
        values.put("smelting.vanilla_xp_multiplier_rank_7", 5.0);
        values.put("smelting.vanilla_xp_multiplier_rank_8", 5.0);
        values.put("alchemy.catalysis_min_speed", 1.0);
        values.put("alchemy.catalysis_max_speed", 4.0);
        values.put("alchemy.catalysis_max_level", 100.0);
        values.put("repair.mastery_max_percent", 200.0);
        values.put("repair.mastery_max_level", 100.0);
        values.put("repair.super_repair_max_percent", 100.0);
        values.put("repair.super_repair_max_level", 100.0);
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
        return new SkillFormulaConfig(values, "minecraft:iron_block", "minecraft:gold_block", "minecraft:flint_and_steel");
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
            var repairAnvilBlock = defaults.repairAnvilBlock;
            var salvageAnvilBlock = defaults.salvageAnvilBlock;
            var miningDetonatorItem = defaults.miningDetonatorItem;
            var present = new java.util.HashSet<String>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                if (separator <= 0 || separator == valueLine.length() - 1) throw new IllegalArgumentException("Invalid formula config line: " + line);
                var key = valueLine.substring(0, separator).trim();
                if (key.equals("repair.anvil_block")) {
                    repairAnvilBlock = valueLine.substring(separator + 1).trim();
                    if (!repairAnvilBlock.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid repair anvil block: " + repairAnvilBlock);
                    present.add(key);
                    continue;
                }
                if (key.equals("salvage.anvil_block")) {
                    salvageAnvilBlock = valueLine.substring(separator + 1).trim();
                    if (!salvageAnvilBlock.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid salvage anvil block: " + salvageAnvilBlock);
                    present.add(key);
                    continue;
                }
                if (key.equals("mining.detonator_item")) {
                    miningDetonatorItem = valueLine.substring(separator + 1).trim();
                    if (!miningDetonatorItem.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid mining detonator item: " + miningDetonatorItem);
                    present.add(key);
                    continue;
                }
                var value = new BigDecimal(valueLine.substring(separator + 1).trim()).doubleValue();
                if (!defaults.values.containsKey(key) || !Double.isFinite(value) || value < 0
                        || (key.endsWith("_divisor") && value <= 0)
                        || ((key.startsWith("fishing.exploit_") || key.endsWith("_max_blocks") || key.endsWith("_max_level") || key.endsWith("_duration_ticks") || key.equals("mining.blast_remote_detonation_distance")
                        || key.equals("abilities.duration_cap_level") || key.equals("abilities.duration_increase_level")) && value != Math.rint(value))) throw new IllegalArgumentException("Unknown or invalid formula: " + key);
                values.put(key, value);
                present.add(key);
            }
            var loaded = new SkillFormulaConfig(values, repairAnvilBlock, salvageAnvilBlock, miningDetonatorItem);
            if (present.size() < defaults.values.size() + 3) Files.writeString(file, loaded.serialize(), StandardCharsets.UTF_8);
            return loaded;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load formula config: " + file, failure);
        }
    }

    public double value(String key) { return values.getOrDefault(key, 0.0); }
    public String repairAnvilBlock() { return repairAnvilBlock; }
    public String salvageAnvilBlock() { return salvageAnvilBlock; }
    public String miningDetonatorItem() { return miningDetonatorItem; }
    public Map<String, Double> values() { return values; }

    private String serialize() {
        var output = new StringBuilder("# BigBangSkills formula/proc constants; values are validated on startup.\n");
        output.append("repair.anvil_block=").append(repairAnvilBlock).append('\n');
        output.append("salvage.anvil_block=").append(salvageAnvilBlock).append('\n');
        output.append("mining.detonator_item=").append(miningDetonatorItem).append('\n');
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> output.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));
        return output.toString();
    }
}
