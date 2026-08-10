package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.notification.NotificationService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {
    @Test void aggregatesXpUntilTheWindowEnds() {
        var service = new NotificationService(Duration.ofSeconds(1));
        var player = UUID.randomUUID();
        var skill = SkillId.parse("bigbangskills:mining");
        var start = Instant.parse("2026-01-01T00:00:00Z");
        assertTrue(service.recordXp(player, skill, BigDecimal.ONE, 1, 1, start).isEmpty());
        assertTrue(service.recordXp(player, skill, BigDecimal.valueOf(2), 1, 1, start.plusMillis(500)).isEmpty());
        var feedback = service.flush(start.plusMillis(1500)).getFirst();
        assertEquals(BigDecimal.valueOf(3), feedback.amount());
        assertEquals(1, feedback.fromLevel());
        assertEquals(1, feedback.toLevel());
    }
}
