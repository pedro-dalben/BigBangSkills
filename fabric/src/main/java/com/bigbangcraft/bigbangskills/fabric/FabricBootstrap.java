package com.bigbangcraft.bigbangskills.fabric;

import com.bigbangcraft.bigbangskills.common.antiexploit.BlockKey;
import com.bigbangcraft.bigbangskills.common.antiexploit.BlockProvenanceService;
import com.bigbangcraft.bigbangskills.common.antiexploit.StationOwnerPersistence;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillConfig;
import com.bigbangcraft.bigbangskills.common.config.SkillXpTables;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.config.TamingSummonTables;
import com.bigbangcraft.bigbangskills.common.config.AlchemyConcoctionTables;
import com.bigbangcraft.bigbangskills.common.config.CombatWeaponTables;
import com.bigbangcraft.bigbangskills.common.config.SkillItemTables;
import com.bigbangcraft.bigbangskills.common.config.DiminishedReturnsConfig;
import com.bigbangcraft.bigbangskills.common.config.HerbalismTreasureTables;
import com.bigbangcraft.bigbangskills.common.persistence.PersistenceStatusFormatter;
import com.bigbangcraft.bigbangskills.common.persistence.PlayerProgressService;
import com.bigbangcraft.bigbangskills.common.notification.NotificationService;
import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakEffect;
import com.bigbangcraft.bigbangskills.common.skill.BlastMiningEngine;
import com.bigbangcraft.bigbangskills.common.skill.DefaultSkills;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessageFormatter;
import com.bigbangcraft.bigbangskills.common.skill.SkillMessages;
import com.bigbangcraft.bigbangskills.common.skill.SkillRegistry;
import com.bigbangcraft.bigbangskills.common.skill.CombatAction;
import com.bigbangcraft.bigbangskills.common.skill.CombatSkillEngine;
import com.bigbangcraft.bigbangskills.common.skill.AcrobaticsEngine;
import com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction;
import com.bigbangcraft.bigbangskills.common.skill.SalvageEngine;
import com.bigbangcraft.bigbangskills.common.skill.FishingEngine;
import com.bigbangcraft.bigbangskills.common.skill.FishingTreasureEngine;
import com.bigbangcraft.bigbangskills.common.skill.FishingShakeEngine;
import com.bigbangcraft.bigbangskills.common.skill.TamingEngine;
import com.bigbangcraft.bigbangskills.common.skill.HerbalismEngine;
import com.bigbangcraft.bigbangskills.common.ability.AbilityService;
import com.bigbangcraft.bigbangskills.common.ability.AbilityType;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
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
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public final class FabricBootstrap implements ModInitializer {
    private static final ThreadLocal<net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity> CURRENT_SMELTING_FURNACE = new ThreadLocal<>();
    private static volatile FabricBootstrap INSTANCE;
    private static final ThreadLocal<Boolean> COMBAT_AREA = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<java.util.Deque<PendingCombat>> PENDING_COMBAT = ThreadLocal.withInitial(ArrayDeque::new);
    public static final Logger LOGGER = LoggerFactory.getLogger("BigBangSkills");
    private static final TagKey<Block> MINING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "mining_ores"));
    private static final TagKey<Block> WOODCUTTING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bigbangskills", "woodcutting_logs"));
    private final SkillConfig skillConfig;
    private final SkillRegistry skills;
    private final GameplayService gameplay;
    private final NotificationService notifications = new NotificationService(Duration.ofMillis(500));
    private final AbilityService abilities = new AbilityService(Clock.systemUTC());
    private final CombatSkillEngine combat;
    private final SkillFormulaConfig formulas;
    private final BlastMiningEngine blastMining;
    private final SkillItemTables itemTables;
    private final SalvageEngine salvage;
    private final FishingEngine fishing;
    private final FishingTreasureEngine fishingTreasures;
    private final FishingShakeEngine fishingShake;
    private final TamingEngine taming;
    private final TamingSummonTables tamingSummons;
    private final AlchemyConcoctionTables alchemyConcoctions;
    private final CombatWeaponTables combatWeapons;
    private final AcrobaticsEngine acrobatics;
    private final com.bigbangcraft.bigbangskills.common.skill.ExcavationTreasureEngine excavationTreasures;
    private final HerbalismEngine herbalism;
    private BlockProvenanceService provenance;
    private PlayerProgressService progress;
    private final Map<BlockKey, BlockBreakEffect> pendingBlockEffects = new ConcurrentHashMap<>();
    private final Map<String, UUID> brewingOwners = new ConcurrentHashMap<>();
    private final Map<String, ItemStack[]> brewingInputs = new ConcurrentHashMap<>();
    private final Map<String, Long> hopperAlchemyTransfers = new ConcurrentHashMap<>();
    private final Map<String, UUID> smeltingOwners = new ConcurrentHashMap<>();
    private final Map<String, FurnaceOutput> smeltingOutputs = new ConcurrentHashMap<>();
    private final Map<UUID, RuptureState> ruptures = new ConcurrentHashMap<>();
    private final Map<UUID, SummonedPet> summonedPets = new ConcurrentHashMap<>();
    // ponytail: weak keys bound target-shake history without a cleanup scheduler; replace if async hooks are added.
    private final Map<UUID, Integer> shakenTargets = new java.util.WeakHashMap<>();
    private final Map<UUID, Integer> trickShotBounces = new java.util.WeakHashMap<>();
    private final Map<UUID, ArrowOrigin> arrowOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, PendingSalvage> pendingSalvages = new ConcurrentHashMap<>();
    private final Map<UUID, PendingSalvage> pendingRepairs = new ConcurrentHashMap<>();
    private final Map<UUID, PreparedFishingReward> preparedFishing = new ConcurrentHashMap<>();
    private final Map<UUID, Long> acrobaticsTeleportCooldowns = new ConcurrentHashMap<>();

    public FabricBootstrap() {
        INSTANCE = this;
        skillConfig = SkillConfig.loadOrCreate(Path.of("config", "bigbangskills", "skills.properties"));
        skills = DefaultSkills.registry(skillConfig);
        formulas = SkillFormulaConfig.loadOrCreate(Path.of("config", "bigbangskills", "skills", "formulas.properties"));
        blastMining = new BlastMiningEngine(formulas);
        salvage = new SalvageEngine(formulas);
        fishing = new FishingEngine((int) formulas.value("fishing.exploit_move_range"), (int) formulas.value("fishing.exploit_over_fish_limit"), formulas);
        fishingShake = FishingShakeEngine.loadOrCreate(Path.of("config", "bigbangskills", "skills", "fishing-shake.properties"));
        fishingTreasures = FishingTreasureEngine.loadOrCreate(Path.of("config", "bigbangskills", "skills", "fishing-treasures.properties"));
        acrobatics = new AcrobaticsEngine(formulas, java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
        itemTables = SkillItemTables.loadOrCreate(Path.of("config", "bigbangskills", "skills", "salvage.properties"));
        excavationTreasures = com.bigbangcraft.bigbangskills.common.skill.ExcavationTreasureEngine.loadOrCreate(Path.of("config", "bigbangskills", "skills", "excavation-treasures.properties"));
        herbalism = new HerbalismEngine(HerbalismTreasureTables.loadOrCreate(Path.of("config", "bigbangskills", "skills", "herbalism-treasures.properties")).all());
        gameplay = new GameplayService(skills, SkillXpTables.loadOrCreate(Path.of("config", "bigbangskills", "skills")), skillConfig, formulas,
                DiminishedReturnsConfig.loadOrCreate(Path.of("config", "bigbangskills", "diminished-returns.properties")));
        combat = new CombatSkillEngine(formulas, skillConfig);
        taming = new TamingEngine(formulas, java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
        tamingSummons = TamingSummonTables.loadOrCreate(Path.of("config", "bigbangskills", "skills", "taming-summons.properties"));
        alchemyConcoctions = AlchemyConcoctionTables.loadOrCreate(Path.of("config", "bigbangskills", "skills", "alchemy-concoctions.properties"));
        combatWeapons = CombatWeaponTables.loadOrCreate(Path.of("config", "bigbangskills", "skills", "combat-weapons.properties"));
    }

    public static void recordFishing(ServerPlayer player, net.minecraft.world.entity.projectile.FishingHook hook, ItemStack catchStack) {
        var instance = INSTANCE;
        if (instance == null || !instance.fishing.acceptCatch(player.getUUID(), hook.getX(), hook.getY(), hook.getZ(), player.level().getGameTime())) {
            if (instance != null) instance.preparedFishing.remove(hook.getUUID());
            return;
        }
        var action = BuiltInRegistries.ITEM.getKey(catchStack.getItem()).toString();
        var skill = SkillId.parse("bigbangskills:fishing");
        instance.awardActivity(player, skill, action, com.bigbangcraft.bigbangskills.api.XpSource.FISHING);
        var reward = instance.preparedFishing.remove(hook.getUUID());
        if (reward == null) return;
        if (instance.skillConfig.fishingExtraFish()) player.drop(reward.stack(), false);
        instance.awardActivity(player, skill, BigDecimal.valueOf(reward.xp()), com.bigbangcraft.bigbangskills.api.XpSource.FISHING, false, true, "treasure_hunter." + reward.itemId());
    }

    public static ItemStack prepareFishingCatch(net.minecraft.world.entity.projectile.FishingHook hook, ItemStack vanilla) {
        var instance = INSTANCE;
        if (instance == null || !(hook.getPlayerOwner() instanceof ServerPlayer player)) return vanilla;
        var replacement = vanilla.is(ItemTags.FISHES) || !instance.skillConfig.fishingOverrideVanillaTreasures()
                ? vanilla : new ItemStack(net.minecraft.world.item.Items.SALMON);
        if (!instance.skillConfig.fishingDropsEnabled() || instance.progress == null) return replacement;
        var skill = SkillId.parse("bigbangskills:fishing");
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("fishing.treasure_hunter")).findFirst().orElse(null);
        if (state == null || ability == null || state.level() < ability.unlockLevel() || !instance.skillConfig.rule(skill).enabled()) return replacement;
        var reward = instance.fishingTreasures.roll(state.level(), instance.fishingLuckOfTheSea(player), instance.skillConfig.fishingLureModifier().doubleValue(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble).orElse(null);
        if (reward == null) return replacement;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.itemId()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return replacement;
        var stack = new ItemStack(item, reward.amount());
        if (reward.enchantable()) instance.fishingTreasures.magicHunterAll(state.level(), reward.rarity(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble).forEach(enchantment -> instance.applyFishingEnchantment(stack, player, enchantment));
        instance.preparedFishing.put(hook.getUUID(), new PreparedFishingReward(stack, reward.xp(), reward.itemId()));
        return instance.skillConfig.fishingExtraFish() ? replacement : stack;
    }

    public static void recordArrowOrigin(net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        var instance = INSTANCE;
        if (instance != null && arrow.getOwner() instanceof ServerPlayer
                && arrow.level() instanceof net.minecraft.server.level.ServerLevel) {
            instance.arrowOrigins.putIfAbsent(arrow.getUUID(), new ArrowOrigin(arrow.position(), arrow.level().getGameTime() + 2400,
                    arrow.getDeltaMovement().length() / 3.0));
        }
    }

    public static void recordFood(ServerPlayer player, ItemStack consumed) {
        var instance = INSTANCE;
        if (instance == null || consumed.isEmpty()) return;
        var itemId = BuiltInRegistries.ITEM.getKey(consumed.getItem()).toString();
        var path = BuiltInRegistries.ITEM.getKey(consumed.getItem()).getPath();
        if (instance.progress == null) return;
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var skill = SkillId.parse("bigbangskills:fishing");
        var fish = path.equals("cod") || path.equals("salmon") || path.equals("tropical_fish")
                || path.equals("cooked_cod") || path.equals("cooked_salmon")
                || instance.gameplay.xpForAction(skill, "food." + itemId).signum() > 0;
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(value -> value.id().equals("fishing.fishermans_diet")).findFirst().orElse(null);
        if (fish && state != null && ability != null && state.level() >= ability.unlockLevel()
                && instance.skillConfig.rule(skill).enabled()) {
            var food = player.getFoodData();
            food.setFoodLevel(Math.min(20, instance.fishing.fishermanDiet(state.level(), food.getFoodLevel())));
        }
        var herbalism = SkillId.parse("bigbangskills:herbalism");
        var herbalismState = profile == null ? null : profile.get(herbalism);
        var farmersDiet = DefaultAbilityCatalog.all().getOrDefault(herbalism, java.util.List.of()).stream()
                .filter(value -> value.id().equals("herbalism.farmers_diet")).findFirst().orElse(null);
        if (herbalismState != null && farmersDiet != null && herbalismState.level() >= farmersDiet.unlockLevel()
                && instance.skillConfig.rule(herbalism).enabled()
                && java.util.Set.of("apple", "baked_potato", "beetroot", "bread", "carrot", "chorus_fruit", "cookie", "dried_kelp", "melon_slice", "mushroom_stew", "potato", "pumpkin_pie", "sweet_berries").contains(path)) {
            var food = player.getFoodData();
            food.setFoodLevel(Math.min(20, instance.herbalism.farmersDiet(herbalismState.level(), food.getFoodLevel())));
        }
    }

    public static void recordShake(ServerPlayer player, LivingEntity target) {
        var instance = INSTANCE;
        if (instance == null || !target.isAlive() || instance.progress == null) return;
        var skill = SkillId.parse("bigbangskills:fishing");
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(value -> value.id().equals("fishing.shake")).findFirst().orElse(null);
        if (state == null || ability == null || state.level() < ability.unlockLevel()
                || !instance.skillConfig.rule(skill).enabled()
                || java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= instance.fishing.shakeChance(instance.fishing.shakeRank(state.level())) / 100.0) return;
        var count = instance.shakenTargets.getOrDefault(target.getUUID(), 0);
        if (count >= 4) return;
        var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        var reward = instance.fishingShake.roll(entityId, state.level(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble).orElse(null);
        if (reward == null) return;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.itemId()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return;
        instance.shakenTargets.put(target.getUUID(), count + 1);
        var level = player.serverLevel();
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, target.getX(), target.getY(), target.getZ(), shakeItem(item, reward)));
        target.hurt(player.damageSources().playerAttack(player), Math.min(Math.max(target.getMaxHealth() / 4.0F, 1.0F), 10.0F));
        instance.awardActivity(player, skill, "shake", com.bigbangcraft.bigbangskills.api.XpSource.FISHING);
    }

    private static ItemStack shakeItem(net.minecraft.world.item.Item item, FishingShakeEngine.Reward reward) {
        var stack = new ItemStack(item, reward.amount());
        if (!reward.potion().isBlank()) {
            var potion = switch (reward.potion()) {
                case "poison" -> net.minecraft.world.item.alchemy.Potions.POISON;
                case "instant_heal" -> net.minecraft.world.item.alchemy.Potions.HEALING;
                case "fire_resistance" -> net.minecraft.world.item.alchemy.Potions.FIRE_RESISTANCE;
                case "speed" -> net.minecraft.world.item.alchemy.Potions.SWIFTNESS;
                default -> null;
            };
            if (potion != null) stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                    net.minecraft.world.item.alchemy.PotionContents.EMPTY.withPotion(potion));
        }
        return stack;
    }

    public static int modifyFishingXp(net.minecraft.world.entity.projectile.FishingHook hook, int vanillaXp) {
        var instance = INSTANCE;
        var owner = hook.getPlayerOwner();
        if (instance == null || !(owner instanceof ServerPlayer player) || instance.progress == null) return vanillaXp;
        var skill = SkillId.parse("bigbangskills:fishing");
        if (!instance.skillConfig.rule(skill).enabled()) return vanillaXp;
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("fishing.treasure_hunter")).findFirst().orElse(null);
        var state = profile == null ? null : profile.get(skill);
        return state == null || ability == null ? vanillaXp : instance.fishing.boostedVanillaXp(vanillaXp, ability.rankForLevel(state.level()));
    }

    public static int[] fishingWaitReduction(net.minecraft.world.entity.projectile.FishingHook hook) {
        var instance = INSTANCE;
        var owner = hook.getPlayerOwner();
        if (instance == null || !(owner instanceof ServerPlayer player) || instance.progress == null) return new int[]{0, 0, 0, 0};
        var skill = SkillId.parse("bigbangskills:fishing");
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("fishing.master_angler")).findFirst().orElse(null);
        var state = profile == null ? null : profile.get(skill);
        if (state == null || ability == null || !instance.skillConfig.rule(skill).enabled()) return new int[]{0, 0, 0, 0};
        var boat = player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat;
        var lure = 0;
        var rod = player.getMainHandItem();
        var enchantments = rod.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var enchantment : enchantments.keySet()) if (enchantment.unwrapKey().map(key -> key.location().getPath()).orElse("").equals("lure")) lure = enchantments.getLevel(enchantment);
        return new int[]{instance.fishing.masterAnglerMinWaitReduction(state.level(), boat), instance.fishing.masterAnglerMaxWaitReduction(state.level(), boat, lure), instance.fishing.masterAnglerMinWaitCap(), instance.fishing.masterAnglerMaxWaitCap()};
    }

    public static void iceFishing(net.minecraft.world.entity.projectile.FishingHook hook) {
        var instance = INSTANCE;
        var owner = hook.getPlayerOwner();
        if (instance == null || !(owner instanceof ServerPlayer player) || !(hook.level() instanceof net.minecraft.server.level.ServerLevel level) || instance.progress == null) return;
        var skill = SkillId.parse("bigbangskills:fishing");
        var state = instance.progress.progress(player.getUUID()).flatMap(profile -> java.util.Optional.ofNullable(profile.get(skill))).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("fishing.ice_fishing")).findFirst().orElse(null);
        var pos = net.minecraft.core.BlockPos.containing(hook.getX(), hook.getY(), hook.getZ());
        if (state == null || ability == null || state.level() < ability.unlockLevel() || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ICE)) return;
        if (!level.getBlockState(pos.below(3)).is(net.minecraft.world.level.block.Blocks.WATER)) return;
        for (var x = -1; x <= 1; x++) for (var z = -1; z <= 1; z++) {
            var neighbor = pos.offset(x, 0, z);
            if (level.getBlockState(neighbor).is(net.minecraft.world.level.block.Blocks.ICE)) level.setBlock(neighbor, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        }
    }

    public static boolean processBlastExplosion(net.minecraft.world.level.Explosion explosion) {
        var instance = INSTANCE;
        var player = instance == null ? null : blastMiningOwner(explosion.getDirectSourceEntity());
        if (instance == null || player == null || instance.progress == null
                || !instance.abilities.isActive(player.getUUID(), "bigbangskills:mining.blast_mining", Instant.now())) return false;
        var tracker = instance.provenance;
        if (tracker == null || !tracker.reliable()) return false;
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        var skill = SkillId.parse("bigbangskills:mining");
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(value -> value.id().equals("mining.blast_mining")).findFirst().orElse(null);
        if (state == null || ability == null || !instance.skillConfig.rule(skill).enabled() || !instance.skillConfig.rule(skill).abilitiesEnabled()) return false;
        var rank = ability.rankForLevel(state.level());
        var engine = instance.blastMining;
        var bonusMultiplier = engine.bonusDropMultiplier(rank, instance.formulas.value("mining.blast_bonus_drops_enabled") > 0);
        var level = player.serverLevel();
        var xp = BigDecimal.ZERO;
        for (var pos : new java.util.ArrayList<>(explosion.getToBlow())) {
            var blockState = level.getBlockState(pos);
            if (blockState.isAir()) continue;
            var placed = tracker.wasPlaced(new BlockKey(worldId(level), pos.getX(), pos.getY(), pos.getZ()));
            var blockEntity = level.getBlockEntity(pos);
            var blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
            var drops = net.minecraft.world.level.block.Block.getDrops(blockState, level, pos, blockEntity, player, player.getMainHandItem());
            var miningXp = instance.gameplay.xpForBlock(skill, blockId);
            if (engine.illegalDrop(blockId)) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                continue;
            }
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (placed) continue;
            // ponytail: path-based ore check mirrors mcMMO's ore-only bonus without a cross-loader tag dependency.
            var ore = miningXp.signum() > 0 && (blockId.endsWith("_ore") || blockId.endsWith(":ancient_debris"));
            if (ore) {
                xp = xp.add(miningXp);
                var yield = engine.oreYield(rank);
                for (var remaining = yield; remaining > 0; remaining--) {
                    if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < Math.min(1, remaining)) {
                        drops.forEach(drop -> net.minecraft.world.level.block.Block.popResource(level, pos, drop.copy()));
                        if (bonusMultiplier > 1 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < instance.formulas.value("mining.blast_bonus_drop_chance") / 100.0)
                            for (var extra = 1; extra < bonusMultiplier; extra++) drops.forEach(drop -> net.minecraft.world.level.block.Block.popResource(level, pos, drop.copy()));
                    }
                }
            } else if (blockState.getBlock().asItem() != net.minecraft.world.item.Items.AIR
                    && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < .10) {
                net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(blockState.getBlock().asItem()));
            }
        }
        if (xp.signum() > 0) instance.awardActivity(player, skill, xp, com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, false, true, "blast_mining");
        // Keep vanilla entity damage and particles; only the block list was already resolved above.
        clearExplosionProvenance(level, explosion);
        explosion.getToBlow().clear();
        return true;
    }

    public static void recordTaming(net.minecraft.world.entity.TamableAnimal animal, ServerPlayer player) {
        var instance = INSTANCE;
        if (instance != null) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
            instance.awardActivity(player, SkillId.parse("bigbangskills:taming"), "animal_taming." + id, com.bigbangcraft.bigbangskills.api.XpSource.TAMING);
        }
    }

    private static void recordArrowRetrieval(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        var instance = INSTANCE;
        if (instance == null || target instanceof ServerPlayer || !(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Arrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player) || instance.progress == null) return;
        var skill = SkillId.parse("bigbangskills:archery");
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        if (profile == null || !instance.skillConfig.rule(skill).enabled()
                || !instance.combat.arrowRetrieval(profile)) return;
        player.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.ARROW));
    }

    public static boolean tryTrickShot(net.minecraft.world.entity.projectile.AbstractArrow arrow, net.minecraft.world.phys.BlockHitResult hit) {
        var instance = INSTANCE;
        if (instance == null || !(arrow.getOwner() instanceof ServerPlayer player) || !(arrow.getWeaponItem().getItem() instanceof CrossbowItem)
                || instance.progress == null) return false;
        var skill = SkillId.parse("bigbangskills:crossbows");
        var profile = instance.progress.progress(player.getUUID()).orElse(null);
        if (profile == null || !instance.skillConfig.rule(skill).enabled()) return false;
        var maxBounces = instance.combat.trickShotBounces(profile);
        var current = instance.trickShotBounces.getOrDefault(arrow.getUUID(), 0);
        var velocity = arrow.getDeltaMovement();
        if (current >= maxBounces || velocity.lengthSqr() < 0.0001) return false;
        var normalInt = hit.getDirection().getNormal();
        var normal = new net.minecraft.world.phys.Vec3(normalInt.getX(), normalInt.getY(), normalInt.getZ());
        if (current == 0 && velocity.normalize().dot(normal.scale(-1)) > Math.cos(Math.PI / 4)) return false;
        var reflected = velocity.subtract(normal.scale(2 * velocity.dot(normal)));
        instance.trickShotBounces.put(arrow.getUUID(), current + 1);
        var point = hit.getLocation();
        arrow.setPos(point.x + normal.x() * .05, point.y + normal.y() * .05, point.z + normal.z() * .05);
        arrow.setDeltaMovement(reflected.scale(.75));
        return true;
    }

    public static float modifyPetDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        var instance = INSTANCE;
        if (instance == null || !(entity instanceof net.minecraft.world.entity.TamableAnimal pet)
                || !(pet.getOwner() instanceof ServerPlayer owner) || instance.progress == null) return amount;
        var profile = instance.progress.progress(owner.getUUID()).orElse(null);
        if (profile == null) return amount;
        var skill = SkillId.parse("bigbangskills:taming");
        if (!instance.skillConfig.rule(skill).enabled()) return amount;
        if ((source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                || source.is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) && instance.taming.hasAbility(profile, "holy_hound"))
            pet.setHealth(Math.min(pet.getMaxHealth(), pet.getHealth() + amount));
        var environmental = source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS) || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA)
                || source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE);
        var reduced = instance.taming.incomingDamage(profile, amount, source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION),
                source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE), source.is(net.minecraft.tags.DamageTypeTags.IS_FALL), environmental, pet.getHealth());
        if (environmental && reduced == 0) pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        return (float) reduced;
    }

    public static float modifyIncomingDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        var instance = INSTANCE;
        if (instance == null) return amount;
        var reduced = instance.modifyPetDamage(entity, source, amount);
        if (instance.progress == null || reduced <= 0) return reduced;
        var acrobaticsSkill = SkillId.parse("bigbangskills:acrobatics");
        var unarmedSkill = SkillId.parse("bigbangskills:unarmed");
        if (entity instanceof ServerPlayer player) {
            var profile = instance.progress.progress(player.getUUID()).orElse(null);
            if (profile != null && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return instance.modifyFallDamage(player, reduced, profile);
            var blastOwner = blastMiningOwner(source);
            if (blastOwner != null
                    && instance.abilities.isActive(blastOwner.getUUID(), "bigbangskills:mining.blast_mining", Instant.now())) {
                if (blastOwner != player) return Math.min(reduced, 24.0F);
                var skill = SkillId.parse("bigbangskills:mining");
                var state = profile == null ? null : profile.get(skill);
                var ability = com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                        .filter(value -> value.id().equals("mining.demolitions_expertise")).findFirst().orElse(null);
                if (state != null && ability != null && state.level() >= ability.unlockLevel()) {
                    var rank = ability.rankForLevel(state.level());
                    reduced *= (float) (1.0 - instance.blastMining.damageReductionPercent(rank) / 100.0);
                }
            }
            if (profile != null && source.getEntity() != null && source.getEntity() != player) {
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
                        && instance.skillConfig.rule(unarmedSkill).enabled()
                        && instance.combat.arrowDeflect(profile)) return 0;
                if (instance.skillConfig.rule(acrobaticsSkill).enabled()
                        && !(source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING) && instance.formulas.value("acrobatics.prevent_dodge_lightning") > 0)) {
                    var dodge = instance.acrobatics.resolveDodge(profile, reduced, player.getHealth());
                    if (dodge.dodgeTriggered()) {
                        if (source.getEntity() instanceof net.minecraft.world.entity.Mob)
                            instance.awardActivity(player, acrobaticsSkill, instance.gameplay.xpForAction(acrobaticsSkill, "dodge").multiply(BigDecimal.valueOf(reduced)), com.bigbangcraft.bigbangskills.api.XpSource.INTEGRATION, false, true, "dodge");
                        return (float) (reduced * dodge.damageMultiplier());
                    }
                }
            }
        }
        var attacker = attacker(source);
        if (attacker != null && attacker != entity && instance.progress != null) {
            var skill = instance.combatSkill(source, attacker);
            var combatProfile = progressProfile(instance, attacker);
            if (skill != null && combatProfile != null) {
                var pvp = entity instanceof ServerPlayer;
                var targetId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                var quality = armorQuality(entity);
                var action = new CombatAction(attacker.getUUID(), skill, BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).toString(), instance.combatXp(source, entity, targetId, skill, pvp), reduced, attacker.getAttackStrengthScale(0.5F), pvp, quality > 0, quality, instance.abilityActive(attacker, skill), ProgressionScope.server("default"));
                var resolution = instance.combat.resolve(combatProfile, action);
                PENDING_COMBAT.get().addLast(new PendingCombat(attacker, entity, resolution));
                return (float) ((reduced + resolution.effect().bonusDamage()) * resolution.effect().damageMultiplier());
            }
        }
        return reduced;
    }

    private float modifyFallDamage(ServerPlayer player, float amount, com.bigbangcraft.bigbangskills.common.progression.PlayerProgress profile) {
        var distance = Math.max(0, player.fallDistance);
        var effect = acrobatics.resolve(profile, distance, player.isCrouching(), player.getHealth());
        var skill = SkillId.parse("bigbangskills:acrobatics");
        var xpAction = effect.rollTriggered() ? "roll" : "fall";
        var xp = gameplay.xpForAction(skill, xpAction).multiply(BigDecimal.valueOf(Math.min(20, Math.max(0, distance - 3))));
        if (hasFeatherFalling(player.getItemBySlot(EquipmentSlot.FEET))) xp = xp.multiply(gameplay.xpForAction(skill, "featherfall_multiplier"));
        var cooldown = acrobaticsTeleportCooldowns.computeIfPresent(player.getUUID(), (id, until) -> until > System.currentTimeMillis() ? until : null);
        var result = cooldown == null ? progress.award(new SkillAwardAction(player.getUUID(), skill, xp,
                com.bigbangcraft.bigbangskills.api.XpSource.FALL, "fall", ProgressionScope.server("default"), true, false, false, true)) : null;
        return result != null && result.accepted() && effect.rollTriggered() ? amount * (float) effect.damageMultiplier() : amount;
    }

    private static com.bigbangcraft.bigbangskills.common.progression.PlayerProgress progressProfile(FabricBootstrap instance, ServerPlayer player) {
        return instance.progress == null ? null : instance.progress.progress(player.getUUID()).orElse(null);
    }

    private static int armorQuality(LivingEntity entity) {
        var quality = 0;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var id = BuiltInRegistries.ITEM.getKey(entity.getItemBySlot(slot).getItem()).getPath();
            quality += id.contains("netherite") ? 4 : id.contains("diamond") ? 3 : id.contains("iron") || id.contains("chainmail") ? 2 : id.contains("gold") || id.contains("leather") ? 1 : 0;
        }
        return quality;
    }

    public static void recordRepair(ServerPlayer player) {
        var instance = INSTANCE;
        if (instance != null) instance.awardActivity(player, SkillId.parse("bigbangskills:repair"), "base", com.bigbangcraft.bigbangskills.api.XpSource.REPAIR);
    }

    public static void recordRepair(ServerPlayer player, ItemStack input, ItemStack output) {
        var instance = INSTANCE;
        if (instance == null || input.isEmpty() || output.isEmpty() || !input.isDamageableItem() || !output.isDamageableItem()) return;
        var repaired = input.getDamageValue() - output.getDamageValue();
        if (repaired <= 0 || input.getMaxDamage() <= 0) return;
        var profile = instance.progress == null ? null : instance.progress.progress(player.getUUID()).orElse(null);
        var skill = SkillId.parse("bigbangskills:repair");
        if (profile != null && instance.skillConfig.rule(skill).enabled()) {
            var state = profile.get(skill);
            if (state != null) {
                applyArcaneForging(output, state.level());
                var improved = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(instance.formulas, java.util.concurrent.ThreadLocalRandom.current()::nextDouble)
                        .repairedDurability(input.getDamageValue(), repaired, state.level());
                output.setDamageValue(Math.max(0, input.getDamageValue() - improved));
                repaired = input.getDamageValue() - output.getDamageValue();
            }
        }
        var material = instance.repairMaterial(input);
        var amount = instance.gameplay.xpForAction(skill, "base")
                .multiply(instance.gameplay.xpForAction(skill, material))
                .multiply(BigDecimal.valueOf(repaired).divide(BigDecimal.valueOf(input.getMaxDamage()), 8, java.math.RoundingMode.DOWN));
        instance.awardActivity(player, skill, amount, com.bigbangcraft.bigbangskills.api.XpSource.REPAIR, false, true, "anvil_repair");
    }

    private static void applyArcaneForging(ItemStack stack, int level) {
        var skill = SkillId.parse("bigbangskills:repair");
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("repair.arcane_forging")).findFirst().orElse(null);
        if (ability == null || level < ability.unlockLevel()) return;
        var enchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;
        var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(enchantments);
        var instance = INSTANCE;
        if (instance == null) return;
        var engine = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(instance.formulas, java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
        for (var enchantment : new java.util.ArrayList<>(enchantments.keySet())) {
            var next = engine.arcaneForgingLevel(ability.rankForLevel(level), enchantments.getLevel(enchantment));
            if (next == 0) mutable.removeIf(value -> value.equals(enchantment)); else mutable.set(enchantment, next);
        }
        stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private String repairMaterial(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var configured = itemTables.repairMaterial(id);
        if (configured != null) return configured;
        // ponytail: unknown modded tools use the neutral category until configured explicitly.
        return "other";
    }

    public static void recordSmelting(ServerPlayer player, ItemStack output) {
        var instance = INSTANCE;
        if (instance != null) {
            var outputPath = BuiltInRegistries.ITEM.getKey(output.getItem()).getPath();
            var action = switch (outputPath) {
                case "iron_ingot" -> "raw_iron";
                case "gold_ingot" -> "raw_gold";
                case "copper_ingot" -> "raw_copper";
                case "netherite_scrap" -> "ancient_debris";
                case "diamond" -> "diamond_ore";
                case "emerald" -> "emerald_ore";
                case "coal" -> "coal_ore";
                case "lapis_lazuli" -> "lapis_ore";
                case "redstone" -> "redstone_ore";
                case "quartz" -> "nether_quartz_ore";
                default -> outputPath;
            };
            instance.awardActivity(player, SkillId.parse("bigbangskills:smelting"), action, com.bigbangcraft.bigbangskills.api.XpSource.SMELTING);
        }
    }

    public static void recordBrewingOwner(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world, ServerPlayer player) {
        var instance = INSTANCE;
        if (instance != null) instance.brewingOwners.put(instance.brewingKey(world, pos), player.getUUID());
    }

    public static boolean allowAlchemyHopperTransfer(net.minecraft.world.Container destination, ItemStack stack) {
        var instance = INSTANCE;
        if (instance == null) return true;
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (instance.skillConfig.blocksAlchemyHopperTransfer(itemId)) return false;
        if (destination instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
            var key = instance.brewingKey(stand.getLevel(), stand.getBlockPos());
            instance.hopperAlchemyTransfers.put(key, stand.getLevel().getGameTime() + 200L);
        }
        return true;
    }

    public static void recordBrewingBefore(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world, net.minecraft.core.NonNullList<ItemStack> items) {
        var instance = INSTANCE;
        if (instance == null) return;
        var bottles = new ItemStack[3];
        for (var i = 0; i < 3; i++) bottles[i] = items.get(i).copy();
        instance.brewingInputs.put(instance.brewingKey(world, pos), bottles);
    }

    public static void recordBrewingAfter(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world, net.minecraft.core.NonNullList<ItemStack> items) {
        var instance = INSTANCE;
        if (instance == null) return;
        var key = instance.brewingKey(world, pos);
        var hopperTransfer = instance.hopperAlchemyTransfers.remove(key) != null;
        var before = instance.brewingInputs.remove(key);
        if (hopperTransfer && !instance.skillConfig.alchemyEnabledForHoppers()) return;
        var playerId = instance.brewingOwners.get(key);
        var player = playerId == null || world.getServer() == null ? null : world.getServer().getPlayerList().getPlayer(playerId);
        if (before == null || player == null) return;
        for (var i = 0; i < 3; i++) {
            var after = items.get(i);
            if (!before[i].isEmpty() && !after.isEmpty() && !ItemStack.matches(before[i], after)) {
                instance.awardActivity(player, SkillId.parse("bigbangskills:alchemy"),
                        "potion_brewing.stage_" + potionStage(before[i], after), com.bigbangcraft.bigbangskills.api.XpSource.ALCHEMY);
            }
        }
    }

    private static int potionStage(ItemStack input, ItemStack output) {
        var stage = potionStage(output);
        var inputPotion = input.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        if (!isWater(inputPotion) && potionStage(input) == stage) return 5;
        return stage;
    }

    private static int potionStage(ItemStack stack) {
        var contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        var stage = contents.hasEffects() ? 2 : 1;
        var potion = contents.potion().map(holder -> BuiltInRegistries.POTION.getKey(holder.value()).getPath()).orElse("");
        var amplifier = false;
        for (var effect : contents.getAllEffects()) amplifier |= effect.getAmplifier() > 0;
        if (potion.contains("strong") || amplifier) stage++;
        if (potion.contains("long")) stage++;
        if (stack.is(net.minecraft.world.item.Items.SPLASH_POTION) || stack.is(net.minecraft.world.item.Items.LINGERING_POTION)) stage++;
        return Math.min(5, stage);
    }

    private static boolean isWater(net.minecraft.world.item.alchemy.PotionContents contents) {
        return contents.potion().map(holder -> BuiltInRegistries.POTION.getKey(holder.value()).getPath().equals("water")).orElse(false);
    }

    public static int brewingExtraTicks(net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        var instance = INSTANCE;
        if (instance == null || world.getServer() == null) return 0;
        var key = instance.brewingKey(world, pos);
        if (instance.hopperAlchemyTransfers.getOrDefault(key, 0L) > world.getGameTime()
                && !instance.skillConfig.alchemyEnabledForHoppers()) return 0;
        var playerId = instance.brewingOwners.get(key);
        var player = playerId == null ? null : world.getServer().getPlayerList().getPlayer(playerId);
        if (player == null || instance.progress == null) return 0;
        var profile = instance.progress.progress(playerId).orElse(null);
        if (profile == null) return 0;
        var alchemy = SkillId.parse("bigbangskills:alchemy");
        if (!instance.skillConfig.rule(alchemy).enabled()) return 0;
        var state = profile.get(alchemy);
        var catalysis = DefaultAbilityCatalog.all().getOrDefault(alchemy, java.util.List.of()).stream()
                .filter(definition -> definition.id().equals("alchemy.catalysis")).findFirst().orElse(null);
        if (state == null || catalysis == null) return 0;
        var speed = new com.bigbangcraft.bigbangskills.common.skill.AlchemyEngine().brewSpeed(state.level(), catalysis.unlockLevel(), false,
                instance.formulas.value("alchemy.catalysis_min_speed"), instance.formulas.value("alchemy.catalysis_max_speed"), (int) instance.formulas.value("alchemy.catalysis_max_level"));
        return speed <= 1 ? 0 : Math.max(0, (int) Math.ceil(speed - 1));
    }

    public static boolean tryConcoction(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.NonNullList<ItemStack> items) {
        var instance = INSTANCE;
        if (instance == null || world.getServer() == null || items.size() < 5) return false;
        var key = instance.brewingKey(world, pos);
        if (instance.hopperAlchemyTransfers.getOrDefault(key, 0L) > world.getGameTime()
                && !instance.skillConfig.alchemyEnabledForHoppers()) return false;
        var playerId = instance.brewingOwners.get(key);
        var player = playerId == null ? null : world.getServer().getPlayerList().getPlayer(playerId);
        var skill = SkillId.parse("bigbangskills:alchemy");
        var profile = playerId == null || instance.progress == null ? null : instance.progress.progress(playerId).orElse(null);
        var state = profile == null ? null : profile.get(skill);
        var ingredient = BuiltInRegistries.ITEM.getKey(items.get(3).getItem()).toString();
        var engine = new com.bigbangcraft.bigbangskills.common.skill.AlchemyEngine();
        var recipe = instance.alchemyConcoctions.recipe(ingredient);
        var tier = recipe == null ? engine.concoctionTier(ingredient) : recipe.tier();
        if (player == null || state == null || tier == 0 || engine.concoctionsRank(state.level()) < tier
                || !instance.skillConfig.rule(skill).enabled()) return false;
        var configuredEffect = recipe == null || recipe.effectId() == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, ResourceLocation.parse(recipe.effectId()))).orElse(null);
        var effect = configuredEffect == null ? switch (ingredient) {
            case "minecraft:carrot" -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0);
            case "minecraft:slime_ball" -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3600, 0);
            case "minecraft:phantom_membrane" -> new MobEffectInstance(MobEffects.SLOW_FALLING, 3600, 0);
            case "minecraft:quartz" -> new MobEffectInstance(MobEffects.ABSORPTION, 3600, 0);
            case "minecraft:rabbit_foot" -> new MobEffectInstance(MobEffects.JUMP, 3600, 1);
            case "minecraft:apple" -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0);
            case "minecraft:rotten_flesh" -> new MobEffectInstance(MobEffects.HUNGER, 3600, 0);
            case "minecraft:brown_mushroom" -> new MobEffectInstance(MobEffects.CONFUSION, 3600, 0);
            case "minecraft:ink_sac" -> new MobEffectInstance(MobEffects.BLINDNESS, 3600, 0);
            case "minecraft:fern" -> new MobEffectInstance(MobEffects.SATURATION, 1, 0);
            case "minecraft:poisonous_potato" -> new MobEffectInstance(MobEffects.WITHER, 3600, 0);
            case "minecraft:golden_apple" -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 0);
            default -> null;
        } : new MobEffectInstance(configuredEffect, recipe.durationTicks(), recipe.amplifier());
        if (effect == null) return false;
        var changed = false;
        for (var slot = 0; slot < 3; slot++) {
            var bottle = items.get(slot);
            if (bottle.is(net.minecraft.world.item.Items.POTION) || bottle.is(net.minecraft.world.item.Items.SPLASH_POTION) || bottle.is(net.minecraft.world.item.Items.LINGERING_POTION)) {
                var contents = bottle.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
                bottle.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, contents.withEffectAdded(effect));
                changed = true;
            }
        }
        if (!changed) return false;
        items.get(3).shrink(1);
        instance.recordBrewingAfter(pos, world, items);
        return true;
    }

    public static void recordSmeltingOwner(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world, ServerPlayer player) {
        var instance = INSTANCE;
        if (instance != null) instance.smeltingOwners.put(instance.brewingKey(world, pos), player.getUUID());
    }

    public static int smeltingFuelDuration(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace, int vanillaDuration) {
        var instance = INSTANCE;
        var playerId = instance == null ? null : instance.smeltingOwners.get(instance.brewingKey(furnace.getLevel(), furnace.getBlockPos()));
        var player = instance == null || playerId == null || furnace.getLevel().getServer() == null ? null : furnace.getLevel().getServer().getPlayerList().getPlayer(playerId);
        var profile = player == null || instance.progress == null ? null : instance.progress.progress(playerId).orElse(null);
        if (profile == null) return vanillaDuration;
        var skill = SkillId.parse("bigbangskills:smelting");
        if (!instance.skillConfig.rule(skill).enabled()) return vanillaDuration;
        var state = profile.get(skill);
        var definition = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("smelting.fuel_efficiency")).findFirst().orElse(null);
        return state == null || definition == null ? vanillaDuration : new com.bigbangcraft.bigbangskills.common.skill.SmeltingEngine().fuelEfficiency(vanillaDuration, definition.rankForLevel(state.level()));
    }

    public static int smeltingVanillaXp(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace, int vanillaXp) {
        var instance = INSTANCE;
        if (furnace == null) return vanillaXp;
        var playerId = instance == null ? null : instance.smeltingOwners.get(instance.brewingKey(furnace.getLevel(), furnace.getBlockPos()));
        var profile = instance == null || playerId == null || instance.progress == null ? null : instance.progress.progress(playerId).orElse(null);
        var skill = SkillId.parse("bigbangskills:smelting");
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(definition -> definition.id().equals("smelting.understanding_the_art")).findFirst().orElse(null);
        if (instance == null || profile == null || state == null || ability == null || !instance.skillConfig.rule(skill).enabled()) return vanillaXp;
        return new com.bigbangcraft.bigbangskills.common.skill.SmeltingEngine(instance.formulas).vanillaXp(vanillaXp, ability.rankForLevel(state.level()));
    }

    public static void beginSmeltingXp(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) { CURRENT_SMELTING_FURNACE.set(furnace); }
    public static void endSmeltingXp() { CURRENT_SMELTING_FURNACE.remove(); }
    public static void recordTeleport(ServerPlayer player, net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        var instance = INSTANCE;
        if (instance != null && (player.level().dimension() != level.dimension() || player.position().distanceToSqr(x, y, z) > 0.000001)
                && instance.formulas.value("acrobatics.xp_after_teleport_cooldown_seconds") > 0) {
            instance.acrobaticsTeleportCooldowns.put(player.getUUID(), System.currentTimeMillis() + (long) (instance.formulas.value("acrobatics.xp_after_teleport_cooldown_seconds") * 1000));
        }
    }
    public static float smeltingRecipeXp(float vanillaXp) {
        var furnace = CURRENT_SMELTING_FURNACE.get();
        var instance = INSTANCE;
        if (furnace == null || instance == null) return vanillaXp;
        var playerId = instance.smeltingOwners.get(instance.brewingKey(furnace.getLevel(), furnace.getBlockPos()));
        var profile = playerId == null || instance.progress == null ? null : instance.progress.progress(playerId).orElse(null);
        var skill = SkillId.parse("bigbangskills:smelting");
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(definition -> definition.id().equals("smelting.understanding_the_art")).findFirst().orElse(null);
        if (profile == null || state == null || ability == null || !instance.skillConfig.rule(skill).enabled()) return vanillaXp;
        return new com.bigbangcraft.bigbangskills.common.skill.SmeltingEngine(instance.formulas).vanillaXp(vanillaXp, ability.rankForLevel(state.level()));
    }

    public static void processSecondSmelt(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
        var instance = INSTANCE;
        var level = furnace.getLevel();
        if (instance == null || level == null || level.getServer() == null) return;
        var key = instance.brewingKey(level, furnace.getBlockPos());
        var output = ((net.minecraft.world.Container) furnace).getItem(2);
        if (output.isEmpty()) { instance.smeltingOutputs.remove(key); return; }
        var item = BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
        var previous = instance.smeltingOutputs.put(key, new FurnaceOutput(item, output.getCount()));
        if (previous == null || !previous.item().equals(item) || output.getCount() <= previous.count()) return;
        var engine = new com.bigbangcraft.bigbangskills.common.skill.SmeltingEngine();
        if (!engine.canSecondSmelt(output.getCount(), output.getMaxStackSize())) return;
        var playerId = instance.smeltingOwners.get(key);
        var profile = playerId == null || instance.progress == null ? null : instance.progress.progress(playerId).orElse(null);
        if (profile == null) return;
        var skill = SkillId.parse("bigbangskills:smelting");
        if (!instance.skillConfig.rule(skill).enabled()) return;
        var state = profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(definition -> definition.id().equals("smelting.second_smelt")).findFirst().orElse(null);
        if (state == null || ability == null || state.level() < ability.unlockLevel()) return;
        var produced = output.getCount() - previous.count();
        var bonus = 0;
        for (var i = 0; i < produced; i++) if (engine.secondSmelt(state.level(), true, java.util.concurrent.ThreadLocalRandom.current().nextDouble(), instance.formulas.value("smelting.second_smelt_max_percent"), (int) instance.formulas.value("smelting.second_smelt_max_level"))) bonus++;
        if (bonus > 0) { output.grow(bonus); furnace.setItem(2, output); furnace.setChanged(); }
        instance.smeltingOutputs.put(key, new FurnaceOutput(item, output.getCount()));
    }

    private record FurnaceOutput(String item, int count) {}

    private String brewingKey(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        return world.dimension().location() + ":" + pos.asLong();
    }

    private boolean salvageBlock(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString().equals(formulas.salvageAnvilBlock());
    }

    private boolean repairBlock(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString().equals(formulas.repairAnvilBlock());
    }

    private boolean repair(ServerPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        var stack = player.getItemInHand(hand);
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var tableRule = itemTables.repairRule(itemId);
        var category = tableRule == null ? null : tableRule.category();
        if (category == null || !stack.isDamageableItem()) return false;
        if (stack.getCount() != 1 || stack.getDamageValue() <= 0) return true;
        var profile = progress == null ? null : progress.progress(player.getUUID()).orElse(null);
        if (profile == null) return true;
        if (!confirmRepair(player, stack, world)) return true;
        var materialId = com.bigbangcraft.bigbangskills.common.skill.RepairEngine.materialItem(category, itemId);
        var material = materialId == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(materialId));
        var skill = SkillId.parse("bigbangskills:repair");
        var materialStack = material == null || material == net.minecraft.world.item.Items.AIR ? null : repairMaterialStack(player, material);
        if (materialStack == null) return true;
        var state = profile.get(skill);
        if (state == null || !skillConfig.rule(skill).enabled()) return true;
        var quantity = tableRule.minimumQuantity() > 0 ? tableRule.minimumQuantity() : com.bigbangcraft.bigbangskills.common.skill.RepairEngine.minimumQuantity(itemId);
        var multiplier = tableRule.xpMultiplier() >= 0 ? tableRule.xpMultiplier() : com.bigbangcraft.bigbangskills.common.skill.RepairEngine.xpMultiplier(itemId);
        var base = Math.max(1, stack.getMaxDamage() / quantity);
        var repaired = new com.bigbangcraft.bigbangskills.common.skill.RepairEngine(formulas, java.util.concurrent.ThreadLocalRandom.current()::nextDouble)
                .repairedDurability(stack.getDamageValue(), base, state.level());
        if (repaired <= 0) return true;
        applyArcaneForging(stack, state.level());
        stack.setDamageValue(stack.getDamageValue() - repaired);
        materialStack.shrink(1);
        var amount = gameplay.xpForAction(skill, "base")
                .multiply(gameplay.xpForAction(skill, category))
                .multiply(BigDecimal.valueOf(multiplier))
                .multiply(BigDecimal.valueOf(repaired).divide(BigDecimal.valueOf(stack.getMaxDamage()), 8, java.math.RoundingMode.DOWN));
        awardActivity(player, skill, amount, com.bigbangcraft.bigbangskills.api.XpSource.REPAIR, false, true, "station_repair");
        if (formulas.value("repair.use_sounds_enabled") > 0) world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private ItemStack repairMaterialStack(ServerPlayer player, net.minecraft.world.item.Item material) {
        for (var candidate : player.getInventory().items) {
            if (candidate.is(material) && (formulas.value("repair.use_enchanted_materials") > 0 || !candidate.isEnchanted())) return candidate;
        }
        return null;
    }

    private boolean confirmRepair(ServerPlayer player, ItemStack stack, net.minecraft.world.level.Level world) {
        if (formulas.value("repair.confirmation_required") <= 0) return true;
        var now = world.getGameTime();
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var previous = pendingRepairs.get(player.getUUID());
        if (previous != null && previous.itemId().equals(itemId) && previous.damage() == stack.getDamageValue() && previous.expiresAt() >= now) {
            pendingRepairs.remove(player.getUUID());
            return true;
        }
        pendingRepairs.put(player.getUUID(), new PendingSalvage(itemId, stack.getDamageValue(), now + 60));
        if (formulas.value("repair.messages_enabled") > 0) player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("repair.confirm", SkillMessages.locale(player.clientInformation().language()))));
        return false;
    }

    public static void clearExplosionProvenance(net.minecraft.world.level.Level level, net.minecraft.world.level.Explosion explosion) {
        var instance = INSTANCE;
        var tracker = instance == null ? null : instance.provenance;
        if (tracker == null) return;
        for (var pos : explosion.getToBlow()) tracker.clear(new BlockKey(worldId(level), pos.getX(), pos.getY(), pos.getZ()));
    }

    private boolean salvage(ServerPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        var stack = player.getItemInHand(hand);
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var rule = itemTables.salvageRule(itemId);
        if (rule == null || !stack.isDamageableItem()) return false;
        var profile = progress == null ? null : progress.progress(player.getUUID()).orElse(null);
        if (profile == null) return true;
        if (!confirmSalvage(player, stack, world)) return true;
        var result = salvage.resolve(profile, itemId, stack.getDamageValue(), stack.getMaxDamage(), rule, stack.isEnchanted());
        if (!result.accepted()) { if (formulas.value("salvage.messages_enabled") > 0) player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("salvage.unavailable", SkillMessages.locale(player.clientInformation().language()), result.reason()))); return true; }
        var material = BuiltInRegistries.ITEM.get(ResourceLocation.parse(rule.resultId()));
        if (material == null || material == net.minecraft.world.item.Items.AIR) return true;
        var arcaneBooks = arcaneSalvageDrops(stack, profile);
        stack.shrink(1);
        if (world instanceof net.minecraft.server.level.ServerLevel level) Block.popResource(level, pos.above(), new ItemStack(material, result.yield()));
        if (world instanceof net.minecraft.server.level.ServerLevel level) for (var book : arcaneBooks) Block.popResource(level, pos.above(), book);
        if (formulas.value("salvage.use_sounds_enabled") > 0) world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private boolean confirmSalvage(ServerPlayer player, ItemStack stack, net.minecraft.world.level.Level world) {
        if (formulas.value("salvage.confirmation_required") <= 0) return true;
        var now = world.getGameTime();
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var previous = pendingSalvages.get(player.getUUID());
        if (previous != null && previous.itemId().equals(itemId) && previous.damage() == stack.getDamageValue() && previous.expiresAt() >= now) {
            pendingSalvages.remove(player.getUUID());
            return true;
        }
        pendingSalvages.put(player.getUUID(), new PendingSalvage(itemId, stack.getDamageValue(), now + 60));
        if (formulas.value("salvage.messages_enabled") > 0) player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("salvage.confirm", SkillMessages.locale(player.clientInformation().language()))));
        return false;
    }

    private record PendingSalvage(String itemId, int damage, long expiresAt) {}

    private java.util.List<ItemStack> arcaneSalvageDrops(ItemStack stack, com.bigbangcraft.bigbangskills.common.progression.PlayerProgress profile) {
        var skill = SkillId.parse("bigbangskills:salvage");
        var state = profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("salvage.arcane_salvage")).findFirst().orElse(null);
        if (state == null || ability == null || !stack.isEnchanted() || state.level() < ability.unlockLevel()) return java.util.List.of();
        var enchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        var books = new java.util.ArrayList<ItemStack>();
        var engine = new SalvageEngine();
        for (var enchantment : enchantments.keySet()) {
            var level = engine.arcaneSalvageLevel(ability.rankForLevel(state.level()), enchantments.getLevel(enchantment), java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
            if (level <= 0) continue;
            var book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            var stored = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            stored.set(enchantment, level);
            book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
            books.add(book);
        }
        return books;
    }

    @Override public void onInitialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            return activateItemAbility(serverPlayer)
                    ? InteractionResultHolder.success(player.getItemInHand(hand))
                    : InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && world.getBlockState(hit.getBlockPos()).getBlock() instanceof BrewingStandBlock) {
                recordBrewingOwner(hit.getBlockPos(), world, serverPlayer);
            }
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && world.getBlockState(hit.getBlockPos()).getBlock() instanceof AbstractFurnaceBlock) {
                recordSmeltingOwner(hit.getBlockPos(), world, serverPlayer);
            }
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && serverPlayer.getItemInHand(hand).getItem() instanceof BlockItem) {
                var target = hit.getBlockPos().relative(hit.getDirection());
                var replaceable = world.getBlockState(target).canBeReplaced();
                world.getServer().execute(() -> {
                    var state = world.getBlockState(target);
                    if (replaceable && trackedSkillBlock(state)) markPlaced(world, target);
                    if (replaceable && BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(formulas.repairAnvilBlock()) && formulas.value("repair.placed_sounds_enabled") > 0) world.playSound(null, target, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (replaceable && BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(formulas.salvageAnvilBlock()) && formulas.value("salvage.placed_sounds_enabled") > 0) world.playSound(null, target, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
                });
            }
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && serverPlayer.getItemInHand(hand).getItem() instanceof BucketItem) {
                var target = hit.getBlockPos().relative(hit.getDirection());
                world.getServer().execute(() -> markFluidPlacement(world, target));
            }
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && herbalismInteraction(serverPlayer, hand, hit.getBlockPos(), world)) return InteractionResult.SUCCESS;
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && repairBlock(world, hit.getBlockPos()) && repair(serverPlayer, hand, hit.getBlockPos(), world)) return InteractionResult.SUCCESS;
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && salvageBlock(world, hit.getBlockPos()) && salvage(serverPlayer, hand, hit.getBlockPos(), world)) return InteractionResult.SUCCESS;
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && remoteBlastMining(serverPlayer, hit.getBlockPos(), world)) return InteractionResult.SUCCESS;
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide() && activateBlockAbility(serverPlayer, world.getBlockState(hit.getBlockPos()))) return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()) return InteractionResult.PASS;
            if (callOfWild(serverPlayer, world) || blockCracker(serverPlayer, pos, world)) return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> player instanceof ServerPlayer serverPlayer && !world.isClientSide() && beastLore(serverPlayer, entity) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            var key = new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ());
            var tracker = provenance;
            var placed = tracker != null && tracker.wasPlaced(key);
            var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            var wood = state.is(BlockTags.LOGS) || state.is(WOODCUTTING)
                    || (serverPlayer.getMainHandItem().getItem() instanceof AxeItem && gameplay.hasBlockXp(SkillId.parse("bigbangskills:woodcutting"), blockId));
            var mining = !wood && (state.is(MINING) || (serverPlayer.getMainHandItem().getItem() instanceof PickaxeItem
                    && (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || gameplay.hasBlockXp(SkillId.parse("bigbangskills:mining"), blockId))));
            var excavation = !wood && !mining && (state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || (serverPlayer.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShovelItem
                    && gameplay.hasBlockXp(SkillId.parse("bigbangskills:excavation"), blockId)));
            var herbalism = !wood && !mining && !excavation && (state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
                    || gameplay.hasBlockXp(SkillId.parse("bigbangskills:herbalism"), blockId)) && herbalismMature(state);
            var abilityActive = (mining && (abilities.isActive(serverPlayer.getUUID(), "bigbangskills:mining.super_breaker", Instant.now()) || abilities.isActive(serverPlayer.getUUID(), "bigbangskills:mining.blast_mining", Instant.now())))
                    || (wood && abilities.isActive(serverPlayer.getUUID(), "bigbangskills:woodcutting.tree_feller", Instant.now()))
                    || (excavation && abilities.isActive(serverPlayer.getUUID(), "bigbangskills:excavation.giga_drill_breaker", Instant.now()))
                    || (herbalism && abilities.isActive(serverPlayer.getUUID(), "bigbangskills:herbalism.green_terra", Instant.now()));
            var action = new BlockBreakAction(serverPlayer.getUUID(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), world.dimension().location().toString(), mining, wood, true, false, placed, tracker != null && tracker.reliable(), hasSilkTouch(serverPlayer.getMainHandItem()), abilityActive, excavation, herbalism, serverPlayer.getVehicle() != null);
            var result = progress == null ? null : progress.blockBreak(action);
            if (result != null && result.accepted()) notifications.recordXp(serverPlayer.getUUID(), result.skillId(), result.amount(), result.previousLevel(), result.currentLevel(), Instant.now()).forEach(feedback -> sendFeedback(serverPlayer, feedback));
            else if (result != null && "profile_loading_queued".equals(result.reason())) serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("profile.queued", SkillMessages.locale(serverPlayer.clientInformation().language()))));
            if (result != null && result.accepted() && (result.blockEffect().extraDrops() > 0 || result.blockEffect().abilityDurabilityCost() || result.blockEffect().chainBreaks() > 0)) pendingBlockEffects.put(key, result.blockEffect());
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            var key = new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ());
            var effect = pendingBlockEffects.remove(key);
            if (effect != null && effect.abilityDurabilityCost()) serverPlayer.getMainHandItem().hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
            if (effect != null && effect.extraDrops() > 0 && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                var drops = Block.getDrops(state, serverLevel, pos, blockEntity, serverPlayer, serverPlayer.getMainHandItem());
                for (var drop : drops) for (var i = 0; i < effect.extraDrops(); i++) Block.popResource(serverLevel, pos, drop.copy());
            }
            if (effect != null && effect.chainBreaks() > 0 && world instanceof net.minecraft.server.level.ServerLevel serverLevel) chainBreak(serverLevel, pos, state, serverPlayer, effect);
            if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                var active = effect != null && effect.chainBreaks() == 8 && effect.chainSameType();
                addExcavationTreasures(serverPlayer, state, active, drop -> Block.popResource(serverLevel, pos, drop));
                addHerbalismTreasure(serverPlayer, state, drop -> Block.popResource(serverLevel, pos, drop));
                replantHerbalism(serverPlayer, state, pos, serverLevel);
            }
            if (provenance != null) provenance.clear(key);
        });
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, blockEntity) -> {
            pendingBlockEffects.remove(new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ()));
            if (provenance != null) provenance.clear(new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ()));
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> processCombatXp(entity, source, damageTaken));
        ServerLivingEntityEvents.AFTER_DEATH.register(FabricBootstrap::recordArrowRetrieval);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> allowDamage(entity, source, amount));
        ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            abilities.expire(Instant.now());
            tickRuptures();
            tickSummonedPets();
            var tick = server.overworld().getGameTime();
            arrowOrigins.entrySet().removeIf(entry -> entry.getValue().expiresAt() < tick);
            hopperAlchemyTransfers.entrySet().removeIf(entry -> entry.getValue() <= tick);
            notifications.flush(Instant.now()).forEach(feedback -> { var player = server.getPlayerList().getPlayer(feedback.playerId()); if (player != null) sendFeedback(player, feedback); });
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> progress = null);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (progress != null) progress.load(handler.getPlayer().getUUID(), com.bigbangcraft.bigbangskills.api.ProgressionScope.server("default"));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { notifications.clear(handler.getPlayer().getUUID()); abilities.clear(handler.getPlayer().getUUID()); fishing.clear(handler.getPlayer().getUUID()); acrobaticsTeleportCooldowns.remove(handler.getPlayer().getUUID()); if (progress != null) progress.unload(handler.getPlayer().getUUID()); });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        LOGGER.info("BigBangSkills Fabric adapter loaded with gathering skill dispatch");
    }

    private void serverStarted(MinecraftServer server) {
        try {
            StationOwnerPersistence.load(Path.of("config", "bigbangskills", "station-owners.properties"), brewingOwners, smeltingOwners);
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
        StationOwnerPersistence.save(Path.of("config", "bigbangskills", "station-owners.properties"), brewingOwners, smeltingOwners);
        var tracker = provenance;
        if (tracker != null) tracker.shutdown(java.time.Duration.ofSeconds(5)).whenComplete((ignored, failure) -> { if (failure != null) LOGGER.error("BigBangSkills provenance shutdown flush failed", failure); });
        if (progress != null) progress.shutdown().whenComplete((ignored, failure) -> { if (failure != null) LOGGER.error("BigBangSkills shutdown flush failed", failure); });
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var skillsCommand = Commands.literal("skills").executes(context -> sendOverview(context.getSource().getPlayerOrException()));
        skillsCommand.then(Commands.argument("skill", StringArgumentType.word()).executes(context -> sendSkill(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "skill"))));
        skillsCommand.then(Commands.literal("ability").then(Commands.argument("skill", StringArgumentType.word()).then(Commands.argument("ability", StringArgumentType.word()).executes(context -> activateAbility(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "skill"), StringArgumentType.getString(context, "ability"))))));
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
        SkillMessageFormatter.skill(progress.progress(player.getUUID()).orElseThrow(), skills, skill, SkillMessages.locale(player.clientInformation().language()), skillConfig).forEach(line -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line)));
        return 1;
    }

    private int activateAbility(ServerPlayer player, String requestedSkill, String requestedAbility) {
        var skill = parseSkill(requestedSkill);
        var definition = skill == null ? null : skills.get(skill).orElse(null);
        var current = progress == null ? null : progress.progress(player.getUUID()).orElse(null);
        var level = current == null || skill == null || current.get(skill) == null ? 0 : current.get(skill).level();
        var ability = skill == null ? null : DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(candidate -> candidate.type() == AbilityType.ACTIVE && (candidate.id().equals(skill.path() + "." + requestedAbility) || candidate.id().equals(requestedAbility))).findFirst().orElse(null);
        var duration = skill == null || skillConfig.rule(skill).abilityDurationSeconds() == 0
                ? AbilityService.levelDuration(level, (int) formulas.value("abilities.duration_cap_level"), (int) formulas.value("abilities.duration_increase_level"))
                : Duration.ofSeconds(skillConfig.rule(skill).abilityDurationSeconds());
        if (definition == null || ability == null || current == null || !skillConfig.rule(skill).enabled() || !skillConfig.rule(skill).abilitiesEnabled() || !abilities.activate(player.getUUID(), ability, level, Instant.now(), skillConfig.abilityCooldown(ability), duration)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("ability.unavailable", SkillMessages.locale(player.clientInformation().language())))); return 0;
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(SkillMessages.text("ability.activated", SkillMessages.locale(player.clientInformation().language()), SkillMessages.text("bigbangskills.ability." + ability.id(), SkillMessages.locale(player.clientInformation().language())))));
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

    private boolean trackedSkillBlock(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return state.is(MINING) || state.is(BlockTags.LOGS) || state.is(WOODCUTTING) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
                || gameplay.hasBlockXp(SkillId.parse("bigbangskills:mining"), id)
                || gameplay.hasBlockXp(SkillId.parse("bigbangskills:woodcutting"), id)
                || gameplay.hasBlockXp(SkillId.parse("bigbangskills:excavation"), id)
                || gameplay.hasBlockXp(SkillId.parse("bigbangskills:herbalism"), id);
    }

    private boolean activateBlockAbility(ServerPlayer player, BlockState state) {
        if (skillConfig.abilityOnlyWhenSneaking() && !player.isCrouching()) return false;
        var item = player.getMainHandItem().getItem();
        if (state.is(net.minecraft.world.level.block.Blocks.TNT)) return false;
        String skill;
        String ability;
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if ((state.is(BlockTags.LOGS) || state.is(WOODCUTTING) || gameplay.hasBlockXp(SkillId.parse("bigbangskills:woodcutting"), id)) && item instanceof AxeItem) {
            skill = "woodcutting"; ability = "tree_feller";
        } else if (item instanceof PickaxeItem) {
            skill = "mining"; ability = "super_breaker";
        } else if (item instanceof AxeItem) {
            skill = "axes"; ability = "skull_splitter";
        } else if (item instanceof net.minecraft.world.item.ShovelItem) {
            skill = "excavation"; ability = "giga_drill_breaker";
        } else if (item instanceof net.minecraft.world.item.HoeItem) {
            skill = "herbalism"; ability = "green_terra";
        } else if (item instanceof SwordItem) {
            skill = "swords"; ability = "serrated_strikes";
        } else {
            return false;
        }
        return activateAbility(player, skill, ability) > 0;
    }

    private boolean activateItemAbility(ServerPlayer player) {
        if (skillConfig.abilityOnlyWhenSneaking() && !player.isCrouching()) return false;
        var item = player.getMainHandItem().getItem();
        if (player.getMainHandItem().isEmpty()) return activateAbility(player, "unarmed", "berserk") > 0;
        if (item instanceof net.minecraft.world.item.PickaxeItem) return activateAbility(player, "mining", "super_breaker") > 0;
        if (item instanceof net.minecraft.world.item.AxeItem) return activateAbility(player, "axes", "skull_splitter") > 0;
        if (item instanceof net.minecraft.world.item.ShovelItem) return activateAbility(player, "excavation", "giga_drill_breaker") > 0;
        if (item instanceof net.minecraft.world.item.HoeItem) return activateAbility(player, "herbalism", "green_terra") > 0;
        if (item instanceof net.minecraft.world.item.SwordItem) return activateAbility(player, "swords", "serrated_strikes") > 0;
        return false;
    }

    private boolean remoteBlastMining(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        var detonator = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals(formulas.miningDetonatorItem());
        if (!player.isCrouching() || (!(player.getMainHandItem().getItem() instanceof PickaxeItem) && !detonator)
                || !world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TNT)
                || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > formulas.value("mining.blast_remote_detonation_distance") * formulas.value("mining.blast_remote_detonation_distance")) return false;
        if (activateAbility(player, "mining", "blast_mining") == 0) return true;
        var profile = progress == null ? null : progress.progress(player.getUUID()).orElse(null);
        var skill = SkillId.parse("bigbangskills:mining");
        var state = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(value -> value.id().equals("mining.blast_mining")).findFirst().orElse(null);
        if (state == null || ability == null) return true;
        var biggerBombs = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .anyMatch(value -> value.id().equals("mining.bigger_bombs") && state.level() >= value.unlockLevel());
        var radius = blastMining.radius(ability.rankForLevel(state.level()), biggerBombs);
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            var tnt = new net.minecraft.world.entity.item.PrimedTnt(serverLevel, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, player);
            tnt.setFuse(0);
            serverLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            serverLevel.addFreshEntity(tnt);
        }
        return true;
    }

    private static ServerPlayer blastMiningOwner(net.minecraft.world.damagesource.DamageSource source) {
        var owner = blastMiningOwner(source.getEntity());
        return owner != null ? owner : blastMiningOwner(source.getDirectEntity());
    }

    private static ServerPlayer blastMiningOwner(net.minecraft.world.entity.Entity source) {
        if (source instanceof ServerPlayer player) return player;
        if (source instanceof net.minecraft.world.entity.item.PrimedTnt tnt && tnt.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    private static boolean hasSilkTouch(ItemStack stack) {
        var enchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        return enchantments.keySet().stream().anyMatch(enchantment -> enchantment.unwrapKey().map(key -> key.location().getPath().equals("silk_touch")).orElse(false));
    }

    private static boolean hasFeatherFalling(ItemStack stack) {
        var enchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        return enchantments.keySet().stream().anyMatch(enchantment -> enchantment.unwrapKey().map(key -> key.location().getPath().equals("feather_falling")).orElse(false));
    }

    private static boolean herbalismMature(BlockState state) {
        var property = state.getBlock().getStateDefinition().getProperty("age");
        if (!(property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age)) return true;
        var current = state.getValue(age);
        var max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return id.equals("minecraft:sweet_berry_bush") ? current >= 2 : current == max && current != 0;
    }

    private void processCombatXp(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        var attacker = attacker(source);
        if (attacker == null || progress == null || attacker == target) return;
        var skill = combatSkill(source, attacker);
        var profile = skill == null ? null : progress.progress(attacker.getUUID()).orElse(null);
        if (skill == null || profile == null) return;
        var pvp = target instanceof ServerPlayer;
        var targetId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        var baseXp = combatXp(source, target, targetId, skill, pvp);
        var pending = PENDING_COMBAT.get().isEmpty() ? null : PENDING_COMBAT.get().peekLast();
        var resolution = pending != null && pending.attacker() == attacker && pending.target() == target
                ? PENDING_COMBAT.get().pollLast()
                : null;
        if (resolution == null) {
            var quality = armorQuality(target);
            var action = new CombatAction(attacker.getUUID(), skill, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).toString(), baseXp, damage, attacker.getAttackStrengthScale(0.5F), pvp, quality > 0, quality, abilityActive(attacker, skill), ProgressionScope.server("default"));
            resolution = new PendingCombat(attacker, target, combat.resolve(profile, action));
        } else {
            resolution = new PendingCombat(attacker, target, resolution.resolution().withAwardAmount(combat.xpForDamage(baseXp, damage)));
        }
        var result = progress.award(resolution.resolution().award());
        if (result.accepted()) {
            applyCombatEffect(attacker, target, resolution.resolution().effect(), source, damage, skillConfig.rule(skill).pvp());
            notifications.recordXp(attacker.getUUID(), result.skillId(), result.amount(), result.previousLevel(), result.currentLevel(), Instant.now()).forEach(feedback -> sendFeedback(attacker, feedback));
        }
    }

    private record PendingCombat(ServerPlayer attacker, LivingEntity target, com.bigbangcraft.bigbangskills.common.skill.CombatResolution resolution) {}

    private record ArrowOrigin(net.minecraft.world.phys.Vec3 position, long expiresAt, double force) {}

    private BigDecimal combatXp(net.minecraft.world.damagesource.DamageSource source, LivingEntity target, String targetId, SkillId skill, boolean pvp) {
        if (target.getTags().contains("bigbangskills_cotw")) return BigDecimal.ZERO;
        var xp = gameplay.combatXp(targetId, pvp);
        var tamed = target instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame()
                || target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed();
        xp = combat.tamedCombatXp(xp, tamed);
        xp = combat.spawnedCombatXp(xp, target.getTags().contains("bigbangskills_spawner_mob"),
                target.getTags().contains("bigbangskills_egg_mob"), target.getTags().contains("bigbangskills_nether_portal_mob"),
                target.getTags().contains("bigbangskills_bred_mob"));
        if (skill.path().equals("archery") && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            var origin = arrowOrigins.get(arrow.getUUID());
            if (origin != null) xp = xp.multiply(BigDecimal.valueOf(combat.archeryDistanceXpMultiplier(origin.position().distanceTo(target.position()))
                    * combat.archeryForceXpMultiplier(origin.force())));
        }
        return xp;
    }

    private static void applyCombatEffect(ServerPlayer attacker, LivingEntity target, com.bigbangcraft.bigbangskills.common.skill.CombatEffect effect, net.minecraft.world.damagesource.DamageSource source, float damage, boolean skillPvp) {
        if (effect.aoeDamage() > 0 && !COMBAT_AREA.get()) {
            COMBAT_AREA.set(true);
            try {
                target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5), entity -> entity != target && entity != attacker && entity.isAlive()
                                && CombatSkillEngine.secondaryTargetAllowed(entity instanceof ServerPlayer,
                                skillPvp && attacker.level().getServer() != null && attacker.level().getServer().isPvpAllowed(),
                                entity instanceof ServerPlayer player && player.isSpectator(), ownedBy(attacker, entity)))
                        .stream().limit(areaTargetLimit(attacker)).forEach(entity -> entity.hurt(target.damageSources().playerAttack(attacker), (float) effect.aoeDamage()));
            } finally {
                COMBAT_AREA.set(false);
            }
        }
        if (effect.daze()) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, effect.statusDurationTicks(), effect.statusAmplifier()));
            if (target instanceof ServerPlayer victim) victim.setXRot(java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 180.0F - 90.0F);
        }
        if (effect.cripple() && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effect.statusDurationTicks(), effect.statusAmplifier()));
        if (effect.momentum()) attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effect.statusDurationTicks(), effect.statusAmplifier()));
        var instance = INSTANCE;
        if (effect.greaterImpact()) {
            var strength = instance == null ? 1.5 : instance.formulas.value("combat.axes.greater_impact_knockback");
            target.knockback(strength, target.getX() - attacker.getX(), target.getZ() - attacker.getZ());
        }
        if (effect.rupture() && effect.ruptureTickDamage() > 0 && instance != null && !(target instanceof ServerPlayer victim && victim.isBlocking())) {
            instance.ruptures.compute(target.getUUID(), (id, current) -> {
                if (current == null) return new RuptureState(target, effect.ruptureTickDamage(), effect.ruptureDurationTicks());
                current.ticksRemaining = effect.ruptureDurationTicks();
                current.damage = effect.ruptureTickDamage();
                return current;
            });
        }
        if (effect.armorDurabilityDamage() > 0) {
            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armor = target.getItemBySlot(slot);
                if (!armor.isEmpty()) {
                    var cap = (int) Math.ceil(armor.getMaxDamage() * (instance == null ? 20.0 : instance.formulas.value("combat.axes.armor_impact_max_percent")) / 100.0);
                    armor.hurtAndBreak(Math.max(1, Math.min(effect.armorDurabilityDamage(), cap)), target, slot);
                }
            }
        }
        var runtime = INSTANCE;
        var grip = target instanceof ServerPlayer victim && runtime != null && runtime.progress != null
                && runtime.skillConfig.rule(SkillId.parse("bigbangskills:unarmed")).enabled()
                && runtime.progress.progress(victim.getUUID()).map(runtime.combat::ironGrip).orElse(false);
        if (effect.disarm() && !grip && target instanceof ServerPlayer victim && !victim.getMainHandItem().isEmpty()) {
            var drop = victim.drop(victim.getMainHandItem(), false);
            if (drop != null && runtime != null && runtime.formulas.value("combat.unarmed.disarm_anti_theft") > 0) {
                drop.addTag("bigbangskills_disarm_owner_" + victim.getUUID());
            }
            victim.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (ownerOf(source.getEntity()) == attacker && source.getEntity() instanceof LivingEntity pet) {
            if (effect.fastFood() && pet.getHealth() < pet.getMaxHealth())
                pet.setHealth(Math.min(pet.getMaxHealth(), pet.getHealth() + damage));
            if (effect.pummel()) target.knockback(1.5, target.getX() - pet.getX(), target.getZ() - pet.getZ());
        }
        if (target instanceof ServerPlayer victim && source.getEntity() instanceof LivingEntity damager
                && source.getDirectEntity() == damager && runtime != null && runtime.progress != null) {
            var skill = SkillId.parse("bigbangskills:swords");
            if (runtime.skillConfig.rule(skill).enabled()) {
                var profile = runtime.progress.progress(victim.getUUID()).orElse(null);
                var reflected = profile == null ? 0 : runtime.combat.counterAttackDamage(profile, damage);
                if (reflected > 0) damager.hurt(victim.damageSources().generic(), (float) reflected);
            }
        }
    }

    private static boolean ownedBy(ServerPlayer player, LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.TamableAnimal tame && tame.getOwner() == player
                || entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()
                && player.getUUID().equals(horse.getOwnerUUID());
    }

    private static long areaTargetLimit(ServerPlayer attacker) {
        var path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).getPath();
        return path.contains("netherite") ? 4 : path.contains("diamond") ? 3 : path.contains("iron") ? 2 : 1;
    }

    private void tickRuptures() {
        ruptures.entrySet().removeIf(entry -> {
            var rupture = entry.getValue();
            if (!rupture.target.isAlive() || rupture.ticksRemaining-- <= 0) return true;
            if (rupture.ticksRemaining % 10 == 0) rupture.target.hurt(rupture.target.damageSources().generic(), (float) rupture.damage);
            return false;
        });
    }

    private static final class RuptureState {
        private final LivingEntity target;
        private double damage;
        private int ticksRemaining;
        private RuptureState(LivingEntity target, double damage, int ticksRemaining) {
            this.target = target; this.damage = damage; this.ticksRemaining = ticksRemaining;
        }
    }

    private void tickSummonedPets() {
        summonedPets.entrySet().removeIf(entry -> {
            var pet = entry.getValue();
            if (!pet.entity.isAlive() || pet.ticksRemaining-- <= 0) {
                if (pet.entity.isAlive()) pet.entity.discard();
                return true;
            }
            return false;
        });
    }

    private static final class SummonedPet {
        private final net.minecraft.world.entity.Entity entity;
        private int ticksRemaining;
        private SummonedPet(net.minecraft.world.entity.Entity entity, int ticksRemaining) {
            this.entity = entity; this.ticksRemaining = ticksRemaining;
        }
    }

    private void chainBreak(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos origin, BlockState original, ServerPlayer player, BlockBreakEffect effect) {
        if (provenance == null || !provenance.reliable()) return;
        var treeFeller = effect.chainSameType();
        if (treeFeller && formulas.value("woodcutting.tree_feller_sounds") > 0) level.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        var woodcutting = SkillId.parse("bigbangskills:woodcutting");
        var woodcuttingLevel = progress == null ? 0 : progress.progress(player.getUUID()).map(value -> value.get(woodcutting) == null ? 0 : value.get(woodcutting).level()).orElse(0);
        var knockOnWood = DefaultAbilityCatalog.all().getOrDefault(woodcutting, java.util.List.of()).stream()
                .filter(value -> value.id().equals("woodcutting.knock_on_wood")).findFirst().orElse(null);
        var knockRank = knockOnWood == null ? 0 : knockOnWood.rankForLevel(woodcuttingLevel);
        var woodEngine = new com.bigbangcraft.bigbangskills.common.skill.WoodcuttingEngine();
        var extraXp = BigDecimal.ZERO;
        var processedLogs = 0;
        var broken = 0;
        var queue = new java.util.ArrayDeque<net.minecraft.core.BlockPos>();
        var seen = new java.util.HashSet<net.minecraft.core.BlockPos>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && broken < effect.chainBreaks()) {
            var current = queue.removeFirst();
            for (var direction : net.minecraft.core.Direction.values()) {
                var next = current.relative(direction);
                if (!seen.add(next) || next.equals(origin) || !level.hasChunkAt(next)) continue;
                var state = level.getBlockState(next);
                var nextId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                var woodLog = state.is(BlockTags.LOGS) || state.is(WOODCUTTING) || gameplay.hasBlockXp(woodcutting, nextId);
                if (effect.chainSameType() && state.getBlock() != original.getBlock()
                        && !(effect.includeLeaves() && state.is(BlockTags.LEAVES))
                        && !(treeFeller && treeComponent(state, original, effect.includeLeaves()))) continue;
                if (state.isAir()) continue;
                if (provenance.wasPlaced(new BlockKey(worldId(level), next.getX(), next.getY(), next.getZ()))) continue;
                var treePart = treeFeller && !woodLog && treeComponent(state, original, effect.includeLeaves());
                if (level.destroyBlock(next, !treePart, player)) {
                    broken++;
                    queue.addLast(next);
                    if (treeFeller && woodLog) {
                        var rawXp = gameplay.xpForBlock(woodcutting, nextId);
                        if (rawXp.signum() > 0) {
                            extraXp = extraXp.add(BigDecimal.valueOf(woodEngine.treeFellerXp(rawXp.intValue(), processedLogs++, formulas.value("woodcutting.tree_feller_reduced_xp") > 0)));
                        }
                    }
                    if (treePart) {
                        var drops = Block.getDrops(state, level, next, level.getBlockEntity(next), player, player.getMainHandItem());
                        if (woodEngine.normalTreePartDrops(java.util.concurrent.ThreadLocalRandom.current()::nextDouble)) {
                            drops.forEach(drop -> Block.popResource(level, next, drop));
                        } else if (knockRank > 0) {
                            drops.stream().filter(drop -> {
                                var path = BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath();
                                return path.endsWith("sapling") || path.endsWith("propagule");
                            }).forEach(drop -> Block.popResource(level, next, drop));
                        }
                    }
                    if (treeFeller && knockOnWood != null && woodEngine.knockOnWoodXpOrb(knockRank,
                            formulas.value("woodcutting.knock_on_wood_xp_orb_enabled") > 0,
                            java.util.concurrent.ThreadLocalRandom.current()::nextDouble)) {
                        net.minecraft.world.entity.ExperienceOrb.award(level,
                                net.minecraft.world.phys.Vec3.atCenterOf(next), java.util.concurrent.ThreadLocalRandom.current().nextInt(100) + 1);
                    }
                }
            }
        }
        if (extraXp.signum() > 0) awardActivity(player, SkillId.parse("bigbangskills:woodcutting"), extraXp, com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, false, true, "tree_feller");
    }

    private static boolean treeComponent(BlockState state, BlockState original, boolean includeLeaves) {
        if (includeLeaves && state.is(BlockTags.LEAVES)) return true;
        var current = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        var root = BuiltInRegistries.BLOCK.getKey(original.getBlock()).getPath();
        if (root.startsWith("stripped_")) root = root.substring("stripped_".length());
        if (current.startsWith("stripped_")) current = current.substring("stripped_".length());
        if (root.startsWith("crimson_") || root.startsWith("warped_")) {
            var family = root.startsWith("crimson_") ? "crimson_" : "warped_";
            return current.startsWith(family) && (current.endsWith("_wart_block") || current.equals("shroomlight"));
        }
        return false;
    }

    private boolean allowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && progress != null) {
            var profile = progress.progress(owner.getUUID()).orElse(null);
            var tamingSkill = SkillId.parse("bigbangskills:taming");
            if (profile != null && skillConfig.rule(tamingSkill).enabled()
                    && taming.incomingDamage(profile, amount, false, false, true, false, pet.getHealth()) == 0) {
                pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return false;
            }
        }
        return true;
    }

    private void awardActivity(ServerPlayer player, SkillId skill, String action, com.bigbangcraft.bigbangskills.api.XpSource source) {
        if (progress == null) return;
        var amount = gameplay.xpForAction(skill, action);
        awardActivity(player, skill, amount, source, false, true, action);
    }

    private void awardActivity(ServerPlayer player, SkillId skill, BigDecimal amount, com.bigbangcraft.bigbangskills.api.XpSource source, boolean pvp, boolean pve, String reason) {
        if (progress == null || amount.signum() <= 0) return;
        var result = progress.award(new SkillAwardAction(player.getUUID(), skill, amount,
                source, reason, ProgressionScope.server("default"), true, false, pvp, pve));
        if (result.accepted()) notifications.recordXp(player.getUUID(), result.skillId(), result.amount(), result.previousLevel(), result.currentLevel(), Instant.now()).forEach(feedback -> sendFeedback(player, feedback));
    }

    private void addExcavationTreasures(ServerPlayer player, BlockState state, boolean abilityActive, java.util.function.Consumer<ItemStack> drops) {
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL) || progress == null) return;
        var skill = SkillId.parse("bigbangskills:excavation");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream()
                .filter(definition -> definition.id().equals("excavation.archaeology")).findFirst().orElse(null);
        var stateProgress = profile == null ? null : profile.get(skill);
        if (stateProgress == null || ability == null || !skillConfig.rule(skill).enabled()) return;
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (var reward : excavationTreasures.roll(blockId, stateProgress.level(), stateProgress.level() >= ability.unlockLevel(), abilityActive, java.util.concurrent.ThreadLocalRandom.current()::nextDouble)) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.itemId()));
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            drops.accept(new ItemStack(item, reward.amount()));
            awardActivity(player, skill, reward.xp(), com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, false, true, "archaeology." + reward.itemId());
        }
    }

    private void addHerbalismTreasure(ServerPlayer player, BlockState state, java.util.function.Consumer<ItemStack> drops) {
        if (progress == null || (formulas.value("herbalism.prevent_afk_leveling") > 0 && player.getVehicle() != null)) return;
        var skill = SkillId.parse("bigbangskills:herbalism");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("herbalism.hylian_luck")).findFirst().orElse(null);
        var stateProgress = profile == null ? null : profile.get(skill);
        if (stateProgress == null || ability == null || !skillConfig.rule(skill).enabled() || stateProgress.level() < ability.unlockLevel()) return;
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        herbalism.hylianLuck(blockId, stateProgress.level(), formulas.value("herbalism.hylian_luck_max_percent"), (int) formulas.value("herbalism.hylian_luck_max_level"), java.util.concurrent.ThreadLocalRandom.current()::nextDouble).ifPresent(reward -> {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.itemId()));
            if (item == null || item == net.minecraft.world.item.Items.AIR) return;
            drops.accept(new ItemStack(item, reward.amount()));
            awardActivity(player, skill, BigDecimal.valueOf(reward.xp()), com.bigbangcraft.bigbangskills.api.XpSource.BLOCK_BREAK, false, true, "hylian_luck." + reward.itemId());
        });
    }

    private void replantHerbalism(ServerPlayer player, BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.server.level.ServerLevel world) {
        if (progress == null || (formulas.value("herbalism.prevent_afk_leveling") > 0 && player.getVehicle() != null) || !herbalismMature(state)) return;
        var crop = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        var replantKey = switch (crop) {
            case "wheat", "carrots", "potatoes", "beetroots", "nether_wart", "cocoa", "sweet_berry_bush" -> "herbalism.replant_" + crop;
            default -> null;
        };
        if (replantKey == null || formulas.value(replantKey) <= 0) return;
        var skill = SkillId.parse("bigbangskills:herbalism");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var current = profile == null ? null : profile.get(skill);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("herbalism.green_thumb")).findFirst().orElse(null);
        var property = state.getBlock().getStateDefinition().getProperty("age");
        if (current == null || ability == null || property == null || !(property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age) || current.level() < ability.unlockLevel() || java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= herbalism.greenThumbChance(current.level(), formulas.value("herbalism.green_thumb_max_percent"), (int) formulas.value("herbalism.green_thumb_max_level")) / 100.0) return;
        var seed = switch (crop) {
            case "wheat" -> net.minecraft.world.item.Items.WHEAT_SEEDS;
            case "carrots" -> net.minecraft.world.item.Items.CARROT;
            case "potatoes" -> net.minecraft.world.item.Items.POTATO;
            case "beetroots" -> net.minecraft.world.item.Items.BEETROOT_SEEDS;
            case "nether_wart" -> net.minecraft.world.item.Items.NETHER_WART;
            case "cocoa" -> net.minecraft.world.item.Items.COCOA_BEANS;
            case "sweet_berry_bush" -> net.minecraft.world.item.Items.SWEET_BERRIES;
            default -> null;
        };
        if (seed == null || player.getInventory().countItem(seed) == 0) return;
        player.getInventory().removeItem(new ItemStack(seed, 1));
        world.setBlock(pos, state.setValue(age, 0), 3);
        markPlaced(world, pos);
    }

    private void applyFishingEnchantment(ItemStack stack, ServerPlayer player, FishingTreasureEngine.MagicEnchantment enchantment) {
        var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, ResourceLocation.parse(enchantment.enchantmentId()));
        var holder = registry.get(key).orElse(null);
        if (holder == null) return;
        if (!stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) && !holder.value().canEnchant(stack)) return;
        var component = stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) ? net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS : net.minecraft.core.component.DataComponents.ENCHANTMENTS;
        var current = stack.getOrDefault(component, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        if (!skillConfig.fishingAllowConflictingEnchants() && current.keySet().stream().anyMatch(existing -> !net.minecraft.world.item.enchantment.Enchantment.areCompatible(holder, existing))) return;
        var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(current);
        mutable.set(holder, enchantment.level());
        stack.set(component, mutable.toImmutable());
    }

    private record PreparedFishingReward(ItemStack stack, int xp, String itemId) {}

    private static int fishingLuckOfTheSea(ServerPlayer player) {
        var enchantments = player.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var enchantment : enchantments.keySet()) if (enchantment.unwrapKey().map(key -> key.location().getPath()).orElse("").equals("luck_of_the_sea")) return enchantments.getLevel(enchantment);
        return 0;
    }

    private boolean herbalismInteraction(ServerPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        if (progress == null) return false;
        var skill = SkillId.parse("bigbangskills:herbalism");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var state = world.getBlockState(pos);
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        var level = profile == null || profile.get(skill) == null ? 0 : profile.get(skill).level();
        if (!skillConfig.rule(skill).enabled()) return false;
        var item = player.getItemInHand(hand);
        java.util.function.DoubleSupplier random = java.util.concurrent.ThreadLocalRandom.current()::nextDouble;
        var green = herbalism.greenTerraConversion(blockId);
        var shroom = herbalism.shroomThumbConversion(blockId);
        var greenAbility = abilities.isActive(player.getUUID(), "bigbangskills:herbalism.green_terra", Instant.now());
        var greenThumb = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("herbalism.green_thumb")).findFirst().orElse(null);
        var shroomAbility = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("herbalism.shroom_thumb")).findFirst().orElse(null);
        if (greenAbility && green.isPresent() && item.is(net.minecraft.world.item.Items.WHEAT_SEEDS) && random.getAsDouble() < herbalism.greenThumbChance(level, formulas.value("herbalism.green_thumb_max_percent"), (int) formulas.value("herbalism.green_thumb_max_level")) / 100.0) {
            item.shrink(1);
            world.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(green.get())).defaultBlockState(), 3);
            return true;
        }
        if (greenThumb != null && level >= greenThumb.unlockLevel() && green.isPresent() && item.is(net.minecraft.world.item.Items.WHEAT_SEEDS) && random.getAsDouble() < herbalism.greenThumbChance(level, formulas.value("herbalism.green_thumb_max_percent"), (int) formulas.value("herbalism.green_thumb_max_level")) / 100.0) {
            item.shrink(1);
            world.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(green.get())).defaultBlockState(), 3);
            return true;
        }
        if (shroomAbility != null && level >= shroomAbility.unlockLevel() && shroom.isPresent() && (item.is(net.minecraft.world.item.Items.BROWN_MUSHROOM) || item.is(net.minecraft.world.item.Items.RED_MUSHROOM)) && player.getInventory().countItem(net.minecraft.world.item.Items.BROWN_MUSHROOM) > 0 && player.getInventory().countItem(net.minecraft.world.item.Items.RED_MUSHROOM) > 0 && random.getAsDouble() < herbalism.shroomThumbChance(level, formulas.value("herbalism.shroom_thumb_max_percent"), (int) formulas.value("herbalism.shroom_thumb_max_level")) / 100.0) {
            player.getInventory().removeItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BROWN_MUSHROOM, 1));
            player.getInventory().removeItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_MUSHROOM, 1));
            world.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(shroom.get())).defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private boolean callOfWild(ServerPlayer player, net.minecraft.world.level.Level world) {
        if (!player.isCrouching() || progress == null) return false;
        var held = player.getMainHandItem();
        var recipe = tamingSummons.snapshot().entrySet().stream().filter(entry -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getValue().itemId())) != null
                && held.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getValue().itemId())))).map(Map.Entry::getValue).findFirst().orElse(null);
        var entityId = tamingSummons.snapshot().entrySet().stream().filter(entry -> entry.getValue() == recipe).map(Map.Entry::getKey).findFirst().orElse(null);
        if (recipe == null || entityId == null || held.getCount() < recipe.itemCount()) return false;
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
        if (type == null) return false;
        var skill = SkillId.parse("bigbangskills:taming");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("taming.call_of_the_wild")).findFirst().orElse(null);
        if (profile == null || ability == null || profile.get(skill) == null || profile.get(skill).level() < ability.unlockLevel() || !skillConfig.rule(skill).enabled()) return false;
        var owned = world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, player.getBoundingBox().inflate(64), entity -> entity.getType() == type &&
                ((entity instanceof net.minecraft.world.entity.TamableAnimal tame && tame.getOwner() == player) ||
                        (entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed() && player.getUUID().equals(horse.getOwnerUUID())))).size();
        if (owned >= recipe.ownerLimit()) return true;
        var pet = type.create(world);
        if (pet == null) return true;
        if (!(pet instanceof net.minecraft.world.entity.TamableAnimal) && !(pet instanceof net.minecraft.world.entity.animal.horse.AbstractHorse)) {
            pet.discard();
            return true;
        }
        pet.addTag("bigbangskills_cotw");
        if (formulas.value("taming.cotw_breeding_prevented") > 0) pet.addTag("bigbangskills_cotw_no_breed");
        player.getInventory().removeItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(recipe.itemId())), recipe.itemCount()));
        pet.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        if (pet instanceof net.minecraft.world.entity.TamableAnimal tame) tame.tame(player);
        if (pet instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
            horse.tameWithName(player);
            var jump = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
            if (jump != null) jump.setBaseValue(com.bigbangcraft.bigbangskills.common.skill.TamingEngine.horseJumpStrength(
                    java.util.concurrent.ThreadLocalRandom.current().nextDouble(), formulas.value("taming.call_of_wild_min_horse_jump_strength"),
                    formulas.value("taming.call_of_wild_max_horse_jump_strength")));
        }
        world.addFreshEntity(pet);
        summonedPets.put(pet.getUUID(), new SummonedPet(pet, recipe.lifespanTicks()));
        awardActivity(player, skill, "call_of_the_wild", com.bigbangcraft.bigbangskills.api.XpSource.TAMING);
        return true;
    }

    private boolean blockCracker(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level world) {
        if (formulas.value("combat.unarmed.block_cracker_enabled") <= 0 || !player.getMainHandItem().isEmpty() || progress == null) return false;
        var skill = SkillId.parse("bigbangskills:unarmed");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("unarmed.block_cracker")).findFirst().orElse(null);
        if (profile == null || ability == null || profile.get(skill) == null || profile.get(skill).level() < ability.unlockLevel() || !skillConfig.rule(skill).enabled()) return false;
        var block = world.getBlockState(pos).getBlock();
        var replacement = switch (BuiltInRegistries.BLOCK.getKey(block).getPath()) {
            case "stone_bricks" -> "cracked_stone_bricks";
            case "infested_stone_bricks" -> "infested_cracked_stone_bricks";
            case "deepslate_bricks" -> "cracked_deepslate_bricks";
            case "deepslate_tiles" -> "cracked_deepslate_tiles";
            case "polished_blackstone_bricks" -> "cracked_polished_blackstone_bricks";
            case "nether_bricks" -> "cracked_nether_bricks";
            default -> null;
        };
        if (replacement == null) return false;
        var cracked = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", replacement));
        if (cracked == null) return false;
        world.setBlock(pos, cracked.defaultBlockState(), 3);
        awardActivity(player, skill, "block_cracker", com.bigbangcraft.bigbangskills.api.XpSource.INTEGRATION);
        return true;
    }

    private boolean beastLore(ServerPlayer player, Entity entity) {
        if (!player.isCrouching() || (!(entity instanceof net.minecraft.world.entity.TamableAnimal)
                && !(entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse)) || progress == null) return false;
        var skill = SkillId.parse("bigbangskills:taming");
        var profile = progress.progress(player.getUUID()).orElse(null);
        var ability = DefaultAbilityCatalog.all().getOrDefault(skill, java.util.List.of()).stream().filter(value -> value.id().equals("taming.beast_lore")).findFirst().orElse(null);
        if (profile == null || ability == null || profile.get(skill) == null || profile.get(skill).level() < ability.unlockLevel()) return false;
        var animal = (net.minecraft.world.entity.LivingEntity) entity;
        var owner = entity instanceof net.minecraft.world.entity.TamableAnimal tame ? tame.getOwnerUUID()
                : ((net.minecraft.world.entity.animal.horse.AbstractHorse) entity).getOwnerUUID();
        var message = "Beast Lore: health " + animal.getHealth() + "/" + animal.getMaxHealth() + (owner == null ? "" : " owner=" + owner);
        if (entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse
                && !(horse instanceof net.minecraft.world.entity.animal.horse.Llama)) {
            var speed = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            var jump = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
            if (speed != null) message += " speed=" + speed.getValue() * 43;
            if (jump != null) {
                var value = jump.getValue();
                message += " jump=" + (-0.1817584952 * Math.pow(value, 3) + 3.689713992 * Math.pow(value, 2) + 2.128599134 * value - 0.343930367);
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        return true;
    }

    private boolean abilityActive(ServerPlayer player, SkillId skill) {
        return switch (skill.path()) {
            case "axes" -> abilities.isActive(player.getUUID(), "bigbangskills:axes.skull_splitter", Instant.now());
            case "swords" -> abilities.isActive(player.getUUID(), "bigbangskills:swords.serrated_strikes", Instant.now());
            case "unarmed" -> abilities.isActive(player.getUUID(), "bigbangskills:unarmed.berserk", Instant.now());
            default -> false;
        };
    }

    private SkillId combatSkill(ItemStack stack) {
        if (stack.isEmpty()) return SkillId.parse("bigbangskills:unarmed");
        if (stack.getItem() instanceof BowItem) return SkillId.parse("bigbangskills:archery");
        if (stack.getItem() instanceof CrossbowItem) return SkillId.parse("bigbangskills:crossbows");
        if (stack.getItem() instanceof TridentItem) return SkillId.parse("bigbangskills:tridents");
        if (stack.getItem() instanceof MaceItem) return SkillId.parse("bigbangskills:maces");
        if (stack.getItem() instanceof AxeItem) return SkillId.parse("bigbangskills:axes");
        if (stack.getItem() instanceof SwordItem) return SkillId.parse("bigbangskills:swords");
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var configured = id.contains("spear") ? SkillId.parse("bigbangskills:spears") : combatWeapons.skillFor(id);
        return configured != null ? configured : formulas.value("combat.unarmed.items_as_unarmed") > 0 ? SkillId.parse("bigbangskills:unarmed") : null;
    }

    private SkillId combatSkill(net.minecraft.world.damagesource.DamageSource source, ServerPlayer owner) {
        if (ownerOf(source.getEntity()) == owner) return SkillId.parse("bigbangskills:taming");
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident) return SkillId.parse("bigbangskills:tridents");
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            return arrow.getWeaponItem().getItem() instanceof CrossbowItem
                    ? SkillId.parse("bigbangskills:crossbows")
                    : SkillId.parse("bigbangskills:archery");
        }
        return combatSkill(owner.getMainHandItem());
    }

    private static ServerPlayer attacker(net.minecraft.world.damagesource.DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) return player;
        var entityOwner = ownerOf(entity);
        if (entityOwner != null) return entityOwner;
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) return player;
        if (source.getDirectEntity() instanceof Projectile projectile) return ownerOf(projectile.getOwner());
        return null;
    }

    private static ServerPlayer ownerOf(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet && pet.getOwner() instanceof ServerPlayer player) return player;
        if (entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()
                && horse.getOwnerUUID() != null && horse.level().getServer() != null)
            return horse.level().getServer().getPlayerList().getPlayer(horse.getOwnerUUID());
        return null;
    }
    public static void transferPistonProvenance(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos piston,
                                                net.minecraft.core.Direction direction, boolean extending) {
        var instance = INSTANCE;
        var tracker = instance == null ? null : instance.provenance;
        if (tracker == null || !tracker.reliable()) return;
        var resolver = new net.minecraft.world.level.block.piston.PistonStructureResolver(level, piston, direction, extending);
        if (!resolver.resolve()) return;
        var tracked = new java.util.ArrayList<net.minecraft.core.BlockPos>();
        for (var source : resolver.getToPush()) {
            if (tracker.wasPlaced(new BlockKey(worldId(level), source.getX(), source.getY(), source.getZ()))) tracked.add(source);
        }
        for (var source : tracked) tracker.clear(new BlockKey(worldId(level), source.getX(), source.getY(), source.getZ()));
        for (var source : tracked) {
            var target = source.relative(resolver.getPushDirection());
            tracker.markPlaced(new BlockKey(worldId(level), target.getX(), target.getY(), target.getZ()));
        }
    }

    public static void transferFluidProvenance(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var instance = INSTANCE;
        var tracker = instance == null ? null : instance.provenance;
        if (tracker == null || !tracker.reliable() || !(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return;
        var current = new BlockKey(worldId(level), pos.getX(), pos.getY(), pos.getZ());
        if (tracker.wasPlaced(current)) return;
        for (var direction : net.minecraft.core.Direction.values()) {
            var source = pos.relative(direction);
            if (level.getBlockState(source).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
                    && tracker.wasPlaced(new BlockKey(worldId(level), source.getX(), source.getY(), source.getZ()))) {
                tracker.markPlaced(current);
                return;
            }
        }
    }

    public static void clearFluidProvenance(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var instance = INSTANCE;
        var tracker = instance == null ? null : instance.provenance;
        if (tracker != null) tracker.clear(new BlockKey(worldId(level), pos.getX(), pos.getY(), pos.getZ()));
    }

    private void markFluidPlacement(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) markPlaced(level, pos);
    }

    private void markPlaced(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) { if (provenance != null) provenance.markPlaced(new BlockKey(worldId(world), pos.getX(), pos.getY(), pos.getZ())); }
    private static UUID worldId(net.minecraft.world.level.Level world) { return UUID.nameUUIDFromBytes(world.dimension().location().toString().getBytes(StandardCharsets.UTF_8)); }
    private static String serverId() { return Path.of(".").toAbsolutePath().normalize().toString(); }
}
