package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Immutable default tables; external config merging belongs at the loader boundary. */
public final class SkillXpTables {
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final SkillId WOODCUTTING = SkillId.parse("bigbangskills:woodcutting");
    private static final Pattern REGISTRY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern ACTION_ID = Pattern.compile("[a-z0-9_.:-]+");
    private static final java.util.Set<String> KNOWN_SKILLS = java.util.Set.of("acrobatics", "alchemy", "archery", "axes", "combat", "crossbows", "excavation", "fishing", "herbalism", "maces", "mining", "repair", "salvage", "smelting", "spears", "swords", "taming", "tridents", "unarmed", "woodcutting");
    private final Map<SkillId, Map<String, BigDecimal>> tables;
    private final Map<SkillId, Map<String, BigDecimal>> actionTables;

    private SkillXpTables(Map<SkillId, Map<String, BigDecimal>> tables, Map<SkillId, Map<String, BigDecimal>> actionTables) {
        this.tables = Map.copyOf(tables); this.actionTables = Map.copyOf(actionTables);
    }

    public static SkillXpTables defaults() {
        return new SkillXpTables(Map.of(
                MINING, load("bigbangskills/mining-xp.properties"),
                WOODCUTTING, load("bigbangskills/woodcutting-xp.properties")), ReferenceExperienceTables.defaults().snapshot());
    }

    public static SkillXpTables loadOrCreate(Path directory) {
        try {
            Files.createDirectories(directory);
            var defaults = defaults().tables;
            var defaultsActions = defaults().actionTables;
            return new SkillXpTables(Map.of(
                    MINING, external(directory.resolve("mining-xp.properties"), defaults.get(MINING)),
                    WOODCUTTING, external(directory.resolve("woodcutting-xp.properties"), defaults.get(WOODCUTTING))),
                    externalActions(directory.resolve("actions-xp.properties"), defaultsActions));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not prepare skill XP config: " + directory, failure);
        }
    }

    public BigDecimal xpFor(SkillId skillId, String blockId) {
        var direct = tables.getOrDefault(skillId, Map.of()).get(blockId);
        if (direct != null) return direct;
        var path = blockId.startsWith("minecraft:") ? blockId.substring("minecraft:".length()) : blockId;
        return actionTables.getOrDefault(skillId, Map.of()).getOrDefault(path, BigDecimal.ZERO);
    }
    public Map<String, BigDecimal> table(SkillId skillId) { return tables.getOrDefault(skillId, Map.of()); }
    public BigDecimal xpForAction(SkillId skillId, String action) {
        var table = actionTables.getOrDefault(skillId, Map.of());
        var value = table.get(action);
        if (value == null && action.contains(":")) value = table.get(action.substring(action.indexOf(':') + 1));
        return value == null ? BigDecimal.ZERO : value;
    }
    public Map<String, BigDecimal> actionTable(SkillId skillId) { return actionTables.getOrDefault(skillId, Map.of()); }

    private static Map<SkillId, Map<String, BigDecimal>> externalActions(Path file, Map<SkillId, Map<String, BigDecimal>> defaults) throws IOException {
        var values = new HashMap<SkillId, Map<String, BigDecimal>>();
        defaults.forEach((skill, entries) -> values.put(skill, new HashMap<>(entries)));
        if (!Files.exists(file)) {
            var lines = new StringBuilder("# skill|action=base XP; override action XP here.\n");
            values.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(SkillId::toString))).forEach(entry -> entry.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(value -> lines.append(entry.getKey().path()).append('|').append(value.getKey()).append('=').append(value.getValue()).append('\n')));
            Files.writeString(file, lines, StandardCharsets.UTF_8);
            return freeze(values);
        }
        for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            var valueLine = line.trim(); if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
            var separator = valueLine.lastIndexOf('='); var pipe = valueLine.indexOf('|');
            if (pipe <= 0 || separator <= pipe || separator == valueLine.length() - 1) throw new IllegalArgumentException("Invalid action XP line: " + line);
            var skill = SkillId.parse("bigbangskills:" + valueLine.substring(0, pipe).trim());
            var action = valueLine.substring(pipe + 1, separator).trim(); var xp = new BigDecimal(valueLine.substring(separator + 1).trim());
            if (!KNOWN_SKILLS.contains(skill.path()) || !ACTION_ID.matcher(action).matches() || xp.signum() < 0) throw new IllegalArgumentException("Invalid action XP line: " + line);
            values.get(skill).put(action, xp);
        }
        return freeze(values);
    }

    private static Map<SkillId, Map<String, BigDecimal>> freeze(Map<SkillId, Map<String, BigDecimal>> values) {
        return values.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }

    private static Map<String, BigDecimal> external(Path file, Map<String, BigDecimal> defaults) throws IOException {
        if (!Files.exists(file)) {
            var lines = new StringBuilder("# registry_id=base XP; add modded IDs here without recompiling.\n");
            defaults.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> lines.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));
            Files.writeString(file, lines, StandardCharsets.UTF_8);
            return defaults;
        }
        var values = new HashMap<>(defaults);
        for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            var valueLine = line.trim();
            if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
            var separator = valueLine.lastIndexOf('=');
            if (separator <= 0 || separator == valueLine.length() - 1) throw new IllegalArgumentException("Invalid XP table line: " + line);
            var id = valueLine.substring(0, separator).trim();
            var xp = new BigDecimal(valueLine.substring(separator + 1).trim());
            if (!REGISTRY_ID.matcher(id).matches() || xp.signum() < 0) throw new IllegalArgumentException("Invalid XP table entry: " + id);
            values.put(id, xp);
        }
        return Map.copyOf(values);
    }

    private static Map<String, BigDecimal> load(String resource) {
        try (InputStream input = SkillXpTables.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing skill XP resource: " + resource);
            var values = new java.util.HashMap<String, BigDecimal>();
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(line -> {
                    var separator = line.lastIndexOf('=');
                    if (separator <= 0 || separator == line.length() - 1) throw new IllegalArgumentException("Invalid XP table line: " + line);
                    var id = line.substring(0, separator).trim();
                    var xp = new BigDecimal(line.substring(separator + 1).trim());
                if (id.isBlank() || xp.signum() < 0) throw new IllegalArgumentException("Invalid XP table entry: " + id);
                values.put(id, xp);
                });
            }
            return Map.copyOf(values);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load skill XP resource: " + resource, failure);
        }
    }
}
