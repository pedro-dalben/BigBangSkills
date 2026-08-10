package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.*;
import com.bigbangcraft.bigbangskills.common.progression.*;
import com.bigbangcraft.bigbangskills.common.skill.*;
import com.bigbangcraft.bigbangskills.common.xp.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");

    @Test void linearCurveConvertsBothDirections() {
        var curve = new LinearXpCurve(BigDecimal.valueOf(100), BigDecimal.valueOf(25));
        assertEquals(BigDecimal.ZERO, curve.totalXpForLevel(1));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(curve.totalXpForLevel(2)));
        assertEquals(10, curve.levelAt(curve.totalXpForLevel(10), 100));
        assertEquals(9, curve.levelAt(curve.totalXpForLevel(10).subtract(BigDecimal.ONE), 100));
    }

    @Test void pipelineAppliesModifiersInOrderAndRaisesLevel() {
        var registry = new SkillRegistry();
        registry.register(new SkillDefinition(MINING, "skill.mining", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        var player = new PlayerProgress(UUID.randomUUID());
        var request = new XpRequest(UUID.randomUUID(), player.playerId(), MINING, BigDecimal.valueOf(10), XpSource.BLOCK_BREAK, "natural_ore_break", ProgressionScope.server("test"), Instant.now());
        var result = new XpService().apply(player, request, registry, List.of(new XpModifier("server", 20, BigDecimal.valueOf(2)), new XpModifier("skill", 10, BigDecimal.valueOf(3))));
        assertTrue(result.accepted());
        assertEquals(BigDecimal.valueOf(60).setScale(4), result.amount().setScale(4));
        assertEquals(6, result.after().level());
        assertEquals(6, new PowerLevelCalculator().calculate(player));
    }
}
