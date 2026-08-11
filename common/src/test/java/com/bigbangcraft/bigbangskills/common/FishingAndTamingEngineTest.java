package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.FishingEngine;
import com.bigbangcraft.bigbangskills.common.skill.TamingEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FishingAndTamingEngineTest {
    @Test void fishingMatchesReferenceRankTablesAndRejectsStationaryFarm() {
        var fishing = new FishingEngine(3, 3);
        assertEquals(35, fishing.shakeChance(4));
        assertEquals(5, fishing.vanillaXpMultiplier(8));
        assertEquals(10, fishing.boostedVanillaXp(2, 8));
        assertEquals(7, fishing.fishermanDiet(40, 5));
        assertEquals(90, fishing.masterAnglerMinWaitReduction(100, true));
        assertEquals(270, fishing.masterAnglerMaxWaitReduction(100, true, 0));
        assertEquals(40, fishing.masterAnglerMinWaitCap());
        assertEquals(100, fishing.masterAnglerMaxWaitCap());
        assertTrue(fishing.canIceFish(5, true, false, true));
        assertTrue(fishing.magicHunter(20));
        var player = UUID.randomUUID();
        assertTrue(fishing.acceptCatch(player, 0, 0, 0, 1));
        assertTrue(fishing.acceptCatch(player, 1, 0, 1, 21));
        assertFalse(fishing.acceptCatch(player, 1, 0, 1, 41));
        assertTrue(fishing.acceptCatch(player, 20, 0, 0, 61));
    }

    @Test void tamingAppliesReferencePetBonusesAtUnlocks() {
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:taming");
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(skill, BigDecimal.ZERO, 100, 0));
        var attack = new TamingEngine(com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig.defaults(), () -> 0.0)
                .resolveAttack(progress, 5);
        assertEquals(2, attack.damageMultiplier());
        assertEquals(2, attack.bonusDamage());
        assertTrue(attack.fastFood());
        assertTrue(attack.pummel());
        assertEquals(6.0, new TamingEngine().incomingDamage(progress, 12, false, true, false), 0.0001);
        assertEquals(2.0, new TamingEngine().incomingDamage(progress, 12, true, false, false), 0.0001);
        assertEquals(0.0, new TamingEngine().incomingDamage(progress, 12, false, false, true), 0.0001);
    }
}
