package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.skill.FishingTreasureEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingTreasureEngineTest {
    @Test void tierRatesProduceBoundedReferenceRewards() {
        var reward = new FishingTreasureEngine().roll(1, () -> 0).orElseThrow();
        assertEquals("minecraft:leather_boots", reward.itemId());
        assertTrue(reward.xp() > 0);
        assertTrue(new FishingTreasureEngine().roll(1, () -> .99).isEmpty());
        assertTrue(new FishingTreasureEngine().roll(1, 3, () -> .09).isPresent());
        assertEquals("minecraft:efficiency", new FishingTreasureEngine().magicHunter(100, 0, () -> 0).orElseThrow().enchantmentId());
    }
}
