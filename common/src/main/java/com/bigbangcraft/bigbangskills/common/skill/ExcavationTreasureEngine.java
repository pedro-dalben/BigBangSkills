package com.bigbangcraft.bigbangskills.common.skill;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.regex.Pattern;

/** Data-driven Archaeology rolls; item spawning and XP persistence stay loader-owned. */
public final class ExcavationTreasureEngine {
    public record Treasure(String sourceBlock, String itemId, int amount, BigDecimal xp, double chancePercent, int level) {
        public Treasure {
            if (sourceBlock == null || itemId == null || sourceBlock.isBlank() || itemId.isBlank()
                    || amount < 1 || xp.signum() < 0 || !Double.isFinite(chancePercent)
                    || chancePercent < 0 || chancePercent > 100 || level < 0) throw new IllegalArgumentException("Invalid excavation treasure");
        }
    }
    public record Reward(String itemId, int amount, BigDecimal xp) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final List<Treasure> treasures;

    public ExcavationTreasureEngine(List<Treasure> treasures) { this.treasures = List.copyOf(treasures); }
    public static ExcavationTreasureEngine defaults() { return new ExcavationTreasureEngine(loadResource()); }

    public static ExcavationTreasureEngine loadOrCreate(Path file) {
        var defaults = new HashMap<String, Treasure>();
        for (var treasure : defaults().treasures) defaults.put(key(treasure), treasure);
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) {
                var output = new StringBuilder("# source_block|item_id=amount|xp|chance_percent|level\n");
                defaults.values().stream().sorted(java.util.Comparator.comparing(ExcavationTreasureEngine::key)).forEach(t -> output.append(key(t)).append('=').append(t.amount()).append('|').append(t.xp()).append('|').append(t.chancePercent()).append('|').append(t.level()).append('\n'));
                Files.writeString(file, output, StandardCharsets.UTF_8);
                return new ExcavationTreasureEngine(new ArrayList<>(defaults.values()));
            }
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                var equals = value.indexOf('=');
                var key = equals > 0 ? value.substring(0, equals).trim() : "";
                var parts = equals > 0 && equals < value.length() - 1 ? value.substring(equals + 1).split("\\|", -1) : new String[0];
                var split = key.indexOf('|');
                if (split <= 0 || parts.length != 4) throw new IllegalArgumentException("Invalid excavation treasure: " + line);
                var source = key.substring(0, split).trim();
                var item = key.substring(split + 1).trim();
                if (!ID.matcher(source).matches() || !ID.matcher(item).matches()) throw new IllegalArgumentException("Invalid excavation treasure ID: " + line);
                defaults.put(source + "|" + item, new Treasure(source, item, Integer.parseInt(parts[0]), new BigDecimal(parts[1]), Double.parseDouble(parts[2]), Integer.parseInt(parts[3])));
            }
            return new ExcavationTreasureEngine(new ArrayList<>(defaults.values()));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load excavation treasures: " + file, failure);
        }
    }

    public List<Reward> roll(String sourceBlock, int level, boolean archaeology, boolean abilityActive, DoubleSupplier random) {
        if (!archaeology || level < 1) return List.of();
        var rewards = new ArrayList<Reward>();
        var rolls = abilityActive ? 3 : 1;
        for (var roll = 0; roll < rolls; roll++) for (var treasure : treasures) {
            if (!treasure.sourceBlock().equals(sourceBlock) || level < treasure.level()
                    || !SkillChance.succeeds(treasure.chancePercent(), random)) continue;
            rewards.add(new Reward(treasure.itemId(), treasure.amount(), treasure.xp()));
        }
        return List.copyOf(rewards);
    }

    public List<Treasure> treasures() { return treasures; }

    private static String key(Treasure treasure) { return treasure.sourceBlock() + "|" + treasure.itemId(); }

    private static List<Treasure> loadResource() {
        try (var input = ExcavationTreasureEngine.class.getClassLoader().getResourceAsStream("bigbangskills/excavation-treasures.properties")) {
            if (input == null) throw new IllegalStateException("Missing excavation treasure table");
            var values = new ArrayList<Treasure>();
            for (var line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                var value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                var equals = value.indexOf('=');
                var key = value.substring(0, equals).split("\\|", -1);
                var parts = value.substring(equals + 1).split("\\|", -1);
                values.add(new Treasure(key[0], key[1], Integer.parseInt(parts[0]), new BigDecimal(parts[1]), Double.parseDouble(parts[2]), Integer.parseInt(parts[3])));
            }
            return values;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load excavation treasure resource", failure);
        }
    }
}
