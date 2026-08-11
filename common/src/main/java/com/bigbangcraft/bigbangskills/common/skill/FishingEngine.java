package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic Fishing rules and the small state needed by its exploit guard. */
public final class FishingEngine {
    private static final int[] SHAKE = {15, 20, 25, 35, 45, 55, 65, 75};
    private static final int[] VANILLA_XP = {1, 2, 3, 3, 4, 4, 5, 5};
    private final ConcurrentHashMap<UUID, CatchState> catches = new ConcurrentHashMap<>();
    private final int[] shake;
    private final int[] vanillaXp;
    private final int moveRange;
    private final int overFishLimit;
    private final int masterAnglerMinWaitPerRank;
    private final int masterAnglerMaxWaitPerRank;
    private final int masterAnglerBoatMinWait;
    private final int masterAnglerBoatMaxWait;
    private final int masterAnglerLureWait;
    private final int masterAnglerMinWaitCap;
    private final int masterAnglerMaxWaitCap;
    private final int fishermansDietRankChange;

    public FishingEngine() { this(3, 10, SkillFormulaConfig.defaults()); }
    public FishingEngine(int moveRange, int overFishLimit) { this(moveRange, overFishLimit, SkillFormulaConfig.defaults()); }
    public FishingEngine(int moveRange, int overFishLimit, SkillFormulaConfig formulas) {
        if (moveRange < 0 || overFishLimit < 1) throw new IllegalArgumentException("Invalid fishing exploit limits");
        this.shake = ranks(formulas, "fishing.shake_chance_rank_", SHAKE);
        this.vanillaXp = ranks(formulas, "fishing.vanilla_xp_multiplier_rank_", VANILLA_XP);
        this.moveRange = moveRange;
        this.overFishLimit = overFishLimit;
        this.masterAnglerMinWaitPerRank = integer(formulas, "fishing.master_angler_min_wait_per_rank", true);
        this.masterAnglerMaxWaitPerRank = integer(formulas, "fishing.master_angler_max_wait_per_rank", true);
        this.masterAnglerBoatMinWait = integer(formulas, "fishing.master_angler_boat_min_wait", false);
        this.masterAnglerBoatMaxWait = integer(formulas, "fishing.master_angler_boat_max_wait", false);
        this.masterAnglerLureWait = integer(formulas, "fishing.master_angler_lure_wait", false);
        this.masterAnglerMinWaitCap = integer(formulas, "fishing.master_angler_min_wait_cap", true);
        this.masterAnglerMaxWaitCap = integer(formulas, "fishing.master_angler_max_wait_cap", true);
        this.fishermansDietRankChange = integer(formulas, "fishing.fishermans_diet_rank_change", true);
    }

    public int shakeChance(int rank) { return shake[Math.max(1, Math.min(shake.length, rank)) - 1]; }
    public int vanillaXpMultiplier(int rank) { return vanillaXp[Math.max(1, Math.min(vanillaXp.length, rank)) - 1]; }
    public int treasureTier(int level) { return rank(level, new int[]{1, 25, 35, 50, 65, 75, 85, 100}); }
    public int shakeRank(int level) { return rank(level, new int[]{15, 20, 25, 30, 40, 50, 60, 70}); }
    public int vanillaXpRank(int level) { return treasureTier(level); }
    public int boostedVanillaXp(int vanillaXp, int rank) { return vanillaXp <= 1 ? vanillaXp : vanillaXp * vanillaXpMultiplier(rank); }
    public int fishermanDiet(int level, int food) {
        return food + Math.min(5, Math.max(0, level / fishermansDietRankChange));
    }
    public int masterAnglerMinWaitReduction(int level, boolean boat) {
        return masterAnglerRank(level) * masterAnglerMinWaitPerRank + (boat ? masterAnglerBoatMinWait : 0);
    }
    public int masterAnglerMaxWaitReduction(int level, boolean boat, int lureLevel) {
        return masterAnglerRank(level) * masterAnglerMaxWaitPerRank + (boat ? masterAnglerBoatMaxWait : 0) + Math.max(0, lureLevel) * masterAnglerLureWait;
    }
    public int masterAnglerMinWaitCap() { return masterAnglerMinWaitCap; }
    public int masterAnglerMaxWaitCap() { return masterAnglerMaxWaitCap; }
    public boolean canIceFish(int level, boolean ice, boolean icyBiome, boolean waterThreeBlocksBelow) {
        return level >= 5 && ice && (icyBiome || waterThreeBlocksBelow);
    }
    public boolean magicHunter(int level) { return level >= 20; }

    /** Returns false after too many catches in one small stationary area. */
    public boolean acceptCatch(UUID player, double x, double y, double z, long tick) {
        var prior = catches.get(player);
        var state = catches.compute(player, (ignored, previous) -> {
            if (previous == null || distance(previous.x, previous.y, previous.z, x, y, z) > moveRange) return new CatchState(x, y, z, tick, 1);
            return new CatchState(previous.x, previous.y, previous.z, tick, previous.count + 1);
        });
        return (prior == null || tick - prior.tick >= 20) && state.count < overFishLimit;
    }

    public void clear(UUID player) { catches.remove(player); }
    public int overFishLimit() { return overFishLimit; }
    public int moveRange() { return moveRange; }

    private static double distance(double ax, double ay, double az, double bx, double by, double bz) {
        return Math.max(Math.max(Math.abs(ax - bx), Math.abs(ay - by)), Math.abs(az - bz));
    }
    private static int rank(int level, int[] unlocks) {
        var rank = 0;
        for (var unlock : unlocks) if (level >= unlock) rank++;
        return Math.min(unlocks.length, rank);
    }
    private static int masterAnglerRank(int level) { return rank(level, new int[]{1, 20, 30, 40, 60, 70, 80, 90}); }
    private static int[] ranks(SkillFormulaConfig formulas, String prefix, int[] defaults) {
        var values = defaults.clone();
        for (var i = 0; i < values.length; i++) {
            var value = formulas.value(prefix + (i + 1));
            if (value < 0 || value != Math.rint(value)) throw new IllegalArgumentException("Invalid fishing rank formula");
            values[i] = (int) value;
        }
        return values;
    }
    private static int integer(SkillFormulaConfig formulas, String key, boolean positive) {
        var value = formulas.value(key);
        if (value < 0 || value != Math.rint(value) || (positive && value <= 0)) throw new IllegalArgumentException("Invalid fishing formula: " + key);
        return (int) value;
    }
    private record CatchState(double x, double y, double z, long tick, int count) {}
}
