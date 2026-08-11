package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.antiexploit.StationOwnerPersistence;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationOwnerPersistenceTest {
    @Test void roundTripsBrewingAndSmeltingOwners() throws Exception {
        var file = Files.createTempFile("bigbangskills-stations", ".properties");
        var id = UUID.randomUUID();
        var brewing = new HashMap<String, UUID>();
        var smelting = new HashMap<String, UUID>();
        brewing.put("minecraft:0", id);
        smelting.put("minecraft:1", id);
        StationOwnerPersistence.save(file, brewing, smelting);
        var loadedBrewing = new HashMap<String, UUID>();
        var loadedSmelting = new HashMap<String, UUID>();
        StationOwnerPersistence.load(file, loadedBrewing, loadedSmelting);
        assertEquals(brewing, loadedBrewing);
        assertEquals(smelting, loadedSmelting);
        Files.deleteIfExists(file);
    }
}
