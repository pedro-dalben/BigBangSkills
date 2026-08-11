package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.skill.ExcavationTreasureEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExcavationTreasureEngineTest {
    @Test void archaeologyAppliesReferenceLevelGatesAndIndependentRolls() {
        var engine = ExcavationTreasureEngine.defaults();
        var levelOne = engine.roll("minecraft:mud", 1, true, false, () -> 0.0);
        assertTrue(levelOne.stream().anyMatch(reward -> reward.itemId().equals("minecraft:stick")));
        assertFalse(levelOne.stream().anyMatch(reward -> reward.itemId().equals("minecraft:heart_of_the_sea")));
        var levelNinety = engine.roll("minecraft:mud", 90, true, false, () -> 0.0);
        assertTrue(levelNinety.stream().anyMatch(reward -> reward.itemId().equals("minecraft:heart_of_the_sea")));
        assertTrue(engine.roll("minecraft:mud", 90, false, false, () -> 0.0).isEmpty());
    }

    @Test void gigaDrillUsesThreeTreasureRollsWithoutChangingTheTable() {
        var engine = ExcavationTreasureEngine.defaults();
        assertEquals(3, engine.roll("minecraft:mud", 1, true, true, () -> 0.0).stream()
                .filter(reward -> reward.itemId().equals("minecraft:stick")).count());
    }
}
