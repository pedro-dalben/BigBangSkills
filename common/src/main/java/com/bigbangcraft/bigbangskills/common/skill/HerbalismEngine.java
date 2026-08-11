package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.HerbalismTreasureTables;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/** Pure Herbalism rules; loader code owns registry/state/inventory mutation. */
public final class HerbalismEngine {
    public record Treasure(String category, String itemId, int amount, int xp, double chancePercent, int level) {}
    public record Reward(String itemId, int amount, int xp) {}

    private final List<Treasure> hylianTreasures;

    public HerbalismEngine() { this(HerbalismTreasureTables.defaults().all()); }
    public HerbalismEngine(List<Treasure> hylianTreasures) { this.hylianTreasures = List.copyOf(hylianTreasures); }

    public double doubleDropsChance(int level, double maxPercent) { return linear(level, 100, maxPercent); }
    public double doubleDropsChance(int level, double maxPercent, int maxLevel) { return linear(level, maxLevel, maxPercent); }
    public double verdantBountyChance(int level, double maxPercent, int maxLevel) { return linear(level, maxLevel, maxPercent); }
    public double greenThumbChance(int level, double maxPercent) { return linear(level, 100, maxPercent); }
    public double greenThumbChance(int level, double maxPercent, int maxLevel) { return linear(level, maxLevel, maxPercent); }
    public double hylianLuckChance(int level, double maxPercent) { return linear(level, 100, maxPercent); }
    public double hylianLuckChance(int level, double maxPercent, int maxLevel) { return linear(level, maxLevel, maxPercent); }
    public double shroomThumbChance(int level, double maxPercent) { return linear(level, 100, maxPercent); }
    public double shroomThumbChance(int level, double maxPercent, int maxLevel) { return linear(level, maxLevel, maxPercent); }

    public int farmersDiet(int level, int food) {
        if (food <= 0) return food;
        return food + Math.min(5, Math.max(0, level / 20));
    }

    public boolean mature(int age, int maxAge, boolean sweetBerryBush) {
        return sweetBerryBush ? age >= 2 : maxAge > 0 && age == maxAge;
    }

    public Optional<String> greenTerraConversion(String blockId) {
        return switch (blockId) {
            case "minecraft:cobblestone_wall" -> Optional.of("minecraft:mossy_cobblestone_wall");
            case "minecraft:stone_bricks" -> Optional.of("minecraft:mossy_stone_bricks");
            case "minecraft:dirt", "minecraft:dirt_path" -> Optional.of("minecraft:grass_block");
            case "minecraft:cobblestone" -> Optional.of("minecraft:mossy_cobblestone");
            default -> Optional.empty();
        };
    }

    public Optional<String> shroomThumbConversion(String blockId) {
        return switch (blockId) {
            case "minecraft:dirt", "minecraft:grass_block", "minecraft:dirt_path" -> Optional.of("minecraft:mycelium");
            default -> Optional.empty();
        };
    }

    public Optional<Reward> hylianLuck(String blockId, int level, double maxPercent, DoubleSupplier random) {
        return hylianLuck(blockId, level, maxPercent, 100, random);
    }

    public Optional<Reward> hylianLuck(String blockId, int level, double maxPercent, int maxLevel, DoubleSupplier random) {
        var category = category(blockId);
        if (category.isEmpty() || random.getAsDouble() >= hylianLuckChance(level, maxPercent, maxLevel) / 100.0) return Optional.empty();
        return hylianTreasures.stream().filter(value -> value.category().equals(category.get()) && level >= value.level())
                .filter(value -> random.getAsDouble() < value.chancePercent() / 100.0)
                .findFirst().map(value -> new Reward(value.itemId(), value.amount(), value.xp()));
    }

    public Optional<String> category(String blockId) {
        if (blockId.equals("minecraft:sweet_berry_bush") || blockId.equals("minecraft:cocoa") || blockId.contains("bush")
                || blockId.endsWith("_sapling") || blockId.equals("minecraft:fern") || blockId.equals("minecraft:large_fern")
                || blockId.equals("minecraft:short_grass") || blockId.equals("minecraft:tall_grass")) return Optional.of("bushes");
        if (blockId.equals("minecraft:flower_pot") || blockId.equals("minecraft:decorated_pot")) return Optional.of("pots");
        if (blockId.endsWith("_flower") || blockId.contains("tulip") || blockId.contains("allium") || blockId.contains("orchid") || blockId.contains("daisy") || blockId.contains("poppy") || blockId.contains("dandelion") || blockId.contains("cornflower") || blockId.contains("lily_of_the_valley") || blockId.contains("wither_rose") || blockId.contains("sunflower") || blockId.contains("lilac") || blockId.contains("peony") || blockId.contains("rose_bush") || blockId.contains("torchflower")) return Optional.of("flowers");
        return Optional.empty();
    }

    private static double linear(int level, int maxLevel, double maxPercent) {
        return Math.min(maxPercent, Math.max(0, level) * maxPercent / Math.max(1, maxLevel));
    }
}
