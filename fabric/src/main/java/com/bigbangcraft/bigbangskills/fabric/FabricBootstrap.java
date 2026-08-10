package com.bigbangcraft.bigbangskills.fabric;

import com.bigbangcraft.bigbangskills.common.antiexploit.BlockKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockProvenanceService;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.persistence.PersistenceStatusFormatter;
import com.bigbangcraft.bigbangskills.common.persistence.PlayerProgressService;
import com.bigbangcraft.bigbangskills.common.notification.NotificationService;
import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessages;
import com.bigbangcraft.bigbangskills.common.skill.SkillRegistry;
import com.bigbangcraft.bigbangskills.persistence.DatabaseConfig;
import com.bigbangcraft.bigbangskills.persistence.JdbcProgressRepository;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraft.world.item.PickaxeItem;
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
import java.util.concurrent.CompletableFuture;
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
            var wood = state.is(BlockTags.LOGS) || state.is(WOODCUTTING);
            var mining = !wood && (state.is(MINING) || (serverPlayer.getMainHandItem().getItem() instanceof PickaxeItem && state.is(BlockTags.MINEABLE_WITH_PICKAXE)));
            var action = new BlockBreakAction(serverPlayer.getUUID(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), world.dimension().location().toString(), mining, wood, true, false, placed, tracker != null && tracker.reliable());
            var result = progress == null ? null : progress.blockBreak(action, mining && state.is(MINING) ? BigDecimal.valueOf(2) : BigDecimal.ONE, BigDecimal.ONE);
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
        skillsCommand.then(Commands.literal("top").executes(context -> sendTop(context.getSource(), "mining")).then(Commands.argument("skill", StringArgumentType.word()).executes(context -> sendTop(context.getSource(), StringArgumentType.getString(context, "skill")))));
        dispatcher.register(skillsCommand);
        dispatcher.register(adminCommands());
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        var root = Commands.literal("skillsadmin").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("status").executes(context -> sendStatus(context.getSource())));
        root.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
        root.then(Commands.literal("xp").then(Commands.literal("add").then(adminXp("add"))).then(Commands.literal("remove").then(adminXp("remove"))).then(Commands.literal("set").then(adminXp("set"))));
        root.then(Commands.literal("level").then(Commands.literal("set").then(Commands.argument("player", StringArgumentType.word()).then(Commands.argument("skill", StringArgumentType.word()).then(Commands.argument("level", IntegerArgumentType.integer(1)).executes(context -> adminLevel(context)))))));
        root.then(Commands.literal("reset").then(Commands.argument("player", StringArgumentType.word()).executes(context -> adminReset(context, null)).then(Commands.argument("skill", StringArgumentType.word()).executes(context -> adminReset(context, StringArgumentType.getString(context, "skill"))))));
        return root;
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> adminXp(String operation) {
        return Commands.argument("player", StringArgumentType.word()).then(Commands.argument("skill", StringArgumentType.word()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> adminXp(context, operation))));
    }

    private int sendStatus(CommandSourceStack source) {
        if (progress == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("runtime.unavailable", java.util.Locale.US))); return 0; }
        PersistenceStatusFormatter.format(progress.status(), Clock.systemUTC()).forEach(line -> source.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        var tracker = provenance;
        if (tracker != null) {
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Provenance sections: " + tracker.sectionCount()));
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Provenance positions: " + tracker.size()));
        }
        return 1;
    }

    private int reload(CommandSourceStack source) {
        try {
            RuntimePersistenceConfig.loadOrCreate(Path.of("config", "bigbangskills", "runtime.properties"));
            DatabaseConfig.loadOrCreate(Path.of("config", "bigbangskills", "database.properties"));
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("reload.success", java.util.Locale.US)));
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("reload.restart", java.util.Locale.US)));
            return 1;
        } catch (Exception failure) {
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills reload failed: " + failure.getMessage()));
            return 0;
        }
    }

    private int sendTop(CommandSourceStack source, String requested) {
        if (progress == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: runtime unavailable")); return 0; }
        var skill = parseSkill(requested);
        if (skill == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown skill: " + requested)); return 0; }
        progress.leaderboard(skill, ProgressionScope.server("default"), 10).whenComplete((rows, failure) -> source.getServer().execute(() -> {
            if (failure != null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Leaderboard unavailable: " + failure.getMessage())); return; }
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Top " + skill.path()));
            for (var i = 0; i < rows.size(); i++) source.sendSystemMessage(net.minecraft.network.chat.Component.literal((i + 1) + ". " + playerName(source.getServer(), rows.get(i).playerId()) + " - " + rows.get(i).totalXp().stripTrailingZeros().toPlainString() + " XP"));
        }));
        return 1;
    }

    private int adminXp(CommandContext<CommandSourceStack> context, String operation) {
        var source = context.getSource();
        if (progress == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: runtime unavailable")); return 0; }
        var player = target(source, StringArgumentType.getString(context, "player"));
        var skill = parseSkill(StringArgumentType.getString(context, "skill"));
        BigDecimal amount;
        try { amount = new BigDecimal(StringArgumentType.getString(context, "amount")); } catch (NumberFormatException failure) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Invalid XP amount")); return 0; }
        if (player == null || skill == null || amount.signum() < 0) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Invalid player, skill or XP")); return 0; }
        var future = "set".equals(operation) ? progress.adminSet(player, skill, amount, ProgressionScope.server("default"), "admin_xp_set") : progress.adminAdjust(player, skill, "remove".equals(operation) ? amount.negate() : amount, ProgressionScope.server("default"), "admin_xp_" + operation);
        return completeAdmin(source, future, operation);
    }

    private int adminLevel(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = target(source, StringArgumentType.getString(context, "player"));
        var skill = parseSkill(StringArgumentType.getString(context, "skill"));
        var level = IntegerArgumentType.getInteger(context, "level");
        com.bigbangcraft.bigbangskills.common.skill.SkillDefinition definition = skill == null ? null : skills.get(skill).orElse(null);
        if (player == null || definition == null || level > definition.maxLevel()) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Invalid player, skill or level")); return 0; }
        return completeAdmin(source, progress.adminSet(player, skill, definition.curve().totalXpForLevel(level), ProgressionScope.server("default"), "admin_level_set"), "level_set");
    }

    private int adminReset(CommandContext<CommandSourceStack> context, String requestedSkill) {
        var source = context.getSource();
        var player = target(source, StringArgumentType.getString(context, "player"));
        if (player == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown player; use an online name or cached UUID")); return 0; }
        var requested = requestedSkill == null ? null : parseSkill(requestedSkill);
        if (requestedSkill != null && requested == null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown skill")); return 0; }
        var ids = requested == null ? skills.snapshot().keySet() : java.util.Set.of(requested);
        var futures = ids.stream().map(skill -> progress.adminSet(player, skill, BigDecimal.ZERO, ProgressionScope.server("default"), "admin_reset").toCompletableFuture()).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).whenComplete((ignored, failure) -> source.getServer().execute(() -> source.sendSystemMessage(net.minecraft.network.chat.Component.literal(failure == null ? "BigBangSkills reset completed" : "BigBangSkills reset failed: " + failure.getMessage()))));
        return 1;
    }

    private int completeAdmin(CommandSourceStack source, java.util.concurrent.CompletionStage<PlayerProgressService.AdminResult> future, String operation) {
        future.whenComplete((result, failure) -> source.getServer().execute(() -> {
            if (failure != null) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Admin operation failed: " + failure.getMessage())); return; }
            if (!result.accepted()) { source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Admin operation rejected: " + result.reason())); return; }
            LOGGER.info("admin actor={} target={} operation={} skill={} oldXp={} newXp={} oldLevel={} newLevel={}", source.getTextName(), result.playerId(), operation, result.skillId(), result.oldXp(), result.newXp(), result.oldLevel(), result.newLevel());
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal("Admin operation completed: " + result.reason()));
        }));
        return 1;
    }

    private UUID target(CommandSourceStack source, String name) {
        var online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
        try { return UUID.fromString(name); } catch (IllegalArgumentException ignored) { return source.getServer().getProfileCache().get(name).map(profile -> profile.getId()).orElse(null); }
    }

    private static SkillId parseSkill(String value) {
        try { return SkillId.parseUserInput(value); } catch (RuntimeException ignored) { return null; }
    }

    private static String playerName(MinecraftServer server, UUID playerId) {
        var online = server.getPlayerList().getPlayer(playerId);
        if (online != null) return online.getGameProfile().getName();
        return server.getProfileCache().get(playerId).map(profile -> profile.getName()).orElse(playerId.toString());
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
