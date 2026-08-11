package com.bigbangcraft.bigbangskills.common.config;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Optional item-to-combat-skill mapping for modded weapons. */
public final class CombatWeaponTables {
    private final Map<String, SkillId> mappings;
    private CombatWeaponTables(Map<String, SkillId> mappings) { this.mappings = Map.copyOf(mappings); }

    public static CombatWeaponTables loadOrCreate(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) Files.writeString(file, "# item_id=skill_path (archery, axes, crossbows, maces, spears, swords, taming, tridents or unarmed)\n", StandardCharsets.UTF_8);
            var values = new java.util.HashMap<String, SkillId>();
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var value = line.trim(); if (value.isEmpty() || value.startsWith("#")) continue;
                var equals = value.indexOf('=');
                if (equals < 1 || equals == value.length() - 1) throw new IllegalArgumentException("Invalid combat weapon line: " + line);
                var item = value.substring(0, equals).trim(); var skill = value.substring(equals + 1).trim();
                if (!item.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !skill.matches("[a-z]+")) throw new IllegalArgumentException("Invalid combat weapon line: " + line);
                var id = SkillId.parse("bigbangskills:" + skill);
                if (!java.util.Set.of("archery", "axes", "crossbows", "maces", "spears", "swords", "taming", "tridents", "unarmed").contains(skill)) throw new IllegalArgumentException("Unsupported combat skill: " + skill);
                values.put(item, id);
            }
            return new CombatWeaponTables(values);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load combat weapon config: " + file, failure);
        }
    }

    public SkillId skillFor(String itemId) { return mappings.get(itemId); }
}
