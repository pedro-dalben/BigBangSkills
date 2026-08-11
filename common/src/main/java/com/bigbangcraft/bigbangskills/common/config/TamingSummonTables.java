package com.bigbangcraft.bigbangskills.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/** Configurable Call of the Wild recipes; registry resolution stays loader-side. */
public final class TamingSummonTables {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Map<String, Recipe> recipes;

    private TamingSummonTables(Map<String, Recipe> recipes) { this.recipes = Map.copyOf(recipes); }

    public static TamingSummonTables loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, "# entity_id=item_id|items|limit|lifespan_ticks\n"
                    + "minecraft:wolf=minecraft:bone|10|2|4800\n"
                    + "minecraft:cat=minecraft:cod|10|1|4800\n"
                    + "minecraft:horse=minecraft:apple|10|1|4800\n", StandardCharsets.UTF_8);
            var values = new java.util.HashMap<String, Recipe>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                var equals = value.indexOf('=');
                var fields = equals < 1 ? new String[0] : value.substring(equals + 1).split("\\|");
                var entity = equals < 1 ? "" : value.substring(0, equals).trim();
                if (!ID.matcher(entity).matches() || fields.length != 4 || !ID.matcher(fields[0].trim()).matches())
                    throw new IllegalArgumentException("Invalid taming summon line: " + line);
                var items = Integer.parseInt(fields[1].trim());
                var limit = Integer.parseInt(fields[2].trim());
                var lifespan = Integer.parseInt(fields[3].trim());
                if (items < 1 || limit < 1 || lifespan < 1) throw new IllegalArgumentException("Invalid taming summon values: " + line);
                values.put(entity, new Recipe(fields[0].trim(), items, limit, lifespan));
            }
            return new TamingSummonTables(values);
        } catch (IOException | NumberFormatException failure) {
            throw new IllegalStateException("Could not load taming summon config: " + file, failure);
        }
    }

    public Recipe recipe(String entityId) { return recipes.get(entityId); }
    public Map<String, Recipe> snapshot() { return recipes; }
    public record Recipe(String itemId, int itemCount, int ownerLimit, int lifespanTicks) {}
}
