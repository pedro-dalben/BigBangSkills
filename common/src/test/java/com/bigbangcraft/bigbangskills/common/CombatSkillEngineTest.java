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

    @Test void arrowRetrievalUsesReferenceLevelCap() {
        var progress = new PlayerProgress(UUID.randomUUID());
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:archery"), BigDecimal.ZERO, 1000, 0));
        assertTrue(new CombatSkillEngine(() -> 0.0).arrowRetrieval(progress));
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
        var impale = engine.resolve(progress, new CombatAction(player, tridents, "minecraft:trident", BigDecimal.ONE, 8, 1,
                false, false, false, ProgressionScope.server("test")));
        assertEquals(6.0, impale.effect().bonusDamage(), 0.0001);
        var steelArm = engine.resolve(progress, new CombatAction(player, unarmed, "minecraft:air", BigDecimal.ONE, 8, 1,
                false, false, false, ProgressionScope.server("test")));
        assertEquals(13.5, steelArm.effect().bonusDamage(), 0.0001);
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
