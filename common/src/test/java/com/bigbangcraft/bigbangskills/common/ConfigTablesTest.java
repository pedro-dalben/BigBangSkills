package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.config.AlchemyConcoctionTables;
import com.bigbangcraft.bigbangskills.common.config.TamingSummonTables;
import com.bigbangcraft.bigbangskills.common.config.CombatWeaponTables;
import com.bigbangcraft.bigbangskills.common.skill.FishingTreasureEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigTablesTest {
    @Test void tamingSummonsAcceptNamespacedModdedRecipes() throws Exception {
        var file = Files.createTempFile("bigbangskills-taming", ".properties");
        Files.writeString(file, "mod:beast=mod:feed|3|4|1200\n");
        var recipe = TamingSummonTables.loadOrCreate(file).recipe("mod:beast");
        assertEquals("mod:feed", recipe.itemId());
        assertEquals(4, recipe.ownerLimit());
        Files.deleteIfExists(file);
    }

    @Test void alchemyAcceptsConfiguredRegistryEffect() throws Exception {
        var file = Files.createTempFile("bigbangskills-alchemy", ".properties");
        Files.writeString(file, "mod:ingredient=3|mod:effect|120|1\n");
        var recipe = AlchemyConcoctionTables.loadOrCreate(file).recipe("mod:ingredient");
        assertNotNull(recipe);
        assertEquals(3, recipe.tier());
        assertEquals("mod:effect", recipe.effectId());
        Files.deleteIfExists(file);
    }

    @Test void combatWeaponsAcceptNamespacedMappings() throws Exception {
        var file = Files.createTempFile("bigbangskills-weapons", ".properties");
        Files.writeString(file, "mod:halberd=spears\n");
        assertEquals("bigbangskills:spears", CombatWeaponTables.loadOrCreate(file).skillFor("mod:halberd").toString());
        Files.deleteIfExists(file);
    }

    @Test void fishingTreasureTableAcceptsNamespacedRewards() throws Exception {
        var file = Files.createTempFile("bigbangskills-fishing", ".properties");
        Files.writeString(file, "0|mod:crystal=2,321,false\n");
        var reward = FishingTreasureEngine.loadOrCreate(file).roll(1, 0, () -> 0.0).orElseThrow();
        assertEquals("mod:crystal", reward.itemId());
        assertEquals(321, reward.xp());
        Files.deleteIfExists(file);
    }
}
