package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.progression.McMmoStandardLinearXpCurve;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.config.SkillXpTables;
import com.bigbangcraft.bigbangskills.common.config.ReferenceExperienceTables;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.skill.BlastMiningEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class McMmoParityTest {
    @Test void standardLinearBoundariesMatchReferenceFormula() {
        var curve = new McMmoStandardLinearXpCurve(1020, 20);
        assertEquals(BigDecimal.ZERO, curve.totalXpForLevel(1));
        assertEquals(BigDecimal.valueOf(11300), curve.totalXpForLevel(2));
        assertEquals(BigDecimal.valueOf(24600), curve.totalXpForLevel(3));
        assertEquals(3, curve.levelAt(BigDecimal.valueOf(24600), 100));
        assertEquals(2, curve.levelAt(BigDecimal.valueOf(24599), 100));
    }

    @Test void unlimitedCapRemainsSafeAtLargeLevel() {
        var curve = new McMmoStandardLinearXpCurve(1020, 20);
        var xp = curve.totalXpForLevel(100_000);
        assertEquals(100_000, curve.levelAt(xp, Integer.MAX_VALUE));
    }

    @Test void blastMiningMatchesBaselineRanks() {
        var engine = new BlastMiningEngine();
        assertEquals(5.0F, engine.radius(1));
        assertEquals(4.0F, engine.radius(1, false));
        assertEquals(50.0, engine.damageReductionPercent(6));
        assertEquals(70.0, engine.oreBonusPercent(8));
        assertEquals(1.7, engine.oreYield(8));
        assertEquals(1.0, engine.oreYield(0));
        assertEquals(1, engine.bonusDropMultiplier(1, true));
        assertEquals(2, engine.bonusDropMultiplier(4, true));
        assertEquals(3, engine.bonusDropMultiplier(8, true));
        assertEquals(0, engine.bonusDropMultiplier(8, false));
        org.junit.jupiter.api.Assertions.assertTrue(engine.illegalDrop("minecraft:infested_stone"));
        org.junit.jupiter.api.Assertions.assertTrue(engine.illegalDrop("minecraft:budding_amethyst"));
        org.junit.jupiter.api.Assertions.assertTrue(engine.illegalDrop("minecraft:spawner"));
        org.junit.jupiter.api.Assertions.assertFalse(engine.illegalDrop("modded:my_ore"));
    }

    @Test void blastMiningRankValuesAreConfigurableWithoutChangingBaselineDefaults() throws Exception {
        var file = java.nio.file.Files.createTempFile("bigbangskills-blast", ".properties");
        java.nio.file.Files.writeString(file, "mining.blast_base_radius=5\nmining.blast_ore_bonus_rank_8=90\n");
        var engine = new BlastMiningEngine(com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.loadOrCreate(file));
        assertEquals(6.0F, engine.radius(1));
        assertEquals(90.0, engine.oreBonusPercent(8));
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test void defaultRegistryContainsReferenceSkills() {
        assertEquals(19, DefaultSkills.registry().snapshot().size());
    }

    @Test void miningAndWoodcuttingUseReferenceBlockValues() {
        var tables = SkillXpTables.defaults();
        assertEquals(BigDecimal.valueOf(2400), tables.xpFor(SkillId.parse("bigbangskills:mining"), "minecraft:diamond_ore"));
        assertEquals(BigDecimal.valueOf(3600), tables.xpFor(SkillId.parse("bigbangskills:mining"), "minecraft:deepslate_diamond_ore"));
        assertEquals(BigDecimal.valueOf(70), tables.xpFor(SkillId.parse("bigbangskills:woodcutting"), "minecraft:oak_log"));
        assertEquals(BigDecimal.ZERO, tables.xpFor(SkillId.parse("bigbangskills:mining"), "modded:unknown_ore"));
    }

    @Test void existingSkillsExposeBaselineAbilityDefinitions() {
        assertEquals(6, DefaultAbilityCatalog.all().get(SkillId.parse("bigbangskills:mining")).size());
        assertEquals(5, DefaultAbilityCatalog.all().get(SkillId.parse("bigbangskills:woodcutting")).size());
        assertEquals(81, DefaultAbilityCatalog.all().values().stream().mapToInt(java.util.List::size).sum());
    }

    @Test void everyBaselineAbilityCarriesOrderedUnlockAndCooldownMetadata() {
        var total = 0;
        for (var abilities : DefaultAbilityCatalog.all().values()) for (var ability : abilities) {
            total++;
            assertFalse(ability.rankUnlockLevels().isEmpty());
            assertEquals(ability.unlockLevel(), ability.rankUnlockLevels().getFirst());
            assertEquals(ability.rankUnlockLevels().size(), ability.rankForLevel(10_000));
            if (ability.type() == com.bigbangcraft.bigbangskills.common.ability.AbilityType.ACTIVE) {
                assertEquals(java.time.Duration.ofSeconds(ability.id().equals("mining.blast_mining") ? 60 : 240), ability.cooldown());
            }
        }
        assertEquals(81, total);
    }

    @Test void referenceExperienceValuesCoverNonGatheringInputs() {
        var values = ReferenceExperienceTables.defaults();
        assertEquals(BigDecimal.valueOf(666), values.xpFor(SkillId.parse("bigbangskills:alchemy"), "potion_brewing.stage_1"));
        assertEquals(BigDecimal.valueOf(80), values.xpFor(SkillId.parse("bigbangskills:excavation"), "mud"));
        assertEquals(BigDecimal.valueOf(100), values.xpFor(SkillId.parse("bigbangskills:fishing"), "cod"));
        assertEquals(BigDecimal.valueOf(1500), values.xpFor(SkillId.parse("bigbangskills:taming"), "animal_taming.sniffer"));
    }
}
