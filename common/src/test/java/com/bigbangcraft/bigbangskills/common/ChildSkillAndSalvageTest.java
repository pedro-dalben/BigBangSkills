package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.config.SkillItemTables;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.PowerLevelCalculator;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.SalvageEngine;
import com.bigbangcraft.bigbangskills.common.skill.SmeltingEngine;
import com.bigbangcraft.bigbangskills.common.skill.WoodcuttingEngine;
import com.bigbangcraft.bigbangskills.common.skill.FishingEngine;
import com.bigbangcraft.bigbangskills.common.skill.AlchemyEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChildSkillAndSalvageTest {
    @Test void childLevelsAreDerivedAndExcludedFromPower() {
        var progress = new PlayerProgress(UUID.randomUUID());
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:repair"), BigDecimal.ZERO, 40, 0));
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:fishing"), BigDecimal.ZERO, 20, 0));
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:salvage"), BigDecimal.ZERO, 1, 0));
        progress.refreshDerived();
        assertEquals(30, progress.get(SkillId.parse("bigbangskills:salvage")).level());
        assertEquals(BigDecimal.ZERO, progress.get(SkillId.parse("bigbangskills:salvage")).totalXp());
        assertEquals(60, new PowerLevelCalculator().calculate(progress));
    }

    @Test void salvageYieldFollowsRemainingDurabilityAndRejectsBrokenItems() {
        var progress = new PlayerProgress(UUID.randomUUID());
        progress.put(new SkillProgress(SkillId.parse("bigbangskills:salvage"), BigDecimal.ZERO, 20, 0));
        var rule = new SkillItemTables.SalvageRule("minecraft:iron_ingot", 4, 100, 1);
        var engine = new SalvageEngine();
        assertEquals(2, engine.resolve(progress, "minecraft:iron_sword", 50, 100, rule).yield());
        assertFalse(engine.resolve(progress, "minecraft:iron_sword", 100, 100, rule).accepted());
        var novice = new PlayerProgress(UUID.randomUUID());
        novice.put(new SkillProgress(SkillId.parse("bigbangskills:salvage"), BigDecimal.ZERO, 1, 0));
        assertEquals("arcane_salvage_required", engine.resolve(novice, "minecraft:iron_sword", 50, 100, rule, true).reason());
        assertEquals(5, engine.arcaneSalvageLevel(8, 7, () -> 0));
        var partialRolls = new double[] {.4, .1};
        var partialIndex = new int[1];
        assertEquals(1, engine.arcaneSalvageLevel(8, 2, () -> partialRolls[partialIndex[0]++]));
        var failedRolls = new double[] {.4, .4};
        var failedIndex = new int[1];
        assertEquals(0, engine.arcaneSalvageLevel(8, 1, () -> failedRolls[failedIndex[0]++]));
        var rankOne = new PlayerProgress(UUID.randomUUID());
        rankOne.put(new SkillProgress(SkillId.parse("bigbangskills:salvage"), BigDecimal.ZERO, 1, 0));
        assertEquals(1, engine.resolve(rankOne, "minecraft:iron_sword", 0, 100, rule).yield());
    }

    @Test void repairMaterialsAreDataDrivenAndSupportConfiguredModdedItems() throws Exception {
        var defaults = SkillItemTables.defaults();
        assertEquals("diamond", defaults.repairMaterial("minecraft:diamond_pickaxe"));
        var directory = java.nio.file.Files.createTempDirectory("bigbangskills-repair");
        var table = directory.resolve("salvage.properties");
        java.nio.file.Files.writeString(directory.resolve("repair.properties"), "modded:hammer=diamond\n");
        var loaded = SkillItemTables.loadOrCreate(table);
        assertEquals("diamond", loaded.repairMaterial("modded:hammer"));
    }

    @Test void smeltingAndAlchemyUseReferenceCurves() {
        var smelting = new SmeltingEngine();
        assertEquals(400, smelting.fuelEfficiency(100, 3));
        assertTrue(smelting.secondSmelt(100, true, 0.1, 50, 100));
        assertTrue(smelting.canSecondSmelt(62, 64));
        assertFalse(smelting.canSecondSmelt(63, 64));
        assertEquals(5, smelting.vanillaXp(1, 8));
        assertEquals(0.5f, smelting.vanillaXp(0.1f, 8), 0.0001f);
        var woodcutting = new WoodcuttingEngine();
        assertEquals(70, woodcutting.treeFellerXp(70, 0, true));
        assertEquals(1, woodcutting.treeFellerXp(70, 20, true));
        assertEquals(12, new FishingEngine().fishermanDiet(40, 10));
        var alchemy = new AlchemyEngine();
        assertEquals(1.0, alchemy.brewSpeed(1, 1, false, 1, 4, 100));
        assertEquals(4.0, alchemy.brewSpeed(100, 1, false, 1, 4, 100));
    }

    @Test void repairMasteryAndSuperRepairUseBoundedReferenceMath() {
        var engine = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(
                com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(), () -> 0.0);
        assertEquals(30, engine.repairedDurability(30, 10, 100));
        assertEquals(20, engine.repairedDurability(30, 10, 1));
        assertEquals(5, engine.arcaneForgingLevel(1, 7));
        var rolls = new double[] {0.0, 0.5};
        var index = new int[1];
        var downgraded = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(
                com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(),
                () -> rolls[index[0]++]);
        assertEquals(1, downgraded.arcaneForgingLevel(1, 2));
        var lost = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(
                com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(), () -> .99);
        assertEquals(0, lost.arcaneForgingLevel(1, 2));
    }
}
