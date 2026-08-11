package com.bigbangcraft.bigbangskills.common.skill;

/** Pure Smelting formulas; loader hooks own furnace inventories and events. */
public final class SmeltingEngine {
    public int fuelEfficiency(int burnTime, int rank) {
        if (burnTime <= 0) return 0;
        var multiplier = switch (Math.max(0, rank)) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 1;
        };
        return Math.min(Short.MAX_VALUE, Math.max(1, burnTime * multiplier));
    }

    public boolean secondSmelt(int level, boolean enabled, double randomUnit, double chanceMaxPercent, int maxBonusLevel) {
        if (!enabled || level < 1 || maxBonusLevel < 1 || chanceMaxPercent <= 0) return false;
        var chance = Math.min(chanceMaxPercent, chanceMaxPercent * level / maxBonusLevel);
        return randomUnit >= 0 && randomUnit < chance / 100.0;
    }

    public int vanillaXp(int baseXp, int rank) {
        if (baseXp <= 0) return 0;
        var multiplier = switch (Math.max(1, rank)) {
            case 2, 3 -> 2;
            case 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> 1;
        };
        return Math.multiplyExact(baseXp, multiplier);
    }

    public float vanillaXp(float baseXp, int rank) {
        if (baseXp <= 0) return 0;
        var multiplier = switch (Math.max(1, rank)) {
            case 2, 3 -> 2;
            case 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> 1;
        };
        return baseXp * multiplier;
    }
}
