package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.AcrobaticsEngine;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkillFormulaAndAcrobaticsTest {
    @Test void formulaConfigRejectsUnknownKeysAndKeepsDefaults() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-formulas", ".properties");
        java.nio.file.Files.writeString(file, "combat.unarmed.berserk_multiplier=2.0\n");
        assertEquals(2.0, SkillFormulaConfig.loadOrCreate(file).value("combat.unarmed.berserk_multiplier"));
        java.nio.file.Files.writeString(file, "unknown.value=1\n");
        assertThrows(IllegalArgumentException.class, () -> SkillFormulaConfig.loadOrCreate(file));
        java.nio.file.Files.writeString(file, "salvage.arcane_salvage_max_enchant=3\n");
        assertEquals(3.0, SkillFormulaConfig.loadOrCreate(file).value("salvage.arcane_salvage_max_enchant"));
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void rollCanCancelAQualifyingFallAtTheSharedBoundary() {
        var id = SkillId.parse("bigbangskills:acrobatics");
        var progress = new PlayerProgress(UUID.randomUUID());
        var curve = DefaultSkills.registry().get(id).orElseThrow().curve();
        progress.put(new SkillProgress(id, curve.totalXpForLevel(100), 100, 0));
        var effect = new AcrobaticsEngine(() -> 0.0).resolve(progress, 10);
        assertTrue(effect.rollTriggered());
        assertEquals(0, effect.damageMultiplier());
        assertFalse(new AcrobaticsEngine(() -> 0.0).resolve(progress, 18, false).rollTriggered());
        assertTrue(new AcrobaticsEngine(() -> 0.0).resolve(progress, 17, true).rollTriggered());
    }

    @Test void concoctionsUseReferenceIngredientTiers() {
        var engine = new com.bigbangcraft.bigbangskills.common.skill.AlchemyEngine();
        assertEquals(6, engine.concoctionsRank(75));
        assertEquals(4, engine.concoctionTier("minecraft:apple"));
        assertEquals(1, engine.concoctionTier("minecraft:breeze_rod"));
        assertEquals(8, engine.concoctionTier("minecraft:golden_apple"));
        assertEquals(0, engine.concoctionTier("minecraft:stick"));
    }
}
