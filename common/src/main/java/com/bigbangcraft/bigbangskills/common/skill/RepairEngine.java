package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;

import java.util.function.DoubleSupplier;

/** Reference repair math; inventory/anvil mutation remains loader-owned. */
public final class RepairEngine {
    private final SkillFormulaConfig formulas;
    private final DoubleSupplier randomUnit;

    public RepairEngine(SkillFormulaConfig formulas, DoubleSupplier randomUnit) {
        this.formulas = java.util.Objects.requireNonNull(formulas);
        this.randomUnit = java.util.Objects.requireNonNull(randomUnit);
    }

    public static String materialItem(String category) {
        return switch (category == null ? "" : category.toLowerCase(java.util.Locale.ROOT)) {
            case "string" -> "minecraft:string";
            case "leather" -> "minecraft:leather";
            case "wood" -> "minecraft:oak_planks";
            case "stone" -> "minecraft:cobblestone";
            case "iron" -> "minecraft:iron_ingot";
            case "copper" -> "minecraft:copper_ingot";
            case "gold" -> "minecraft:gold_ingot";
            case "diamond" -> "minecraft:diamond";
            case "netherite" -> "minecraft:netherite_scrap";
            default -> null;
        };
    }

    public static String materialItem(String category, String itemId) {
        var material = materialItem(category);
        if (material != null) return material;
        return switch (path(itemId)) {
            case "elytra" -> "minecraft:phantom_membrane";
            case "trident" -> "minecraft:prismarine_crystals";
            case "mace" -> "minecraft:breeze_rod";
            default -> null;
        };
    }

    public static int minimumQuantity(String itemId) {
        var path = path(itemId);
        if (path.equals("shield")) return 6;
        if (path.equals("elytra")) return 8;
        if (path.equals("trident")) return 16;
        if (path.equals("mace")) return 4;
        if (path.contains("helmet")) return 5;
        if (path.contains("chestplate")) return 8;
        if (path.contains("leggings")) return 7;
        if (path.contains("boots")) return 4;
        if (path.contains("pickaxe") || path.contains("axe")) return 3;
        if (path.contains("sword") || path.contains("hoe") || path.contains("bow") || path.contains("rod")) return 2;
        if (path.contains("shovel") || path.equals("shears") || path.equals("flint_and_steel") || path.contains("on_a_stick")) return 1;
        return 1;
    }

    public static double xpMultiplier(String itemId) {
        var path = path(itemId);
        if (path.equals("shield")) return .25;
        if (path.startsWith("wooden_") || path.startsWith("stone_")) return path.contains("shovel") ? .16 : path.contains("sword") || path.contains("hoe") ? .25 : .5;
        if (path.startsWith("copper_")) return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots") ? 1.8 : path.contains("shovel") || path.contains("hoe") ? .2 : .3;
        if (path.startsWith("iron_") || path.equals("shears") || path.equals("flint_and_steel")) return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots") ? 2 : path.contains("shovel") || path.equals("flint_and_steel") ? .3 : path.contains("pickaxe") || path.contains("axe") ? 1 : .5;
        if (path.startsWith("golden_")) return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots") || path.contains("sword") || path.contains("hoe") ? 4 : path.contains("shovel") ? 2.6 : 8;
        if (path.startsWith("diamond_")) return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots") ? 6 : path.contains("shovel") ? .3 : path.contains("pickaxe") || path.contains("axe") ? 1 : .5;
        if (path.startsWith("netherite_")) return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots") ? 7 : path.contains("shovel") ? .4 : path.contains("hoe") ? .75 : path.contains("pickaxe") || path.contains("axe") ? 1.1 : .6;
        if (path.startsWith("leather_")) return 1;
        if (path.equals("elytra") || path.equals("trident") || path.equals("mace")) return 3;
        if (path.contains("bow") || path.contains("rod") || path.contains("on_a_stick")) return .5;
        return 1;
    }

    private static String path(String itemId) {
        if (itemId == null || itemId.isBlank()) return "";
        var separator = itemId.indexOf(':');
        return (separator < 0 ? itemId : itemId.substring(separator + 1)).toLowerCase(java.util.Locale.ROOT);
    }

    public int repairedDurability(int currentDamage, int baseAmount, int level) {
        if (currentDamage <= 0 || baseAmount <= 0) return 0;
        var mastery = formulas.value("repair.mastery_max_percent")
                * Math.min(1.0, Math.max(0, level) / formulas.value("repair.mastery_max_level")) / 100.0;
        var amount = (int) Math.round(baseAmount * (1.0 + mastery));
        if (SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("repair.super_repair_max_level"), formulas.value("repair.super_repair_max_percent")), randomUnit)) amount *= 2;
        return Math.min(currentDamage, Math.max(1, amount));
    }

    public int arcaneForgingLevel(int rank, int enchantmentLevel) {
        if (enchantmentLevel <= 0 || rank <= 0) return 0;
        var boundedRank = Math.min(8, rank);
        var keep = formulas.value("repair.arcane_forging_keep_rank_" + boundedRank);
        var downgrade = formulas.value("repair.arcane_forging_downgrade_rank_" + boundedRank);
        if (randomUnit.getAsDouble() * 100 >= keep) return 0;
        var level = Math.min(5, enchantmentLevel);
        return enchantmentLevel > 1 && randomUnit.getAsDouble() * 100 >= 100 - downgrade
                ? level - 1 : level;
    }
}
