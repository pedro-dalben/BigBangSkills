package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;

/** Pure Smelting formulas; loader hooks own furnace inventories and events. */
public final class SmeltingEngine {
    private final SkillFormulaConfig formulas;

    public SmeltingEngine() { this(SkillFormulaConfig.defaults()); }
    public SmeltingEngine(SkillFormulaConfig formulas) { this.formulas = java.util.Objects.requireNonNull(formulas); }

    public boolean canSecondSmelt(int resultCount, int maxStackSize) {
        return resultCount >= 0 && maxStackSize > 1 && resultCount <= maxStackSize - 2;
    }

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
        return Math.multiplyExact(baseXp, (int) formulas.value("smelting.vanilla_xp_multiplier_rank_" + Math.max(1, Math.min(8, rank))));
    }

    public float vanillaXp(float baseXp, int rank) {
        if (baseXp <= 0) return 0;
        return baseXp * (float) formulas.value("smelting.vanilla_xp_multiplier_rank_" + Math.max(1, Math.min(8, rank)));
    }
}
