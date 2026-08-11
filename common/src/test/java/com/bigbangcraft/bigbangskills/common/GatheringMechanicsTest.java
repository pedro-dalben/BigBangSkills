package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction;
import com.bigbangcraft.bigbangskills.api.XpSource;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatheringMechanicsTest {
    private static final SkillId MINING = SkillId.parse("bigbangskills:mining");
    private static final SkillId WOODCUTTING = SkillId.parse("bigbangskills:woodcutting");
    private static final SkillId EXCAVATION = SkillId.parse("bigbangskills:excavation");

    @Test void miningUsesReferenceXpAndDoubleDropChance() {
        var player = UUID.randomUUID();
        var service = new GameplayService(DefaultSkills.registry(), com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.005);
        var result = service.blockBreak(new PlayerProgress(player), new BlockBreakAction(player, "minecraft:diamond_ore", "world", true, false, true, false, false, true, true, false), ProgressionScope.server("test"));
        assertTrue(result.accepted());
        assertEquals(2400, result.amount().intValue());
        assertEquals(1, result.blockEffect().extraDrops());
    }

    @Test void woodcuttingCleanCutsWinsAtConfiguredLevel() {
        var player = UUID.randomUUID();
        var progress = new PlayerProgress(player);
        var curve = DefaultSkills.registry().get(WOODCUTTING).orElseThrow().curve();
        progress.put(new SkillProgress(WOODCUTTING, curve.totalXpForLevel(100), 100, 0));
        var service = new GameplayService(DefaultSkills.registry(), com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var result = service.blockBreak(progress, new BlockBreakAction(player, "minecraft:oak_log", "world", false, true, true, false, false, true, false, false), ProgressionScope.server("test"));
        assertTrue(result.accepted());
        assertEquals(2, result.blockEffect().extraDrops());
    }

    @Test void genericAwardsUseTheSameConfiguredProgressionPath() {
        var player = UUID.randomUUID();
        var progress = new PlayerProgress(player);
        var skill = SkillId.parse("bigbangskills:fishing");
        var service = new GameplayService(DefaultSkills.registry(), com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var result = service.award(progress, new SkillAwardAction(player, skill, BigDecimal.valueOf(100), XpSource.INTEGRATION,
                "fishing_cod", ProgressionScope.server("test"), true, false, false, false));
        assertTrue(result.accepted());
        assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), result.amount());
        assertEquals(100, progress.get(skill).totalXp().intValue());
    }

    @Test void actionAwardsReadExternalizableReferenceValues() {
        var player = UUID.randomUUID();
        var progress = new PlayerProgress(player);
        var service = new GameplayService(DefaultSkills.registry(), com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var result = service.award(progress, SkillId.parse("bigbangskills:fishing"), "cod", XpSource.INTEGRATION, "fishing_cod", ProgressionScope.server("test"), false, false, player);
        assertTrue(result.accepted());
        assertEquals(100, result.amount().intValue());
    }

    @Test void excavationAndHerbalismUseReferenceBlockActions() {
        var player = UUID.randomUUID();
        var service = new GameplayService(DefaultSkills.registry(), com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var excavation = service.blockBreak(new PlayerProgress(player), new BlockBreakAction(player, "minecraft:mud", "world", false, false, true, false, false, true, false, false, true, false), ProgressionScope.server("test"));
        assertTrue(excavation.accepted());
        assertEquals(80, excavation.amount().intValue());
        var herbalism = service.blockBreak(new PlayerProgress(player), new BlockBreakAction(player, "minecraft:dandelion", "world", false, false, true, false, false, true, false, false, false, true), ProgressionScope.server("test"));
        assertTrue(herbalism.accepted());
        assertEquals(100, herbalism.amount().intValue());
        assertEquals(1, herbalism.blockEffect().extraDrops());
    }

    @Test void activeTreeAndGigaDrillEffectsHaveBoundedChains() {
        var player = UUID.randomUUID();
        var registry = DefaultSkills.registry();
        var service = new GameplayService(registry, com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var wood = new PlayerProgress(player);
        var woodCurve = registry.get(WOODCUTTING).orElseThrow().curve();
        wood.put(new SkillProgress(WOODCUTTING, woodCurve.totalXpForLevel(5), 5, 0));
        var tree = service.blockBreak(wood, new BlockBreakAction(player, "minecraft:oak_log", "world", false, true, true, false, false, true, false, true), ProgressionScope.server("test"));
        assertEquals(1000, tree.blockEffect().chainBreaks());
        var dirt = new PlayerProgress(player);
        var dirtCurve = registry.get(EXCAVATION).orElseThrow().curve();
        dirt.put(new SkillProgress(EXCAVATION, dirtCurve.totalXpForLevel(5), 5, 0));
        var giga = service.blockBreak(dirt, new BlockBreakAction(player, "minecraft:dirt", "world", false, false, true, false, false, true, false, true, true, false), ProgressionScope.server("test"));
        assertEquals(8, giga.blockEffect().chainBreaks());
    }

    @Test void leafBlowerExtendsTreeFellerToLeaves() {
        var player = UUID.randomUUID();
        var registry = DefaultSkills.registry();
        var service = new GameplayService(registry, com.bigbangcraft.bigbangskills.common.config.SkillXpTables.defaults(), com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults(), () -> 0.0);
        var progress = new PlayerProgress(player);
        progress.put(new SkillProgress(WOODCUTTING, BigDecimal.ZERO, 15, 0));
        var result = service.blockBreak(progress, new BlockBreakAction(player, "minecraft:oak_log", "world", false, true, true, false, false, true, false, true), ProgressionScope.server("test"));
        assertTrue(result.blockEffect().includeLeaves());
    }

    @Test void knockOnWoodUsesReferenceDropAndOrbBoundaries() {
        var engine = new com.bigbangcraft.bigbangskills.common.skill.WoodcuttingEngine();
        assertTrue(engine.normalTreePartDrops(() -> .74));
        assertFalse(engine.normalTreePartDrops(() -> .75));
        assertFalse(engine.knockOnWoodXpOrb(1, true, () -> 0.0));
        assertTrue(engine.knockOnWoodXpOrb(2, true, () -> 0.09));
        assertFalse(engine.knockOnWoodXpOrb(2, false, () -> 0.0));
    }
}
