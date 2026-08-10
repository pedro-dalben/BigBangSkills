package com.bigbangcraft.bigbangskills.neoforge;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockProvenanceService;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.persistence.PersistenceStatusFormatter;
import com.bigbangcraft.bigbangskills.common.persistence.PlayerProgressService;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter;
import com.bigbangcraft.bigbangskills.common.skill.SkillRegistry;
import com.bigbangcraft.bigbangskills.persistence.DatabaseConfig;
import com.bigbangcraft.bigbangskills.persistence.JdbcProgressRepository;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executors;

@Mod("bigbangskills")
public final class NeoForgeBootstrap {
    private static final TagKey<Block> MINING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "mining_ores"));
    private static final TagKey<Block> WOODCUTTING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "woodcutting_logs"));
    private final SkillRegistry skills = DefaultSkills.registry();
    private final GameplayService gameplay = new GameplayService(skills);
    private final BlockProvenanceService provenance = new BlockProvenanceService(200_000);
    private PlayerProgressService progress;

    public NeoForgeBootstrap() {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            var config = DatabaseConfig.loadOrCreate(Path.of("config", "bigbangskills", "database.properties"));
            var dataSource = config.createDataSource();
            var repository = new JdbcProgressRepository(dataSource, serverId());
            var scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> { var thread = new Thread(runnable, "bigbangskills-runtime"); thread.setDaemon(true); return thread; });
            progress = new PlayerProgressService(repository, skills, gameplay, RuntimePersistenceConfig.loadOrCreate(Path.of("config", "bigbangskills", "runtime.properties")), event.getServer()::execute, scheduler, message -> System.getLogger("BigBangSkills").log(System.Logger.Level.WARNING, message));
            progress.setDatabaseDriver(config.type().name());
            progress.start(repository::initializeAsync);
            System.getLogger("BigBangSkills").log(System.Logger.Level.INFO, "BigBangSkills database: " + config.safeDescription());
        } catch (Exception failure) {
            System.getLogger("BigBangSkills").log(System.Logger.Level.ERROR, "BigBangSkills database configuration failed; runtime disabled", failure);
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (progress != null) progress.shutdown().whenComplete((ignored, failure) -> { if (failure != null) System.getLogger("BigBangSkills").log(System.Logger.Level.ERROR, "BigBangSkills shutdown flush failed", failure); });
    }

    private void onServerStopped(ServerStoppedEvent event) { progress = null; }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (progress != null && event.getEntity() instanceof ServerPlayer player) progress.load(player.getUUID(), ProgressionScope.server("default"));
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (progress != null) progress.unload(event.getEntity().getUUID());
    }

    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var state = event.getPlacedBlock();
        if (state.is(MINING) || state.is(BlockTags.LOGS) || state.is(WOODCUTTING)) {
            var pos = event.getPos();
            provenance.markPlaced(new BlockKey(worldId(player), pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        var pos = event.getPos();
        var key = new BlockKey(worldId(player), pos.getX(), pos.getY(), pos.getZ());
        var action = new BlockBreakAction(player.getUUID(), state.getBlock().builtInRegistryHolder().key().location().toString(), player.level().dimension().location().toString(), state.is(MINING), state.is(BlockTags.LOGS) || state.is(WOODCUTTING), true, event.isCanceled(), provenance.wasPlaced(key), provenance.reliable());
        var result = progress == null ? null : progress.blockBreak(action, BigDecimal.ONE, BigDecimal.ONE);
        provenance.clear(key);
        if (result != null && result.accepted()) player.sendSystemMessage(net.minecraft.network.chat.Component.literal("+" + result.amount().stripTrailingZeros().toPlainString() + " XP (" + result.skillId().path() + ")"));
        else if (result != null && "profile_loading_queued".equals(result.reason())) player.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: perfil carregando; XP desta ação foi enfileirado."));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
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
        if (progress == null || progress.progress(player.getUUID()).isEmpty()) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: perfil ainda está carregando.")); return 0; }
        SkillMessageFormatter.overview(progress.progress(player.getUUID()).orElseThrow(), skills).forEach(line -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        return 1;
    }

    private int sendSkill(ServerPlayer player, String skill) {
        if (progress == null || progress.progress(player.getUUID()).isEmpty()) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("BigBangSkills: perfil ainda está carregando.")); return 0; }
        SkillMessageFormatter.skill(progress.progress(player.getUUID()).orElseThrow(), skills, skill).forEach(line -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        return 1;
    }

    private static UUID worldId(net.minecraft.server.level.ServerPlayer player) { return UUID.nameUUIDFromBytes(player.level().dimension().location().toString().getBytes(StandardCharsets.UTF_8)); }
    private static String serverId() { return Path.of(".").toAbsolutePath().normalize().toString(); }
}
