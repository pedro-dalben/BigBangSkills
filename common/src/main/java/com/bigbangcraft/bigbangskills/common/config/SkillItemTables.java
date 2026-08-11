package com.bigbangcraft.bigbangskills.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Data-driven item rules for skills whose input is an item rather than a block. */
public final class SkillItemTables {
    public record SalvageRule(String resultId, int maximumQuantity, int maximumDurability, int minimumLevel) {
        public SalvageRule {
            if (resultId == null || resultId.isBlank() || maximumQuantity < 1 || maximumDurability < 1 || minimumLevel < 0) {
                throw new IllegalArgumentException("Invalid salvage rule");
            }
        }
    }

    private static final Pattern REGISTRY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Map<String, SalvageRule> salvage;
    private final Map<String, String> repairMaterials;

    private SkillItemTables(Map<String, SalvageRule> salvage, Map<String, String> repairMaterials) {
        this.salvage = Map.copyOf(salvage);
        this.repairMaterials = Map.copyOf(repairMaterials);
    }

    public static SkillItemTables defaults() {
        return new SkillItemTables(loadResource("bigbangskills/salvage.properties"), loadRepairResource());
    }

    public static SkillItemTables loadOrCreate(Path file) {
        var defaults = defaults().salvage;
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) {
                var out = new StringBuilder("# item_id=result_id|maximum_quantity|maximum_durability|minimum_level\n");
                defaults.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    var rule = entry.getValue();
                    out.append(entry.getKey()).append('=').append(rule.resultId()).append('|').append(rule.maximumQuantity()).append('|').append(rule.maximumDurability()).append('|').append(rule.minimumLevel()).append('\n');
                });
                Files.writeString(file, out, StandardCharsets.UTF_8);
            return new SkillItemTables(defaults, loadOrCreateRepair(file.resolveSibling("repair.properties")));
            }
            var values = new HashMap<>(defaults);
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                var fields = separator > 0 && separator < valueLine.length() - 1 ? valueLine.substring(separator + 1).split("\\|", -1) : new String[0];
                if (separator <= 0 || fields.length != 4) throw new IllegalArgumentException("Invalid salvage line: " + line);
                var itemId = valueLine.substring(0, separator).trim();
                var resultId = fields[0].trim();
                if (!REGISTRY_ID.matcher(itemId).matches() || !REGISTRY_ID.matcher(resultId).matches()) throw new IllegalArgumentException("Invalid salvage registry ID: " + line);
                values.put(itemId, new SalvageRule(resultId, Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3])));
            }
            return new SkillItemTables(values, loadOrCreateRepair(file.resolveSibling("repair.properties")));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load salvage table: " + file, failure);
        }
    }

    public Map<String, SalvageRule> salvage() { return salvage; }
    public SalvageRule salvageRule(String itemId) { return salvage.get(itemId); }
    public String repairMaterial(String itemId) { return repairMaterials.get(itemId); }

    private static Map<String, String> loadOrCreateRepair(Path file) {
        var defaults = loadRepairResource();
        try {
            Files.createDirectories(file.toAbsolutePath().normalize().getParent());
            if (!Files.exists(file)) {
                var out = new StringBuilder("# item_id=repair_xp_category\n");
                defaults.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));
                Files.writeString(file, out, StandardCharsets.UTF_8);
                return defaults;
            }
            var values = new HashMap<>(defaults);
            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                var itemId = separator > 0 ? valueLine.substring(0, separator).trim() : "";
                var category = separator > 0 ? valueLine.substring(separator + 1).trim() : "";
                if (!REGISTRY_ID.matcher(itemId).matches() || category.isBlank()) throw new IllegalArgumentException("Invalid repair line: " + line);
                values.put(itemId, category);
            }
            return values;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load repair table: " + file, failure);
        }
    }

    private static Map<String, String> loadRepairResource() {
        try (var input = SkillItemTables.class.getClassLoader().getResourceAsStream("bigbangskills/repair.properties")) {
            if (input == null) throw new IllegalStateException("Missing item table: bigbangskills/repair.properties");
            var values = new HashMap<String, String>();
            for (var line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                if (separator <= 0 || separator == valueLine.length() - 1) throw new IllegalArgumentException("Invalid repair line: " + line);
                values.put(valueLine.substring(0, separator).trim(), valueLine.substring(separator + 1).trim());
            }
            return values;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load repair table", failure);
        }
    }

    private static Map<String, SalvageRule> loadResource(String resource) {
        try (var input = SkillItemTables.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing item table: " + resource);
            var values = new HashMap<String, SalvageRule>();
            for (var line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                var valueLine = line.trim();
                if (valueLine.isEmpty() || valueLine.startsWith("#")) continue;
                var separator = valueLine.indexOf('=');
                var fields = valueLine.substring(separator + 1).split("\\|", -1);
                values.put(valueLine.substring(0, separator), new SalvageRule(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3])));
            }
            return values;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load item table: " + resource, failure);
        }
    }
}
