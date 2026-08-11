package com.bigbangcraft.bigbangskills.common.skill;

import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.DoubleSupplier;

/** Bounded fishing treasure resolver matching the reference rarity/tier model. */
public final class FishingTreasureEngine {
    public record Reward(String itemId, int amount, int xp, boolean enchantable, int rarity) {}
    private record Entry(String itemId, int amount, int xp, int rarity, boolean enchantable) {}

    private static final List<Entry> DEFAULT_ENTRIES = List.of(
            new Entry("minecraft:leather_boots", 1, 200, 0, true), new Entry("minecraft:leather_helmet", 1, 200, 0, true),
            new Entry("minecraft:leather_leggings", 1, 200, 0, true), new Entry("minecraft:leather_chestplate", 1, 200, 0, true),
            new Entry("minecraft:wooden_sword", 1, 200, 0, true), new Entry("minecraft:wooden_shovel", 1, 200, 0, true),
            new Entry("minecraft:wooden_pickaxe", 1, 200, 0, true), new Entry("minecraft:wooden_axe", 1, 200, 0, true),
            new Entry("minecraft:wooden_hoe", 1, 200, 0, true), new Entry("minecraft:lapis_lazuli", 20, 200, 0, false),
            new Entry("minecraft:stone_sword", 1, 200, 1, true), new Entry("minecraft:stone_shovel", 1, 200, 1, true),
            new Entry("minecraft:stone_pickaxe", 1, 200, 1, true), new Entry("minecraft:stone_axe", 1, 200, 1, true),
            new Entry("minecraft:stone_hoe", 1, 200, 1, true), new Entry("minecraft:copper_sword", 1, 200, 1, true),
            new Entry("minecraft:copper_shovel", 1, 200, 1, true), new Entry("minecraft:copper_pickaxe", 1, 200, 1, true),
            new Entry("minecraft:copper_axe", 1, 200, 1, true), new Entry("minecraft:copper_hoe", 1, 200, 1, true),
            new Entry("minecraft:golden_sword", 1, 200, 1, true), new Entry("minecraft:golden_shovel", 1, 200, 1, true),
            new Entry("minecraft:golden_pickaxe", 1, 200, 1, true), new Entry("minecraft:golden_axe", 1, 200, 1, true),
            new Entry("minecraft:golden_hoe", 1, 200, 1, true), new Entry("minecraft:golden_boots", 1, 200, 1, true),
            new Entry("minecraft:golden_helmet", 1, 200, 1, true), new Entry("minecraft:golden_leggings", 1, 200, 1, true),
            new Entry("minecraft:golden_chestplate", 1, 200, 1, true), new Entry("minecraft:iron_ingot", 5, 200, 1, false),
            new Entry("minecraft:gold_ingot", 5, 200, 1, false), new Entry("minecraft:iron_sword", 1, 200, 2, true),
            new Entry("minecraft:iron_shovel", 1, 200, 2, true), new Entry("minecraft:iron_pickaxe", 1, 200, 2, true),
            new Entry("minecraft:iron_axe", 1, 200, 2, true), new Entry("minecraft:iron_hoe", 1, 200, 2, true),
            new Entry("minecraft:bow", 1, 200, 2, true), new Entry("minecraft:ender_pearl", 1, 200, 2, false),
            new Entry("minecraft:blaze_rod", 1, 200, 2, false), new Entry("minecraft:name_tag", 1, 200, 2, false),
            new Entry("minecraft:copper_boots", 1, 200, 0, true), new Entry("minecraft:copper_helmet", 1, 200, 0, true),
            new Entry("minecraft:copper_leggings", 1, 200, 0, true), new Entry("minecraft:copper_chestplate", 1, 200, 0, true),
            new Entry("minecraft:iron_boots", 1, 200, 3, true), new Entry("minecraft:iron_helmet", 1, 200, 3, true),
            new Entry("minecraft:iron_leggings", 1, 200, 3, true), new Entry("minecraft:iron_chestplate", 1, 200, 3, true),
            new Entry("minecraft:ghast_tear", 1, 200, 3, false), new Entry("minecraft:diamond", 5, 200, 3, false),
            new Entry("minecraft:nautilus_shell", 1, 200, 4, false), new Entry("minecraft:diamond_sword", 1, 200, 4, true),
            new Entry("minecraft:diamond_shovel", 1, 200, 4, true), new Entry("minecraft:diamond_pickaxe", 1, 200, 4, true),
            new Entry("minecraft:diamond_axe", 1, 200, 4, true), new Entry("minecraft:diamond_hoe", 1, 200, 4, true),
            new Entry("minecraft:diamond_boots", 1, 200, 4, true), new Entry("minecraft:diamond_helmet", 1, 200, 4, true),
            new Entry("minecraft:diamond_leggings", 1, 200, 4, true), new Entry("minecraft:diamond_chestplate", 1, 200, 4, true),
            new Entry("minecraft:netherite_sword", 1, 200, 5, true), new Entry("minecraft:netherite_shovel", 1, 200, 5, true),
            new Entry("minecraft:netherite_pickaxe", 1, 200, 5, true), new Entry("minecraft:netherite_axe", 1, 200, 5, true),
            new Entry("minecraft:netherite_hoe", 1, 200, 5, true), new Entry("minecraft:netherite_boots", 1, 200, 5, true),
            new Entry("minecraft:netherite_helmet", 1, 200, 5, true), new Entry("minecraft:netherite_leggings", 1, 200, 5, true),
            new Entry("minecraft:netherite_chestplate", 1, 200, 5, true), new Entry("minecraft:enchanted_book", 1, 400, 4, true),
            new Entry("minecraft:netherite_scrap", 1, 400, 4, false));

    private static final double[][] RATES = {
            {7.50, 1.25, .25, .10, .01, .01},
            {6.50, 1.75, .75, .50, .05, .01},
            {3.50, 2.75, 1.25, 1.00, .10, .01},
            {2.00, 3.50, 2.25, 1.50, 1.00, .01},
            {1.50, 3.75, 2.50, 2.00, 1.00, .01},
            {1.00, 3.25, 3.75, 2.50, 1.50, .05},
            {.25, 2.75, 4.00, 5.00, 2.50, .10},
            {.10, 1.50, 6.00, 7.50, 5.00, .25}};
    private static final double[][] ENCHANTMENT_RATES = {
            {5, 1, .1, .01, .01, .01}, {7.5, 1, .1, .01, .01, .01}, {7.5, 2.5, .25, .1, .01, .01},
            {10, 2.75, .5, .1, .05, .05}, {10, 4, .75, .25, .1, .1}, {9.5, 5.5, 1.75, .5, .25, .25},
            {8.5, 7.5, 2.75, .75, .5, .5}, {7.5, 10, 5.25, 1.5, .75, .75}};
    private static final String[][] MAGIC_ENCHANTMENTS = {
            {"efficiency:1", "unbreaking:1", "fortune:1", "protection:1", "fire_protection:1", "feather_falling:1", "blast_protection:1", "projectile_protection:1", "respiration:1", "thorns:1", "sharpness:1", "smite:1", "bane_of_arthropods:1", "power:1"},
            {"efficiency:2", "protection:2", "fire_protection:2", "feather_falling:2", "blast_protection:2", "projectile_protection:2", "sharpness:2", "smite:2", "bane_of_arthropods:2", "knockback:1", "looting:1", "power:2", "punch:1"},
            {"efficiency:3", "unbreaking:2", "protection:3", "fire_protection:3", "feather_falling:3", "blast_protection:3", "projectile_protection:3", "respiration:2", "sharpness:3", "smite:3", "bane_of_arthropods:3", "fire_aspect:1", "looting:2", "power:3"},
            {"efficiency:4", "fortune:2", "aqua_affinity:1", "thorns:2", "sharpness:4", "smite:4", "bane_of_arthropods:4", "power:4", "flame:1"},
            {"efficiency:5", "unbreaking:3", "fortune:3", "protection:4", "fire_protection:4", "feather_falling:4", "blast_protection:4", "projectile_protection:4", "respiration:3", "aqua_affinity:1", "thorns:3", "sharpness:5", "smite:5", "bane_of_arthropods:5", "knockback:2", "fire_aspect:2", "looting:3", "silk_touch:1", "power:5", "punch:2", "infinity:1"},
            {"efficiency:5", "unbreaking:3", "fortune:3", "protection:4", "fire_protection:4", "feather_falling:4", "blast_protection:4", "projectile_protection:4", "respiration:3", "aqua_affinity:1", "thorns:3", "sharpness:5", "smite:5", "bane_of_arthropods:5", "knockback:2", "fire_aspect:2", "looting:3", "silk_touch:1", "power:5", "punch:2", "infinity:1"}};
    private final List<Entry> entries;

    public FishingTreasureEngine() { this(DEFAULT_ENTRIES); }

    private FishingTreasureEngine(List<Entry> entries) { this.entries = List.copyOf(entries); }

    public static FishingTreasureEngine loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, serialize(DEFAULT_ENTRIES), StandardCharsets.UTF_8);
            var parsed = new java.util.ArrayList<Entry>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                var pair = value.split("=", 2);
                var key = pair.length == 2 ? pair[0].split("\\|", 2) : new String[0];
                var fields = pair.length == 2 ? pair[1].split(",", -1) : new String[0];
                if (key.length != 2 || fields.length != 3) throw new IllegalArgumentException("Invalid fishing treasure entry: " + line);
                var itemId = key[1].trim();
                var entry = new Entry(itemId, Integer.parseInt(fields[0].trim()), Integer.parseInt(fields[1].trim()), Integer.parseInt(key[0].trim()), bool(fields[2].trim()));
                if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || entry.amount() < 1 || entry.xp() < 0 || entry.rarity() < 0 || entry.rarity() >= 6) {
                    throw new IllegalArgumentException("Invalid fishing treasure values: " + line);
                }
                parsed.add(entry);
            }
            if (parsed.isEmpty()) throw new IllegalArgumentException("Fishing treasure table cannot be empty");
            return new FishingTreasureEngine(parsed);
        } catch (java.io.IOException | RuntimeException failure) {
            throw new IllegalStateException("Could not load fishing treasure rules: " + file, failure);
        }
    }

    public Optional<Reward> roll(int level, DoubleSupplier random) {
        return roll(level, 0, random);
    }

    public Optional<Reward> roll(int level, int luckOfTheSea, DoubleSupplier random) {
        var tier = Math.max(1, Math.min(8, new FishingEngine().treasureTier(level))) - 1;
        var pick = random.getAsDouble() * 100 * Math.max(0, 1 - Math.max(0, luckOfTheSea) * 0.04);
        var rarity = -1;
        for (var i = 0; i < RATES[tier].length; i++) if ((pick -= RATES[tier][i]) < 0) { rarity = i; break; }
        if (rarity < 0) return Optional.empty();
        final var selectedRarity = rarity;
        var choices = entries.stream().filter(entry -> entry.rarity() == selectedRarity).toList();
        if (choices.isEmpty()) return Optional.empty();
        var entry = choices.get(Math.min(choices.size() - 1, (int) (random.getAsDouble() * choices.size())));
        return Optional.of(new Reward(entry.itemId(), entry.amount(), entry.xp(), entry.enchantable(), entry.rarity()));
    }

    public Optional<MagicEnchantment> magicHunter(int level, int rarity, DoubleSupplier random) {
        if (level < 20 || rarity < 0 || rarity >= 6) return Optional.empty();
        var tier = Math.max(1, Math.min(8, new FishingEngine().treasureTier(level))) - 1;
        if (!SkillChance.succeeds(ENCHANTMENT_RATES[tier][rarity], random)) return Optional.empty();
        var choices = MAGIC_ENCHANTMENTS[rarity];
        var choice = choices[Math.min(choices.length - 1, (int) (random.getAsDouble() * choices.length))];
        var separator = choice.indexOf(':');
        return Optional.of(new MagicEnchantment("minecraft:" + choice.substring(0, separator), Integer.parseInt(choice.substring(separator + 1))));
    }

    public record MagicEnchantment(String enchantmentId, int level) {}

    private static String serialize(List<Entry> values) {
        var output = new StringBuilder("# rarity|item_id=amount,xp,enchantable\n");
        values.forEach(entry -> output.append(entry.rarity()).append('|').append(entry.itemId()).append('=')
                .append(entry.amount()).append(',').append(entry.xp()).append(',').append(entry.enchantable()).append('\n'));
        return output.toString();
    }

    private static boolean bool(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Expected boolean, got: " + value);
    }
}
