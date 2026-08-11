package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Numeric XP fixtures from the fixed mcMMO Experience_Values tree. */
public final class ReferenceExperienceTables {
    private final Map<SkillId, Map<String, BigDecimal>> values;

    private ReferenceExperienceTables(Map<SkillId, Map<String, BigDecimal>> values) { this.values = Map.copyOf(values); }

    public static ReferenceExperienceTables defaults() {
        try (var input = ReferenceExperienceTables.class.getClassLoader().getResourceAsStream("bigbangskills/experience-values.properties")) {
            if (input == null) throw new IllegalStateException("Missing experience values");
            var result = new HashMap<SkillId, Map<String, BigDecimal>>();
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(line -> {
                    var equals = line.lastIndexOf('=');
                    var pipe = line.indexOf('|');
                    if (pipe <= 0 || equals <= pipe || equals == line.length() - 1) throw new IllegalArgumentException("Invalid experience value: " + line);
                    var skill = SkillId.parse("bigbangskills:" + line.substring(0, pipe));
                    var action = line.substring(pipe + 1, equals);
                    var xp = new BigDecimal(line.substring(equals + 1));
                    if (action.isBlank() || xp.signum() < 0) throw new IllegalArgumentException("Invalid experience value: " + line);
                    result.computeIfAbsent(skill, ignored -> new HashMap<>()).put(action, xp);
                });
            }
            return new ReferenceExperienceTables(result.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue()))));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load experience values", failure);
        }
    }

    public BigDecimal xpFor(SkillId skill, String action) { return values.getOrDefault(skill, Map.of()).getOrDefault(action, BigDecimal.ZERO); }
    public Map<String, BigDecimal> values(SkillId skill) { return values.getOrDefault(skill, Map.of()); }
    public Map<SkillId, Map<String, BigDecimal>> snapshot() { return values; }
}
