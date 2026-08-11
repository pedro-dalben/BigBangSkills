package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.skill.HerbalismEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HerbalismEngineTest {
    private final HerbalismEngine engine = new HerbalismEngine();

    @Test void referenceChanceAndMaturityRules() {
        assertEquals(50, engine.doubleDropsChance(50, 100), 0.001);
        assertEquals(5, engine.verdantBountyChance(100, 50, 1000), 0.001);
        assertTrue(engine.mature(2, 3, true));
        assertFalse(engine.mature(1, 3, true));
        assertTrue(engine.mature(7, 7, false));
    }

    @Test void conversionsAndHylianLuckAreBounded() {
        assertEquals("minecraft:mycelium", engine.shroomThumbConversion("minecraft:dirt").orElseThrow());
        assertEquals("minecraft:grass_block", engine.greenTerraConversion("minecraft:dirt_path").orElseThrow());
        assertTrue(engine.hylianLuck("minecraft:poppy", 100, 10, () -> 0).isPresent());
        assertTrue(engine.hylianLuck("minecraft:stone", 100, 10, () -> 0).isEmpty());
    }
}
