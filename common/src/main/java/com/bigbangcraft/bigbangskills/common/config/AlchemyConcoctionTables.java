package com.bigbangcraft.bigbangskills.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/** Ingredient ranks plus optional registry-backed effects for modded brewing. */
public final class AlchemyConcoctionTables {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final String[] DEFAULTS = {
            "minecraft:breeze_rod=1", "minecraft:blaze_powder=1", "minecraft:fermented_spider_eye=1", "minecraft:ghast_tear=1", "minecraft:glowstone_dust=1", "minecraft:golden_carrot=1", "minecraft:magma_cream=1", "minecraft:nether_wart=1", "minecraft:redstone=1", "minecraft:glistering_melon_slice=1", "minecraft:spider_eye=1", "minecraft:sugar=1", "minecraft:gunpowder=1", "minecraft:water_lily=1", "minecraft:pufferfish=1", "minecraft:dragon_breath=1", "minecraft:stone=1", "minecraft:slime_block=1", "minecraft:cobweb=1", "minecraft:turtle_helmet=1",
            "minecraft:carrot=2", "minecraft:slime_ball=2", "minecraft:phantom_membrane=2", "minecraft:quartz=3", "minecraft:rabbit_foot=3", "minecraft:apple=4", "minecraft:rotten_flesh=4", "minecraft:brown_mushroom=5", "minecraft:ink_sac=5", "minecraft:fern=6", "minecraft:poisonous_potato=7", "minecraft:golden_apple=8"
    };
    private final Map<String, Recipe> recipes;
    private AlchemyConcoctionTables(Map<String, Recipe> recipes) { this.recipes = Map.copyOf(recipes); }

    public static AlchemyConcoctionTables loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, "# ingredient_id=tier|effect_id|duration_ticks|amplifier; effect fields may be omitted\n"
                    + String.join("\n", DEFAULTS) + "\n", StandardCharsets.UTF_8);
            var values = new java.util.HashMap<String, Recipe>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim(); if (value.isEmpty() || value.startsWith("#")) continue;
                var equals = value.indexOf('='); var fields = equals < 1 ? new String[0] : value.substring(equals + 1).split("\\|");
                var ingredient = equals < 1 ? "" : value.substring(0, equals).trim();
                if (!ID.matcher(ingredient).matches() || (fields.length != 1 && fields.length != 4)) throw new IllegalArgumentException("Invalid alchemy line: " + line);
                var tier = Integer.parseInt(fields[0].trim());
                if (tier < 1 || tier > 8) throw new IllegalArgumentException("Invalid alchemy tier: " + line);
                if (fields.length == 1) values.put(ingredient, new Recipe(tier, null, 0, 0));
                else {
                    var effect = fields[1].trim();
                    if (!effect.equals("-") && !ID.matcher(effect).matches()) throw new IllegalArgumentException("Invalid alchemy effect: " + line);
                    var duration = Integer.parseInt(fields[2].trim()); var amplifier = Integer.parseInt(fields[3].trim());
                    if (duration < 1 || amplifier < 0) throw new IllegalArgumentException("Invalid alchemy effect values: " + line);
                    values.put(ingredient, new Recipe(tier, effect.equals("-") ? null : effect, duration, amplifier));
                }
            }
            return new AlchemyConcoctionTables(values);
        } catch (IOException | NumberFormatException failure) {
            throw new IllegalStateException("Could not load alchemy config: " + file, failure);
        }
    }

    public Recipe recipe(String ingredientId) { return recipes.get(ingredientId); }
    public record Recipe(int tier, String effectId, int durationTicks, int amplifier) {}
}
