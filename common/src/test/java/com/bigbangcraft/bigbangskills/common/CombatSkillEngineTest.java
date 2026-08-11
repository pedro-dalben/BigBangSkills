package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.CombatAction;
import com.bigbangcraft.bigbangskills.common.skill.CombatSkillEngine;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CombatSkillEngineTest {
    @Test void swordsResolveConfiguredPassiveAndActiveEffects() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        var curve = DefaultSkills.registry().get(skill).orElseThrow().curve();
        progress.put(new SkillProgress(skill, curve.totalXpForLevel(100), 100, 0));
        var engine = new CombatSkillEngine(() -> 0.0);
        var result = engine.resolve(progress, new CombatAction(player, skill, "minecraft:diamond_sword", BigDecimal.ONE,
                10, 1, false, false, true, ProgressionScope.server("test")));
        assertTrue(result.award().amount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(1.0, result.effect().damageMultiplier());
        assertEquals(4.0, result.effect().bonusDamage(), 0.0001);
        assertEquals(2.5, result.effect().aoeDamage(), 0.0001);
        assertTrue(result.effect().rupture());
        assertEquals(1.0, result.effect().ruptureTickDamage(), 0.0001);
        assertEquals(100, result.effect().ruptureDurationTicks());
    }

    @Test void ruptureUsesCommittedAttackStrengthAndReferenceTickDamage() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var weak = new CombatSkillEngine(() -> 0.5).resolve(progress, new CombatAction(player, skill,
                "minecraft:diamond_sword", BigDecimal.ONE, 10, .25, false, false, false, ProgressionScope.server("test")));
        assertFalse(weak.effect().rupture());
        var full = new CombatSkillEngine(() -> 0.0).resolve(progress, new CombatAction(player, skill,
                "minecraft:diamond_sword", BigDecimal.ONE, 10, 1, false, false, false, ProgressionScope.server("test")));
        assertEquals(1.0, full.effect().ruptureTickDamage(), .0001);
    }

    @Test void ruptureDurationUsesPvpAndPveFormulaOverrides() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-rupture", ".properties");
        java.nio.file.Files.writeString(file, "combat.swords.rupture_duration_ticks_pvp=40\ncombat.swords.rupture_duration_ticks_pve=60\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var engine = new CombatSkillEngine(formulas, () -> 0.0);
        assertEquals(40, engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_sword", BigDecimal.ONE,
                10, 1, true, false, false, ProgressionScope.server("test"))).effect().ruptureDurationTicks());
        assertEquals(60, engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_sword", BigDecimal.ONE,
                10, 1, false, false, false, ProgressionScope.server("test"))).effect().ruptureDurationTicks());
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void serratedStrikesUsesConfiguredDamageDivisor() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-serrated", ".properties");
        java.nio.file.Files.writeString(file, "combat.swords.serrated_strikes_damage_divisor=2\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(formulas, () -> 0.0).resolve(progress, new CombatAction(player, skill,
                "minecraft:iron_sword", BigDecimal.ONE, 10, 1, false, false, true, ProgressionScope.server("test")));
        assertEquals(5.0, result.effect().aoeDamage(), 0.0001);
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void ruptureRespectsConfiguredChanceCap() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-rupture-cap", ".properties");
        java.nio.file.Files.writeString(file, "combat.swords.rupture_max_percent=10\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(formulas, () -> 0.5).resolve(progress, new CombatAction(player, skill,
                "minecraft:iron_sword", BigDecimal.ONE, 10, 1, false, false, false, ProgressionScope.server("test")));
        assertFalse(result.effect().rupture());
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void skullSplitterUsesConfiguredDamageDivisor() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-skull-splitter", ".properties");
        java.nio.file.Files.writeString(file, "combat.axes.skull_splitter_damage_divisor=5\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(formulas, () -> 0.0).resolve(progress, new CombatAction(player, skill,
                "minecraft:iron_axe", BigDecimal.ONE, 10, 1, false, false, true, ProgressionScope.server("test")));
        assertEquals(2.0, result.effect().aoeDamage(), 0.0001);
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void archeryForceUsesConfiguredXpMultiplierAndCap() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-archery-force", ".properties");
        java.nio.file.Files.writeString(file, "combat.archery.force_multiplier=2\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var engine = new CombatSkillEngine(formulas, () -> 0.0);
        assertEquals(0.5, engine.archeryForceXpMultiplier(.25), 0.0001);
        assertEquals(1.0, engine.archeryForceXpMultiplier(.75), 0.0001);
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void momentumUsesConfiguredRankChance() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-momentum", ".properties");
        java.nio.file.Files.writeString(file, "combat.spears.momentum_chance_rank_10=40\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:spears");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(formulas, () -> 0.45).resolve(progress, new CombatAction(player, skill,
                "mod:spear", BigDecimal.ONE, 10, 1, false, false, false, ProgressionScope.server("test")));
        assertFalse(result.effect().momentum());
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void combatXpUsesDamageAndReferenceCeiling() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-combat-xp", ".properties");
        java.nio.file.Files.writeString(file, "combat.xp_damage_ceiling=5\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:swords");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var engine = new CombatSkillEngine(formulas, () -> .999);
        var capped = engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_sword", BigDecimal.valueOf(20),
                10, 1, true, false, false, ProgressionScope.server("test")));
        var small = engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_sword", BigDecimal.valueOf(20),
                2, 1, true, false, false, ProgressionScope.server("test")));
        assertEquals(0, capped.award().amount().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, small.award().amount().compareTo(BigDecimal.valueOf(40)));
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void pvpAndWeaponSkillsShareOneResolutionPath() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 50, 0));
        var result = new CombatSkillEngine(() -> 0.0).resolve(progress, new CombatAction(player, skill,
                "minecraft:iron_axe", BigDecimal.valueOf(20), 8, 1, true, true, false, ProgressionScope.server("test")));
        assertTrue(result.award().pvp());
        assertTrue(result.effect().bonusDamage() > 0);
    }

    @Test void defensiveCombatAbilitiesUseReferenceChanceCaps() {
        var player = UUID.randomUUID();
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:unarmed"), BigDecimal.ZERO, 100, 0));
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:swords"), BigDecimal.ZERO, 100, 0));
        var engine = new CombatSkillEngine(() -> 0.0);
        assertTrue(engine.arrowDeflect(progress));
        assertTrue(engine.ironGrip(progress));
        assertEquals(5.0, engine.counterAttackDamage(progress, 10), 0.0001);
    }

    @Test void limitBreakAddsRankedPvPDamageButNotDefaultPvEDamage() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var engine = new CombatSkillEngine(() -> 0.0);
        var pvp = engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_axe", BigDecimal.ONE,
                8, 1, true, false, false, ProgressionScope.server("test")));
        var pve = engine.resolve(progress, new CombatAction(player, skill, "minecraft:iron_axe", BigDecimal.ONE,
                8, 1, false, false, false, ProgressionScope.server("test")));
        assertTrue(pvp.effect().bonusDamage() > pve.effect().bonusDamage());
    }

    @Test void pveLimitBreakUsesUnarmoredMobQualityWithoutPvpReduction() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-limit-break", ".properties");
        java.nio.file.Files.writeString(file, "combat.limit_break_allow_pve=1\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 10, 0));
        var result = new CombatSkillEngine(formulas, com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> .999).resolve(progress,
                new CombatAction(player, skill, "minecraft:iron_axe", BigDecimal.ONE, 8, 1, false, false, false,
                        ProgressionScope.server("test")));
        assertEquals(3.0, result.effect().bonusDamage(), 0.0001);
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void limitBreakTruncatesBeforeAttackStrengthScaling() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 10, 0));
        var result = new CombatSkillEngine(() -> .999).resolve(progress, new CombatAction(player, skill,
                "minecraft:iron_axe", BigDecimal.ONE, 8, .5, true, true, false,
                ProgressionScope.server("test")));
        assertEquals(1.5, result.effect().bonusDamage(), 0.0001);
    }

    @Test void arrowRetrievalUsesReferenceLevelCap() {
        var progress = new PlayerProgress(UUID.randomUUID());
        var skill = SkillId.parse("bigbangskills:archery");
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        assertTrue(new CombatSkillEngine(() -> .99).arrowRetrieval(progress));
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 50, 0));
        assertFalse(new CombatSkillEngine(() -> .51).arrowRetrieval(progress));
    }

    @Test void archeryDistanceMultiplierUsesReferenceCap() {
        var engine = new CombatSkillEngine();
        assertEquals(1.0, engine.archeryDistanceXpMultiplier(0), 0.0001);
        assertEquals(2.25, engine.archeryDistanceXpMultiplier(50), 0.0001);
        assertEquals(2.25, engine.archeryDistanceXpMultiplier(100), 0.0001);
    }

    @Test void combatProcBoundariesMatchReferenceFormulas() {
        var player = UUID.randomUUID();
        var axes = SkillId.parse("bigbangskills:axes");
        var tridents = SkillId.parse("bigbangskills:tridents");
        var unarmed = SkillId.parse("bigbangskills:unarmed");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(axes, BigDecimal.ZERO, 100, 0));
        progress.put(new SkillProgress(tridents, BigDecimal.ZERO, 1000, 0));
        progress.put(new SkillProgress(unarmed, BigDecimal.ZERO, 1000, 0));
        var engine = new CombatSkillEngine(() -> 0.0);
        var greater = engine.resolve(progress, new CombatAction(player, axes, "minecraft:iron_axe", BigDecimal.ONE, 8, 1,
                false, false, false, ProgressionScope.server("test")));
        assertTrue(greater.effect().greaterImpact());
        var armoredGreater = engine.resolve(progress, new CombatAction(player, axes, "minecraft:iron_axe", BigDecimal.ONE, 8, 1,
                true, true, 13, false, ProgressionScope.server("test")));
        assertTrue(armoredGreater.effect().greaterImpact());
        var impale = engine.resolve(progress, new CombatAction(player, tridents, "minecraft:trident", BigDecimal.ONE, 8, 1,
                false, false, false, ProgressionScope.server("test")));
        assertEquals(6.0, impale.effect().bonusDamage(), 0.0001);
        var steelArm = engine.resolve(progress, new CombatAction(player, unarmed, "minecraft:air", BigDecimal.ONE, 8, 1,
                false, false, false, ProgressionScope.server("test")));
        assertEquals(13.5, steelArm.effect().bonusDamage(), 0.0001);
    }

    @Test void steelArmDamageOverrideUsesConfiguredRank(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        java.nio.file.Files.writeString(directory.resolve("formulas.properties"), "combat.unarmed.steel_arm_damage_override=1\ncombat.unarmed.steel_arm_override_rank_20=42\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(directory.resolve("formulas.properties"));
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:unarmed");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 1000, 0));
        var result = new CombatSkillEngine(formulas, () -> 0.0).resolve(progress, new CombatAction(player, skill,
                "minecraft:air", BigDecimal.ONE, 8, 1, false, false, false, ProgressionScope.server("test")));
        assertEquals(42.0, result.effect().bonusDamage(), 0.0001);
    }

    @Test void macesCrippleUsesReferenceRankFourCap() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:maces");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var action = new CombatAction(player, skill, "minecraft:mace", BigDecimal.ONE, 8, 1,
                true, false, false, ProgressionScope.server("test"));
        assertFalse(new CombatSkillEngine(() -> .34).resolve(progress, action).effect().cripple());
        assertTrue(new CombatSkillEngine(() -> .32).resolve(progress, action).effect().cripple());
    }

    @Test void unarmedDisarmUsesConfiguredMaxLevel(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        java.nio.file.Files.writeString(directory.resolve("formulas.properties"), "combat.unarmed.disarm_max_level=200\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(directory.resolve("formulas.properties"));
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:unarmed");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 150, 0));
        var action = new CombatAction(player, skill, "minecraft:air", BigDecimal.ONE, 8, 1,
                true, false, false, ProgressionScope.server("test"));
        assertFalse(new CombatSkillEngine(formulas, () -> .30).resolve(progress, action).effect().disarm());
    }

    @Test void tamedCombatXpUsesConfiguredMultiplier(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        java.nio.file.Files.writeString(directory.resolve("formulas.properties"), "combat.tamed_mob_xp_multiplier=0.25\n");
        var formulas = com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(directory.resolve("formulas.properties"));
        var result = new CombatSkillEngine(formulas).tamedCombatXp(BigDecimal.valueOf(20), true);
        assertEquals(0, result.compareTo(BigDecimal.valueOf(5)));
        assertEquals(0, new CombatSkillEngine(formulas).tamedCombatXp(BigDecimal.valueOf(20), false).compareTo(BigDecimal.valueOf(20)));
    }

    @Test void disabledPvpPolicyCannotApplyCombatEffects() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-combat", ".properties");
        java.nio.file.Files.writeString(file, "skill.axes.pvp=false\n");
        var config = com.bigbangcraft.bigbangskills.common.config.SkillConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(), config, () -> 0.0)
                .resolve(progress, new CombatAction(player, skill, "minecraft:iron_axe", BigDecimal.ONE, 8, 1, true, false, false,
                        ProgressionScope.server("test")));
        assertEquals(0, result.effect().bonusDamage());
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void secondaryCombatTargetsRespectPvpSpectatorAndPetRules() {
        assertTrue(CombatSkillEngine.secondaryTargetAllowed(false, false, false, false));
        assertTrue(CombatSkillEngine.secondaryTargetAllowed(true, true, false, false));
        assertFalse(CombatSkillEngine.secondaryTargetAllowed(true, false, false, false));
        assertFalse(CombatSkillEngine.secondaryTargetAllowed(true, true, true, false));
        assertFalse(CombatSkillEngine.secondaryTargetAllowed(false, true, false, true));
    }

    @Test void disablingActiveAbilitiesKeepsPassiveCombatEffects() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-passives", ".properties");
        java.nio.file.Files.writeString(file, "skill.axes.abilities_enabled=false\n");
        var config = com.bigbangcraft.bigbangskills.common.config.SkillConfig.loadOrCreate(file);
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:axes");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var result = new CombatSkillEngine(com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(), config, () -> 0.0)
                .resolve(progress, new CombatAction(player, skill, "minecraft:iron_axe", BigDecimal.ONE, 8, 1,
                        false, false, false, ProgressionScope.server("test")));
        assertTrue(result.effect().bonusDamage() > 0);
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void everyCombatPrimarySkillUsesTheSharedDispatcher() {
        var player = UUID.randomUUID();
        var progress = new PlayerProgress(player);
        var engine = new CombatSkillEngine(() -> 0.0);
        var weapons = java.util.Map.of(
                "archery", "minecraft:bow", "axes", "minecraft:iron_axe", "crossbows", "minecraft:crossbow",
                "maces", "minecraft:mace", "spears", "minecraft:iron_sword", "swords", "minecraft:iron_sword",
                "tridents", "minecraft:trident", "unarmed", "minecraft:air");
        for (var entry : weapons.entrySet()) {
            var skill = SkillId.parse("bigbangskills:" + entry.getKey());
            progress.put(new SkillProgress(skill, BigDecimal.ZERO, 1000, 0));
            var result = engine.resolve(progress, new CombatAction(player, skill, entry.getValue(), BigDecimal.ONE,
                    8, 1, false, false, false, ProgressionScope.server("test")));
            assertEquals(skill, result.award().skillId());
            assertTrue(result.award().amount().signum() > 0);
        }
    }
}
