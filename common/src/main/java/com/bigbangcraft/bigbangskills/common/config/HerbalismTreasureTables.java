package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.common.skill.HerbalismEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** External Hylian Luck rewards with the fixed baseline as defaults. */
public final class HerbalismTreasureTables {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final List<String> DEFAULTS = List.of(
            "bushes|minecraft:melon_seeds=1,0,100,0", "bushes|minecraft:pumpkin_seeds=1,0,100,0", "bushes|minecraft:cocoa_beans=1,0,100,0",
            "flowers|minecraft:carrot=1,0,100,0", "flowers|minecraft:potato=1,0,100,0", "flowers|minecraft:apple=1,0,100,0",
            "pots|minecraft:emerald=1,0,100,0", "pots|minecraft:diamond=1,0,100,0", "pots|minecraft:gold_nugget=1,0,100,0",
            "pots|minecraft:copper_ingot=1,5,100,0");
    private final List<HerbalismEngine.Treasure> treasures;

    private HerbalismTreasureTables(List<HerbalismEngine.Treasure> treasures) { this.treasures = List.copyOf(treasures); }

    public static HerbalismTreasureTables defaults() {
        return parse(DEFAULTS);
    }

    public static HerbalismTreasureTables loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, "# category|item_id=amount,xp,chance_percent,level\n" + String.join("\n", DEFAULTS) + "\n", StandardCharsets.UTF_8);
            return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load herbalism treasure config: " + file, failure);
        }
    }

    public List<HerbalismEngine.Treasure> all() { return treasures; }

    private static HerbalismTreasureTables parse(List<String> lines) {
        var parsed = new java.util.ArrayList<HerbalismEngine.Treasure>();
        for (var line : lines) {
            var value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;
            var equals = value.indexOf('=');
            String[] key = equals < 1 ? new String[0] : value.substring(0, equals).trim().split("\\|", -1);
            String[] fields = equals < 1 ? new String[0] : value.substring(equals + 1).split(",", -1);
            if (key.length != 2 || fields.length != 4 || !ID.matcher(key[1]).matches()) throw new IllegalArgumentException("Invalid herbalism treasure: " + line);
            var treasure = new HerbalismEngine.Treasure(key[0], key[1], Integer.parseInt(fields[0].trim()), Integer.parseInt(fields[1].trim()),
                    Double.parseDouble(fields[2].trim()), Integer.parseInt(fields[3].trim()));
            if (treasure.amount() < 1 || treasure.xp() < 0 || !Double.isFinite(treasure.chancePercent())
                    || treasure.chancePercent() < 0 || treasure.chancePercent() > 100 || treasure.level() < 0)
                throw new IllegalArgumentException("Invalid herbalism treasure values: " + line);
            parsed.add(treasure);
        }
        return new HerbalismTreasureTables(parsed);
    }
}
