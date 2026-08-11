package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.api.XpSource;
import com.bigbangcraft.bigbangskills.common.config.DiminishedReturnsConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillXpTables;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiminishedReturnsTest {
    @Test void keepsConfiguredMinimumAfterThresholdIsExceeded() {
        var skill = SkillId.parse("bigbangskills:mining");
        var config = new DiminishedReturnsConfig(true, BigDecimal.valueOf(.05), 10, Map.of(skill, BigDecimal.valueOf(100)));
        var gameplay = new GameplayService(DefaultSkills.registry(), SkillXpTables.defaults(), SkillConfig.defaults(), SkillFormulaConfig.defaults(), config);
        var player = new PlayerProgress(UUID.randomUUID());
        var action = new SkillAwardAction(player.playerId(), skill, BigDecimal.valueOf(100), XpSource.CUSTOM, "test", ProgressionScope.server("test"), true, false, false, true);
        gameplay.award(player, action);
        gameplay.award(player, action);
        assertEquals(BigDecimal.valueOf(5), gameplay.award(player, action).amount());
    }
}
