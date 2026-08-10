package com.bigbangcraft.bigbangskills.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.*;
import com.bigbangcraft.bigbangskills.common.progression.LinearXpCurve;
import com.bigbangcraft.bigbangskills.common.skill.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricBootstrap implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("BigBangSkills");
    private static final TagKey<net.minecraft.world.level.block.Block> MINING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "mining_ores"));
    private static final TagKey<net.minecraft.world.level.block.Block> WOODCUTTING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "woodcutting_logs"));
    public FabricBootstrap() {}
    @Override public void onInitialize() {
        var registry = new SkillRegistry();
        registry.register(new SkillDefinition(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:mining"), "bigbangskills.skill.mining", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        registry.register(new SkillDefinition(com.bigbangcraft.bigbangskills.api.SkillId.parse("bigbangskills:woodcutting"), "bigbangskills.skill.woodcutting", 100, new LinearXpCurve(BigDecimal.TEN, BigDecimal.ONE), true));
        var gameplay = new GameplayService(registry);
        var provenance = new BlockProvenanceService(200_000);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player instanceof ServerPlayer && !world.isClientSide()) {
                var target = hit.getBlockPos().relative(hit.getDirection());
                provenance.markPlaced(new BlockKey(worldId(world), target.getX(), target.getY(), target.getZ()));
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            var key = new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ());
            var placed = provenance.wasPlaced(key);
            var mining = state.is(MINING);
            var wood = state.is(BlockTags.LOGS) || state.is(WOODCUTTING);
            var result = gameplay.blockBreak(new BlockBreakAction(serverPlayer.getUUID(), blockId, world.dimension().location().toString(), mining, wood, !serverPlayer.isSpectator(), false, placed, provenance.reliable()), BigDecimal.ONE, BigDecimal.ONE);
            provenance.clear(key);
            if (result.accepted()) serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("+" + result.amount().stripTrailingZeros().toPlainString() + " XP (" + result.skillId().path() + ")"));
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("skills").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            var progress = gameplay.progress(player.getUUID());
            if (progress == null || progress.skills().isEmpty()) player.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: profile ainda sem XP."));
            else progress.skills().values().forEach(state -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(state.skillId().path() + ": nível " + state.level() + " (" + state.totalXp().stripTrailingZeros().toPlainString() + " XP)")));
            return 1;
        })));
        LOGGER.info("BigBangSkills Fabric adapter loaded with Mining and Woodcutting");
    }
    private static UUID worldId(net.minecraft.world.level.Level world) { return UUID.nameUUIDFromBytes(world.dimension().location().toString().getBytes(StandardCharsets.UTF_8)); }
}
