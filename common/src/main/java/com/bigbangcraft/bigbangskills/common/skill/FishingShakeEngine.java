package com.bigbangcraft.bigbangskills.common.skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

/** Configurable entity loot resolver for Fishing Shake. */
public final class FishingShakeEngine {
    private static final String REGISTRY_ID = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    private static final String ENTITY_KEY = "[a-z0-9_.-]+(?::[a-z0-9_./-]+)?";
    public record Reward(String itemId, int amount, int xp, String potion) {
        public Reward(String itemId, int amount, int xp) { this(itemId, amount, xp, ""); }
    }
    public record Entry(String itemId, int amount, int xp, double chance, int level, String potion) {
        public Entry(String itemId, int amount, int xp, double chance, int level) { this(itemId, amount, xp, chance, level, ""); }
    }
    private final Map<String, List<Entry>> entries;

    public FishingShakeEngine() { this(defaults()); }
    public FishingShakeEngine(Map<String, List<Entry>> entries) { this.entries = Map.copyOf(entries); }

    public java.util.Optional<Reward> roll(String entityId, int level, DoubleSupplier random) {
        var choices = entries.getOrDefault(entityId, entries.getOrDefault(entityPath(entityId), List.of())).stream()
                .filter(entry -> level >= entry.level()).toList();
        var dice = random.getAsDouble() * 100.0;
        for (var entry : choices) {
            if (dice < entry.chance()) return java.util.Optional.of(new Reward(entry.itemId(), entry.amount(), entry.xp(), entry.potion()));
            dice -= entry.chance();
        }
        return java.util.Optional.empty();
    }

    public Map<String, List<String>> configuredEntries() {
        return entries.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                value -> value.getValue().stream().map(entry -> entry.itemId() + "," + entry.amount() + "," + entry.xp() + "," + entry.chance() + "," + entry.level() + (entry.potion().isBlank() ? "" : "," + entry.potion())).toList()));
    }

    public static FishingShakeEngine loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, serialize(defaults()), StandardCharsets.UTF_8);
            var parsed = new java.util.HashMap<String, List<Entry>>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                var pair = value.split("=", 2);
                if (pair.length != 2) throw new IllegalArgumentException("Invalid fishing shake entry: " + line);
                var key = pair[0].split("\\|", 2);
                var fields = pair[1].split(",", -1);
                if (key.length != 2 || (fields.length != 4 && fields.length != 5)) throw new IllegalArgumentException("Invalid fishing shake entry: " + line);
                var entry = new Entry(key[1], Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Double.parseDouble(fields[2]), Integer.parseInt(fields[3]), fields.length == 5 ? fields[4].trim() : "");
                if (!key[0].matches(ENTITY_KEY) || !entry.itemId().matches(REGISTRY_ID)
                        || (fields.length != 4 && fields.length != 5) || entry.amount() < 1 || entry.xp() < 0 || !Double.isFinite(entry.chance()) || entry.chance() < 0 || entry.chance() > 100 || entry.level() < 0
                        || (!entry.potion().isBlank() && !entry.potion().matches("[a-z0-9_.-]+"))) {
                    throw new IllegalArgumentException("Invalid fishing shake values: " + line);
                }
                parsed.computeIfAbsent(key[0], ignored -> new ArrayList<>()).add(entry);
            }
            return new FishingShakeEngine(parsed);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Could not load fishing shake rules: " + file, failure);
        }
    }

    private static String serialize(Map<String, List<Entry>> values) {
        var output = new StringBuilder("# entity_path_or_registry_id|item_id=amount,xp,chance_percent,unlock_level[,potion]\n");
        values.forEach((entity, entries) -> entries.forEach(entry -> output.append(entity).append('|').append(entry.itemId()).append('=').append(entry.amount()).append(',').append(entry.xp()).append(',').append(entry.chance()).append(',').append(entry.level()).append('\n')));
        return output.toString();
    }

    private static Map<String, List<Entry>> defaults() {
        return Map.ofEntries(
                Map.entry("blaze", List.of(new Entry("minecraft:blaze_rod", 1, 0, 100, 0))),
                Map.entry("cave_spider", List.of(new Entry("minecraft:spider_eye", 1, 0, 49, 0), new Entry("minecraft:string", 1, 0, 49, 0), new Entry("minecraft:cobweb", 1, 0, 1, 0), new Entry("minecraft:potion", 1, 0, 1, 0, "poison"))),
                Map.entry("chicken", List.of(new Entry("minecraft:feather", 1, 0, 33.3, 0), new Entry("minecraft:chicken", 1, 0, 33.3, 0), new Entry("minecraft:egg", 1, 0, 33.4, 0))),
                Map.entry("cow", List.of(new Entry("minecraft:milk_bucket", 1, 0, 2, 0), new Entry("minecraft:leather", 1, 0, 49, 0), new Entry("minecraft:beef", 1, 0, 49, 0))),
                Map.entry("creeper", List.of(new Entry("minecraft:creeper_head", 1, 0, 1, 0), new Entry("minecraft:gunpowder", 1, 0, 99, 0))),
                Map.entry("enderman", List.of(new Entry("minecraft:ender_pearl", 1, 0, 100, 0))),
                Map.entry("ghast", List.of(new Entry("minecraft:gunpowder", 1, 0, 50, 0), new Entry("minecraft:ghast_tear", 1, 0, 50, 0))),
                Map.entry("horse", List.of(new Entry("minecraft:leather", 1, 0, 99, 0), new Entry("minecraft:saddle", 1, 0, 1, 0))),
                Map.entry("iron_golem", List.of(new Entry("minecraft:pumpkin", 1, 0, 3, 0), new Entry("minecraft:iron_ingot", 1, 0, 12, 0), new Entry("minecraft:poppy", 1, 0, 85, 0))),
                Map.entry("magma_cube", List.of(new Entry("minecraft:magma_cream", 1, 0, 100, 0))),
                Map.entry("mooshroom", List.of(new Entry("minecraft:milk_bucket", 1, 0, 5, 0), new Entry("minecraft:mushroom_stew", 1, 0, 5, 0), new Entry("minecraft:leather", 1, 0, 30, 0), new Entry("minecraft:beef", 1, 0, 30, 0), new Entry("minecraft:red_mushroom", 2, 0, 30, 0))),
                Map.entry("pig", List.of(new Entry("minecraft:porkchop", 1, 0, 100, 0))),
                Map.entry("pig_zombie", List.of(new Entry("minecraft:rotten_flesh", 1, 0, 50, 0), new Entry("minecraft:gold_nugget", 1, 0, 50, 0))),
                Map.entry("zombified_piglin", List.of(new Entry("minecraft:rotten_flesh", 1, 0, 50, 0), new Entry("minecraft:gold_nugget", 1, 0, 50, 0))),
                Map.entry("sheep", List.of(new Entry("minecraft:white_wool", 3, 0, 100, 0))),
                Map.entry("shulker", List.of(new Entry("minecraft:shulker_shell", 1, 0, 25, 0), new Entry("minecraft:purpur_block", 1, 0, 75, 0))),
                Map.entry("skeleton", List.of(new Entry("minecraft:skeleton_skull", 1, 0, 2, 0), new Entry("minecraft:bone", 1, 0, 49, 0), new Entry("minecraft:arrow", 2, 0, 49, 0))),
                Map.entry("slime", List.of(new Entry("minecraft:slime_ball", 1, 0, 100, 0))),
                Map.entry("spider", List.of(new Entry("minecraft:spider_eye", 1, 0, 50, 0), new Entry("minecraft:string", 1, 0, 50, 0))),
                Map.entry("snowman", List.of(new Entry("minecraft:pumpkin", 1, 0, 3, 0), new Entry("minecraft:snowball", 2, 0, 97, 0))),
                Map.entry("snow_golem", List.of(new Entry("minecraft:pumpkin", 1, 0, 3, 0), new Entry("minecraft:snowball", 2, 0, 97, 0))),
                Map.entry("squid", List.of(new Entry("minecraft:ink_sac", 1, 0, 100, 0))),
                Map.entry("witch", List.of(new Entry("minecraft:splash_potion", 1, 0, 1, 0, "instant_heal"), new Entry("minecraft:splash_potion", 1, 0, 1, 0, "fire_resistance"), new Entry("minecraft:splash_potion", 1, 0, 1, 0, "speed"), new Entry("minecraft:glass_bottle", 1, 0, 7, 0), new Entry("minecraft:glowstone_dust", 1, 0, 15, 0), new Entry("minecraft:gunpowder", 1, 0, 15, 0), new Entry("minecraft:redstone", 1, 0, 15, 0), new Entry("minecraft:spider_eye", 1, 0, 15, 0), new Entry("minecraft:stick", 1, 0, 15, 0), new Entry("minecraft:sugar", 1, 0, 15, 0))),
                Map.entry("wither_skeleton", List.of(new Entry("minecraft:wither_skeleton_skull", 1, 0, 2, 0), new Entry("minecraft:bone", 1, 0, 49, 0), new Entry("minecraft:coal", 2, 0, 49, 0))),
                Map.entry("zombie", List.of(new Entry("minecraft:zombie_head", 1, 0, 2, 0), new Entry("minecraft:rotten_flesh", 1, 0, 98, 0))));
    }

    private static String entityPath(String entityId) {
        var separator = entityId == null ? -1 : entityId.indexOf(':');
        return separator < 0 ? entityId : entityId.substring(separator + 1);
    }
}
