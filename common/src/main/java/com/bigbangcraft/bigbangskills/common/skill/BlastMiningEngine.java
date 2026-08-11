package com.bigbangcraft.bigbangskills.common.skill;

/** Pure TNT ability values from the fixed mcMMO baseline. */
public final class BlastMiningEngine {
    private static final float[] RADIUS_BONUS = {0, 1, 1, 2, 2, 3, 3, 4, 4};
    private static final double[] DAMAGE_REDUCTION = {0, 0, 0, 0, 25, 25, 50, 50, 100};
    private static final double[] ORE_BONUS = {0, 35, 40, 45, 50, 55, 60, 65, 70};

    public float radius(int rank) { return radius(rank, true); }
    public float radius(int rank, boolean biggerBombs) {
        return 4.0F + (biggerBombs ? RADIUS_BONUS[Math.max(0, Math.min(8, rank))] : 0);
    }
    public double damageReductionPercent(int rank) { return DAMAGE_REDUCTION[Math.max(0, Math.min(8, rank))]; }
    public double oreBonusPercent(int rank) { return ORE_BONUS[Math.max(0, Math.min(8, rank))]; }

    public double oreYield(int rank) {
        return Math.min(3.0, 1.0 + oreBonusPercent(rank) / 100.0);
    }

    public int bonusDropMultiplier(int rank, boolean enabled) {
        if (!enabled) return 0;
        return rank >= 7 ? 3 : rank >= 3 ? 2 : rank >= 1 ? 1 : 0;
    }

    public boolean illegalDrop(String blockId) {
        if (blockId == null || blockId.isBlank()) return true;
        var path = blockId.substring(blockId.indexOf(':') + 1);
        return path.startsWith("infested_") || path.equals("budding_amethyst") || path.equals("spawner");
    }
}
