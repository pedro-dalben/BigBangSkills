package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.skill.FishingShakeEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FishingShakeEngineTest {
    @Test void resolvesConfiguredEntityLootAndRespectsWeight() {
        var engine = new FishingShakeEngine(java.util.Map.of("zombie", java.util.List.<FishingShakeEngine.Entry>of()));
        assertTrue(engine.roll("zombie", 1, () -> 0).isEmpty());
        var defaults = new FishingShakeEngine();
        assertEquals("minecraft:zombie_head", defaults.roll("zombie", 1, () -> 0).orElseThrow().itemId());
        assertEquals("poison", defaults.roll("cave_spider", 1, () -> .99).orElseThrow().potion());
        assertEquals("instant_heal", defaults.roll("witch", 1, () -> 0).orElseThrow().potion());
        assertTrue(defaults.roll("unknown_modded_entity", 100, () -> 0).isEmpty());
    }

    @Test void resolvesNamespacedModdedEntityWithoutBreakingVanillaPathDefaults() {
        var engine = new FishingShakeEngine(java.util.Map.of(
                "modded:crystal_beast", java.util.List.of(new FishingShakeEngine.Entry("modded:shard", 1, 0, 100, 0))));
        assertEquals("modded:shard", engine.roll("modded:crystal_beast", 1, () -> 0).orElseThrow().itemId());
        assertEquals("minecraft:zombie_head", new FishingShakeEngine().roll("minecraft:zombie", 1, () -> 0).orElseThrow().itemId());
    }

    @Test void rejectsMalformedRulesBeforeLoaderUse() throws Exception {
        var file = Files.createTempFile("bigbangskills-shake", ".properties");
        Files.writeString(file, "zombie|bad-item=1,0,NaN,0\n");
        assertThrows(IllegalStateException.class, () -> FishingShakeEngine.loadOrCreate(file));
        Files.deleteIfExists(file);
    }

    @Test void loadsOptionalPotionMetadata() throws Exception {
        var file = Files.createTempFile("bigbangskills-shake-potion", ".properties");
        Files.writeString(file, "modded|minecraft:potion=1,0,100,0,poison\n");
        assertEquals("poison", FishingShakeEngine.loadOrCreate(file).roll("modded", 1, () -> 0).orElseThrow().potion());
        Files.deleteIfExists(file);
    }
}
