package com.bigbangcraft.bigbangskills.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.antiexploit.XpEligibilityService;
import com.bigbangcraft.bigbangskills.common.progression.LinearXpCurve;
import com.bigbangcraft.bigbangskills.common.skill.*;
import java.math.BigDecimal;

@Mod("bigbangskills")
public final class NeoForgeBootstrap {
    private static final TagKey<Block> MINING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "mining_ores"));
    private static final TagKey<Block> WOODCUTTING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "woodcutting_logs"));
    private final GameplayService gameplay;

    public NeoForgeBootstrap() {
        var skills = new SkillRegistry();
        skills.register(new SkillDefinition(SkillId.parse("bigbangskills:mining"), "bigbangskills.skill.mining", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        skills.register(new SkillDefinition(SkillId.parse("bigbangskills:woodcutting"), "bigbangskills.skill.woodcutting", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        gameplay = new GameplayService(skills);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        var result = gameplay.blockBreak(new BlockBreakAction(player.getUUID(), state.getBlock().builtInRegistryHolder().key().location().toString(), player.level().dimension().location().toString(), state.is(MINING), state.is(BlockTags.LOGS) || state.is(WOODCUTTING), !player.isSpectator(), event.isCanceled(), false, true), BigDecimal.ONE, BigDecimal.ONE);
        if (result.accepted()) player.sendSystemMessage(net.minecraft.network.chat.Component.literal("+" + result.amount().stripTrailingZeros().toPlainString() + " XP (" + result.skillId().path() + ")"));
    }
}
