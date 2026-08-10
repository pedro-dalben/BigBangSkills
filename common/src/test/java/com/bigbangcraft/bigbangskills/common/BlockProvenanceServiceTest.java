package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.antiexploit.BlockKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockProvenanceService;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockProvenanceServiceTest {
    @Test void persistsPackedSectionAcrossRestartAndCleansEmptySection() throws Exception {
        var directory = Files.createTempDirectory("bigbangskills-provenance");
        var file = directory.resolve("data").resolve("provenance.dat");
        var world = UUID.randomUUID();
        var key = new BlockKey(world, -1, -17, -33);

        try (var first = new BlockProvenanceService(10, file)) {
            first.loadAsync().toCompletableFuture().get(2, TimeUnit.SECONDS);
            first.markPlaced(key);
            first.flushAsync().toCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        try (var restarted = new BlockProvenanceService(10, file)) {
            restarted.loadAsync().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertTrue(restarted.reliable());
            assertTrue(restarted.wasPlaced(key));
            assertEquals(1, restarted.size());
            assertEquals(1, restarted.sectionCount());
            restarted.clear(key);
            restarted.flushAsync().toCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        try (var cleaned = new BlockProvenanceService(10, file)) {
            cleaned.loadAsync().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertFalse(cleaned.wasPlaced(key));
            assertEquals(0, cleaned.size());
            assertEquals(0, cleaned.sectionCount());
        }
    }
}
