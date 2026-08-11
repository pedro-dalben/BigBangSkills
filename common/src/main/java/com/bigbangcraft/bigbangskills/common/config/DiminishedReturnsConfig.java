package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Optional mcMMO-style recent-XP throttle; disabled by default for compatibility. */
public record DiminishedReturnsConfig(boolean enabled, BigDecimal guaranteedMinimumFraction,
                                      int intervalMinutes, Map<SkillId, BigDecimal> thresholds) {
    public DiminishedReturnsConfig {
        if (guaranteedMinimumFraction.signum() < 0 || guaranteedMinimumFraction.compareTo(BigDecimal.ONE) > 0 || intervalMinutes < 1) throw new IllegalArgumentException("Invalid diminished returns config");
        thresholds = Map.copyOf(thresholds);
        thresholds.values().forEach(value -> { if (value.signum() < 0) throw new IllegalArgumentException("Invalid diminished returns threshold"); });
    }

    public static DiminishedReturnsConfig defaults() {
        var thresholds = new HashMap<SkillId, BigDecimal>();
        for (var skill : new String[]{"acrobatics", "alchemy", "archery", "axes", "crossbows", "excavation", "fishing", "herbalism", "maces", "mining", "repair", "spears", "swords", "taming", "tridents", "unarmed", "woodcutting"}) thresholds.put(SkillId.parse("bigbangskills:" + skill), BigDecimal.valueOf(20_000));
        return new DiminishedReturnsConfig(false, BigDecimal.valueOf(.05), 10, thresholds);
    }

    public BigDecimal threshold(SkillId skill) { return thresholds.getOrDefault(skill, BigDecimal.ZERO); }

    public static DiminishedReturnsConfig loadOrCreate(Path file) {
        var defaults = defaults();
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) { Files.writeString(file, defaults.serialize(), StandardCharsets.UTF_8); return defaults; }
            var enabled = defaults.enabled(); var minimum = defaults.guaranteedMinimumFraction(); var interval = defaults.intervalMinutes(); var thresholds = new HashMap<>(defaults.thresholds()); var legacyFile = true;
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim(); if (value.isEmpty() || value.startsWith("#")) continue;
                var separator = value.indexOf('='); if (separator <= 0) throw new IllegalArgumentException("Invalid diminished returns line: " + line);
                var key = value.substring(0, separator).trim(); var text = value.substring(separator + 1).trim();
                switch (key) {
                    case "schema_version" -> legacyFile = !text.equals("2");
                    case "enabled" -> { if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) throw new IllegalArgumentException("Invalid enabled value"); enabled = Boolean.parseBoolean(text); }
                    case "guaranteed_minimum_fraction" -> minimum = new BigDecimal(text);
                    case "interval_minutes" -> interval = Integer.parseInt(text);
                    case "interval_seconds" -> interval = Math.max(1, (int) Math.ceil(Integer.parseInt(text) / 60.0));
                    default -> { if (!key.startsWith("threshold.")) throw new IllegalArgumentException("Unknown diminished returns key: " + key); thresholds.put(SkillId.parse("bigbangskills:" + key.substring("threshold.".length())), new BigDecimal(text)); }
                }
            }
            if (legacyFile && thresholds.values().stream().allMatch(BigDecimal.ZERO::equals)) thresholds = new HashMap<>(defaults.thresholds());
            return new DiminishedReturnsConfig(enabled, minimum, interval, thresholds);
        } catch (IOException | RuntimeException failure) { throw new IllegalStateException("Could not load diminished returns config: " + file, failure); }
    }

    private String serialize() {
        var output = new StringBuilder("# Optional recent-XP throttle; disabled by default.\n").append("schema_version=2\n");
        output.append("enabled=").append(enabled).append('\n').append("guaranteed_minimum_fraction=").append(guaranteedMinimumFraction).append('\n').append("interval_minutes=").append(intervalMinutes).append('\n');
        thresholds.forEach((skill, value) -> output.append("threshold.").append(skill.path()).append('=').append(value).append('\n'));
        return output.toString();
    }
}
