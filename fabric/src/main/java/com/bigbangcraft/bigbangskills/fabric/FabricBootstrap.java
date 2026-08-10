package com.bigbangcraft.bigbangskills.fabric;

import com.bigbangcraft.bigbangskills.common.antiexploit.BlockKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockProvenanceService;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.persistence.PersistenceStatusFormatter;
import com.bigbangcraft.bigbangskills.common.persistence.PlayerProgressService;
import com.bigbangcraft.bigbangskills.common.notification.NotificationService;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessages;
import com.bigbangcraft.bigbangskills.common.skill.SkillRegistry;
import com.bigbangcraft.bigbangskills.persistence.DatabaseConfig;
import com.bigbangcraft.bigbangskills.persistence.JdbcProgressRepository;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class FabricBootstrap implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("BigBangSkills");
    private static final TagKey<Block> MINING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "mining_ores"));
    private static final TagKey<Block> WOODCUTTING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "woodcutting_logs"));
    private final SkillRegistry skills = DefaultSkills.registry();
    private final GameplayService gameplay = new GameplayService(skills);
    private final NotificationService notifications = new NotificationService(Duration.ofMillis(500));
    private BlockProvenanceService provenance;
    private PlayerProgressService progress;

    @Override public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && serverPlayer.getItemInHand(hand).getItem() instanceof BlockItem) {
                var target = hit.getBlockPos().relative(hit.getDirection());
                var replaceable = world.getBlockState(target).canBeReplaced();
                world.getServer().execute(() -> {
                    var state = world.getBlockState(target);
                    if (replaceable && (state.is(MINING) || state.is(BlockTags.LOGS) || state.is(WOODCUTTING))) markPlaced(world, target);
                });
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            var key = new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ());
            var tracker = provenance;
            var placed = tracker != null && tracker.wasPlaced(key);
            var mining = state.is(MINING);
            var wood = state.is(BlockTags.LOGS) || state.is(WOODCUTTING);
            var action = new BlockBreakAction(serverPlayer.getUUID(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), world.dimension().location().toString(), mining, wood, true, false, placed, tracker != null && tracker.reliable());
            var result = progress == null ? null : progress.blockBreak(action, BigDecimal.ONE, BigDecimal.ONE);
            if (tracker != null) tracker.clear(key);
            if (result != null && result.accepted()) notifications.recordXp(serverPlayer.getUUID(), result.skillId(), result.amount(), result.previousLevel(), result.currentLevel(), Instant.now()).forEach(feedback -> sendFeedback(serverPlayer, feedback));
            else if (result != null && "profile_loading_queued".equals(result.reason())) serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("profile.queued", SkillMessages.locale(serverPlayer.clientInformation().language()))));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> notifications.flush(Instant.now()).forEach(feedback -> { var player = server.getPlayerList().getPlayer(feedback.playerId()); if (player != null) sendFeedback(player, feedback); }));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> progress = null);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (progress != null) progress.load(handler.getPlayer().getUUID(), com.bigbangcraft.bigbangskills.api.ProgressionScope.server("default"));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { notifications.clear(handler.getPlayer().getUUID()); if (progress != null) progress.unload(handler.getPlayer().getUUID()); });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        LOGGER.info("BigBangSkills Fabric adapter loaded with Mining and Woodcutting");
    }

    private void serverStarted(MinecraftServer server) {
        try {
            provenance = new BlockProvenanceService(200_000, server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("bigbangskills-provenance.dat"));
            provenance.loadAsync().whenComplete((ignored, failure) -> { if (failure != null) LOGGER.error("BigBangSkills provenance load failed; XP remains fail-closed", failure); });
            var config = DatabaseConfig.loadOrCreate(Path.of("config", "bigbangskills", "database.properties"));
            var dataSource = config.createDataSource();
            var repository = new JdbcProgressRepository(dataSource, serverId());
            var scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> { var thread = new Thread(runnable, "bigbangskills-runtime"); thread.setDaemon(true); return thread; });
            progress = new PlayerProgressService(repository, skills, gameplay, RuntimePersistenceConfig.loadOrCreate(Path.of("config", "bigbangskills", "runtime.properties")), server::execute, scheduler, LOGGER::warn);
            progress.setDatabaseDriver(config.type().name());
            progress.start(repository::initializeAsync);
            LOGGER.info("BigBangSkills database: {}", config.safeDescription());
        } catch (Exception failure) {
            LOGGER.error("BigBangSkills database configuration failed; runtime disabled", failure);
        }
    }

    private void serverStopping(MinecraftServer server) {
        var tracker = provenance;
        if (tracker != null) tracker.shutdown(java.time.Duration.ofSeconds(5)).whenComplete((ignored, failure) -> { if (failure != null) LOGGER.error("BigBangSkills provenance shutdown flush failed", failure); });
        if (progress != null) progress.shutdown().whenComplete((ignored, failure) -> { if (failure != null) LOGGER.error("BigBangSkills shutdown flush failed", failure); });
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var skillsCommand = Commands.literal("skills").executes(context -> sendOverview(context.getSource().getPlayerOrException()));
        skillsCommand.then(Commands.literal("mining").executes(context -> sendSkill(context.getSource().getPlayerOrException(), "mining")));
        skillsCommand.then(Commands.literal("woodcutting").executes(context -> sendSkill(context.getSource().getPlayerOrException(), "woodcutting")));
        dispatcher.register(skillsCommand);
        dispatcher.register(Commands.literal("skillsadmin").requires(source -> source.hasPermission(2)).then(Commands.literal("status").executes(context -> {
            if (progress == null) { context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: runtime indisponível")); return 0; }
            PersistenceStatusFormatter.format(progress.status(), Clock.systemUTC()).forEach(line -> context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
            return 1;
        })));
    }

    private int sendOverview(ServerPlayer player) {
        if (progress == null || progress.progress(player.getUUID()).isEmpty()) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("profile.loading", SkillMessages.locale(player.clientInformation().language())))); return 0; }
        SkillMessageFormatter.overview(progress.progress(player.getUUID()).orElseThrow(), skills, SkillMessages.locale(player.clientInformation().language())).forEach(line -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        return 1;
    }

    private int sendSkill(ServerPlayer player, String skill) {
        if (progress == null || progress.progress(player.getUUID()).isEmpty()) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("profile.loading", SkillMessages.locale(player.clientInformation().language())))); return 0; }
        SkillMessageFormatter.skill(progress.progress(player.getUUID()).orElseThrow(), skills, skill, SkillMessages.locale(player.clientInformation().language())).forEach(line -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        return 1;
    }

    private static void sendFeedback(ServerPlayer player, NotificationService.Feedback feedback) {
        var locale = SkillMessages.locale(player.clientInformation().language());
        var skill = SkillMessages.text("bigbangskills.skill." + feedback.skillId().path(), locale);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("xp.gained", locale, feedback.amount().stripTrailingZeros().toPlainString(), skill)), true);
        if (feedback.toLevel() > feedback.fromLevel()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("level.up", locale, skill, feedback.fromLevel(), feedback.toLevel())));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void markPlaced(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) { if (provenance != null) provenance.markPlaced(new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ())); }
    private static UUID worldId(net.minecraft.world.level.Level world) { return UUID.nameUUIDFromBytes(world.dimension().location().toString().getBytes(StandardCharsets.UTF_8)); }
    private static String serverId() { return Path.of(".").toAbsolutePath().normalize().toString(); }
}
