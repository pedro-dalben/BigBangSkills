package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.config.ProgressionConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillXpTables;
import com.bigbangcraft.bigbangskills.api.SkillId;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test void packagedFormulaDefaultsMatchTheValidatedConfigSchema() throws Exception {
        var defaults = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults();
        var properties = new java.util.Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("bigbangskills/formulas.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        defaults.values().forEach((key, value) -> {
            assertTrue(properties.containsKey(key), key);
            assertEquals(value, Double.parseDouble(properties.getProperty(key)), 0.000001, key);
        });
        assertEquals(defaults.salvageAnvilBlock(), properties.getProperty("salvage.anvil_block"));
    }

    @Test void formulaConfigMigrationWritesNewDefaultsAndKeepsOverrides(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("formulas.properties");
        Files.writeString(file, "mining.blast_bonus_drop_chance=73\n");
        var loaded = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        assertEquals(73.0, loaded.value("mining.blast_bonus_drop_chance"));
        assertTrue(Files.readString(file).contains("fishing.master_angler_min_wait_per_rank=10.0"));
    }

    @Test void defaultsAreValidAndImmutable() {
        var config = ProgressionConfig.defaults();
        assertEquals(BigDecimal.ONE, config.globalMultiplier());
        assertThrows(UnsupportedOperationException.class, () -> config.blockXp().put("bad", BigDecimal.ONE));
    }
    @Test void negativeValuesFailValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressionConfig(BigDecimal.ONE.negate(), BigDecimal.ONE, true, Map.of()));
    }

    @Test void skillConfigCreatesAndValidatesExternalSettings(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        var defaults = SkillConfig.loadOrCreate(file);
        assertEquals(19, defaults.rules().size());
        assertEquals("LINEAR", defaults.experienceCurve());
        assertEquals(1020, defaults.linearBase());
        assertEquals(20, defaults.linearMultiplier());
        assertEquals(2000, defaults.exponentialBase());
        assertEquals(new BigDecimal("0.1"), defaults.exponentialMultiplier());
        assertEquals(new BigDecimal("1.80"), defaults.exponentialExponent());
        assertEquals(0, defaults.rule(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining")).levelCap());
        assertFalse(defaults.abilityOnlyWhenSneaking());
        Files.writeString(file, "experience.curve=linear\nexperience.linear_base=100\nexperience.linear_multiplier=5\nskill.mining.xp_multiplier=2\nskill.mining.level_cap=80\n");
        var loaded = SkillConfig.loadOrCreate(file);
        assertEquals("LINEAR", loaded.experienceCurve());
        assertEquals(100, loaded.linearBase());
        assertEquals(5, loaded.linearMultiplier());
        var mining = loaded.rule(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining"));
        assertEquals(BigDecimal.valueOf(2), mining.xpMultiplier());
        assertEquals(80, mining.levelCap());
        assertEquals(BigDecimal.valueOf(1275), com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(loaded)
                .get(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining")).orElseThrow().curve().totalXpForLevel(2));
        assertFalse(loaded.abilityOnlyWhenSneaking());
        assertTrue(Files.readString(file).contains("schema_version=4"));
        Files.writeString(file, "abilities.only_activate_when_sneaking=true\n");
        assertTrue(SkillConfig.loadOrCreate(file).abilityOnlyWhenSneaking());
        Files.writeString(file, "skill.mining.enabled=maybe\n");
        assertThrows(IllegalArgumentException.class, () -> SkillConfig.loadOrCreate(file));
    }

    @Test void unsupportedProgressionCurveFailsClosed(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "experience.curve=quadratic\n");
        assertThrows(IllegalArgumentException.class, () -> SkillConfig.loadOrCreate(file));
    }

    @Test void exponentialProgressionUsesReferenceStandardGrouping(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "experience.curve=EXPONENTIAL\n");
        var config = SkillConfig.loadOrCreate(file);
        var curve = com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config)
                .get(SkillId.parse("bigbangskills:mining")).orElseThrow().curve();
        assertEquals(BigDecimal.valueOf(20022), curve.totalXpForLevel(2));
        assertEquals(2, curve.levelAt(BigDecimal.valueOf(20022), 100));
    }

    @Test void configuredAbilityCooldownIsShownByGenericSkillDetails(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "skill.axes.ability_cooldown_seconds=17\n");
        var config = SkillConfig.loadOrCreate(file);
        var player = java.util.UUID.randomUUID();
        var progress = new com.bigbangcraft.bigbangskills.common.progression.PlayerProgress(player);
        var skill = SkillId.parse("bigbangskills:axes");
        var curve = com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config).get(skill).orElseThrow().curve();
        progress.put(new com.bigbangcraft.bigbangskills.common.progression.SkillProgress(skill, curve.totalXpForLevel(100), 100, 0));
        assertTrue(com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter.skill(progress,
                com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config), "axes", java.util.Locale.US, config)
                .stream().anyMatch(line -> line.contains("17s")));
        var details = com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter.skill(progress,
                com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config), "axes", java.util.Locale.US, config);
        assertTrue(details.stream().anyMatch(line -> line.contains("Activation:")));
        assertTrue(details.stream().anyMatch(line -> line.contains("Restrictions:")));
        assertTrue(details.stream().anyMatch(line -> line.contains("XP formula:")));
    }

    @Test void acrobaticsFeatherFallingMultiplierComesFromActionTable() {
        assertEquals(BigDecimal.valueOf(2.0), SkillXpTables.defaults().xpForAction(
                SkillId.parse("bigbangskills:acrobatics"), "featherfall_multiplier"));
    }

    @Test void baselineAbilityCooldownsUseCatalogValuesBeforeSkillOverride() throws Exception {
        var blast = com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog.all()
                .get(SkillId.parse("bigbangskills:mining")).stream()
                .filter(value -> value.id().equals("mining.blast_mining")).findFirst().orElseThrow();
        assertEquals(java.time.Duration.ofSeconds(60), SkillConfig.defaults().abilityCooldown(blast));
        var file = java.nio.file.Files.createTempFile("bigbangskills-cooldown", ".properties");
        java.nio.file.Files.writeString(file, "skill.mining.ability_cooldown_seconds=17\n");
        assertEquals(java.time.Duration.ofSeconds(17), SkillConfig.loadOrCreate(file).abilityCooldown(blast));
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void explicitCooldownOverrideCanMatchCatalogDefaultValue(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var blast = com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog.all()
                .get(SkillId.parse("bigbangskills:mining")).stream()
                .filter(value -> value.id().equals("mining.blast_mining")).findFirst().orElseThrow();
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "skill.mining.ability_cooldown_override_seconds=240\n");
        var config = SkillConfig.loadOrCreate(file);
        assertEquals(java.time.Duration.ofSeconds(240), config.abilityCooldown(blast));
        assertTrue(Files.readString(file).contains("ability_cooldown_override_seconds=240"));
    }

    @Test void legacyGeneratedHundredLevelCapsMigrateButCustomCapsDoNot(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        var legacy = new StringBuilder();
        for (var skill : SkillConfig.defaults().rules().keySet()) legacy.append("skill.").append(skill.path()).append(".level_cap=100\n");
        var allHundred = legacy.toString();
        Files.writeString(file, allHundred);
        assertEquals(0, SkillConfig.loadOrCreate(file).rule(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining")).levelCap());
        Files.writeString(file, legacy.append("skill.mining.level_cap=80\n"));
        assertEquals(80, SkillConfig.loadOrCreate(file).rule(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining")).levelCap());
        Files.writeString(file, "schema_version=2\n" + allHundred);
        assertEquals(100, SkillConfig.loadOrCreate(file).rule(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining")).levelCap());
    }

    @Test void moddedBlockXpCanBeAddedWithoutRecompile(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var tables = SkillXpTables.loadOrCreate(directory);
        Files.writeString(directory.resolve("mining-xp.properties"), "modded:mythril_ore=777\n");
        var updated = SkillXpTables.loadOrCreate(directory);
        var mining = com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining");
        assertEquals(BigDecimal.valueOf(777), updated.xpFor(mining, "modded:mythril_ore"));
        assertEquals(BigDecimal.valueOf(2400), tables.xpFor(mining, "minecraft:diamond_ore"));
        Files.writeString(directory.resolve("actions-xp.properties"), "fishing|cod=321\n");
        assertEquals(BigDecimal.valueOf(321), SkillXpTables.loadOrCreate(directory).xpForAction(
                com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:fishing"), "cod"));
    }

    @Test void moddedCombatEntityXpCanUseNamespacedAction(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("actions-xp.properties");
        java.nio.file.Files.writeString(file, "combat|multiplier.mymod:crystal_beast=77\n");
        var tables = SkillXpTables.loadOrCreate(directory);
        assertEquals(java.math.BigDecimal.valueOf(77), tables.xpForAction(SkillId.parse("bigbangskills:combat"), "multiplier.mymod:crystal_beast"));
    }

    @Test void globalAndPvpXpSettingsAreLoaded(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "experience.global_xp_multiplier=1.5\nexperience.pvp_xp_multiplier=0.25\nexperience.pvp_rewards=false\n");
        var config = SkillConfig.loadOrCreate(file);
        assertEquals(new BigDecimal("1.5"), config.globalXpMultiplier());
        assertEquals(new BigDecimal("0.25"), config.pvpXpMultiplier());
        assertFalse(config.pvpRewards());
    }

    @Test void globalAndPvpMultipliersApplyInOrder(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "experience.global_xp_multiplier=1.5\nexperience.pvp_xp_multiplier=0.25\n");
        var config = SkillConfig.loadOrCreate(file);
        var player = java.util.UUID.randomUUID();
        var result = new com.bigbangcraft.bigbangskills.common.skill.GameplayService(
                com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config), SkillXpTables.defaults(), config).award(
                new com.bigbangcraft.bigbangskills.common.progression.PlayerProgress(player),
                new com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction(player,
                        com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining"), BigDecimal.valueOf(100),
                        com.bigbangcraft.bigbangskills.api.XpSource.INTEGRATION, "test", com.bigbangcraft.bigbangskills.api.ProgressionScope.server("test"),
                        true, false, true, false));
        assertEquals(new BigDecimal("37.5"), result.amount());
    }

    @Test void perSkillXpMultiplierIsAppliedExactlyOnce(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var file = directory.resolve("skills.properties");
        Files.writeString(file, "skill.mining.xp_multiplier=2\n");
        var config = SkillConfig.loadOrCreate(file);
        var player = java.util.UUID.randomUUID();
        var result = new com.bigbangcraft.bigbangskills.common.skill.GameplayService(
                com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config), SkillXpTables.defaults(), config).award(
                new com.bigbangcraft.bigbangskills.common.progression.PlayerProgress(player),
                new com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction(player,
                        com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining"), BigDecimal.valueOf(100),
                        com.bigbangcraft.bigbangskills.api.XpSource.INTEGRATION, "test",
                        com.bigbangcraft.bigbangskills.api.ProgressionScope.server("test"), true, false, false, true));
        assertTrue(result.accepted());
        assertEquals(0, result.amount().compareTo(BigDecimal.valueOf(200)));
    }

    @Test void disabledSkillRejectsAwardsAtTheSharedGameplayBoundary(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("skills.properties"), "skill.mining.enabled=false\n");
        var config = SkillConfig.loadOrCreate(directory.resolve("skills.properties"));
        var player = java.util.UUID.randomUUID();
        var result = new com.bigbangcraft.bigbangskills.common.skill.GameplayService(
                com.bigbangcraft.bigbangskills.common.skill.DefaultSkills.registry(config),
                SkillXpTables.defaults(), config).award(
                new com.bigbangcraft.bigbangskills.common.progression.PlayerProgress(player),
                new com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction(
                        player, com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining"),
                        BigDecimal.ONE, com.bigbangcraft.bigbangskills.api.XpSource.INTEGRATION, "test",
                        com.bigbangcraft.bigbangskills.api.ProgressionScope.server("test"), true, false, false, true));
        assertFalse(result.accepted());
        assertEquals("skill_disabled", result.reason());
    }
}
