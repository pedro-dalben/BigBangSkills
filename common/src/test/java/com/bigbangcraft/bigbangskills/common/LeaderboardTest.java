package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.leaderboard.LeaderboardService;
import com.bigbangcraft.bigbangskills.common.progression.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {
    @Test void snapshotIsSortedAndLimited() {
        var skill = SkillId.parse("bigbangskills:mining"); var curve = new LinearXpCurve(BigDecimal.ONE, BigDecimal.ONE); var a = new PlayerProgress(UUID.randomUUID()); var b = new PlayerProgress(UUID.randomUUID());
        a.put(new SkillProgress(skill, BigDecimal.TEN, 4, 1)); b.put(new SkillProgress(skill, BigDecimal.ONE, 2, 1));
        var service = new LeaderboardService(Clock.systemUTC()); service.refresh(List.of(a, b));
        assertEquals(a.playerId(), service.top(skill, 1).getFirst().playerId());
    }
}
