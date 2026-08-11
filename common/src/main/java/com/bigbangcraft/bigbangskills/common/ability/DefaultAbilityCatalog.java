package com.bigbangcraft.bigbangskills.common.ability;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Baseline ability metadata derived from the fixed mcMMO enum and skillranks files. */
public final class DefaultAbilityCatalog {
    private DefaultAbilityCatalog() {}

    public static Map<SkillId, List<AbilityDefinition>> all() { return Holder.ALL; }

    private static Map<SkillId, List<AbilityDefinition>> load() {
        try (var input = DefaultAbilityCatalog.class.getClassLoader().getResourceAsStream("bigbangskills/abilities.properties")) {
            if (input == null) throw new IllegalStateException("Missing ability catalog");
            var ranks = loadRanks();
            var result = new HashMap<SkillId, List<AbilityDefinition>>();
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(line -> {
                    var fields = line.split("\\|", -1);
                    if (fields.length != 6) throw new IllegalArgumentException("Invalid ability catalog line: " + line);
                    var skill = SkillId.parse("bigbangskills:" + fields[0]);
                    var rankLevels = ranks.getOrDefault(fields[0] + "|" + fields[1], List.of(Integer.parseInt(fields[2])));
                    var definition = new AbilityDefinition(fields[0] + "." + fields[1], skill,
                            AbilityType.valueOf(fields[3]), rankLevels.getFirst(),
                            Duration.ofSeconds(Long.parseLong(fields[4])), Duration.ofSeconds(Long.parseLong(fields[5])),
                            rankLevels);
                    result.computeIfAbsent(skill, ignored -> new ArrayList<>()).add(definition);
                });
            }
            return result.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load ability catalog", failure);
        }
    }

    private static Map<String, List<Integer>> loadRanks() {
        try (var input = DefaultAbilityCatalog.class.getClassLoader().getResourceAsStream("bigbangskills/ability-ranks.properties")) {
            if (input == null) throw new IllegalStateException("Missing ability rank catalog");
            var result = new HashMap<String, List<Integer>>();
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(line -> {
                    var fields = line.split("\\|", -1);
                    if (fields.length != 3) throw new IllegalArgumentException("Invalid ability rank line: " + line);
                    var levels = java.util.Arrays.stream(fields[2].split(",")) .map(Integer::parseInt).toList();
                    result.put(fields[0] + "|" + fields[1], levels);
                });
            }
            return Map.copyOf(result);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load ability rank catalog", failure);
        }
    }

    private static final class Holder {
        private static final Map<SkillId, List<AbilityDefinition>> ALL = load();
    }
}
