package com.bigbangcraft.bigbangskills.common.skill;

/** Side effects for a successful block break; the loader applies them to vanilla drops/tool state. */
public record BlockBreakEffect(int extraDrops, boolean abilityDurabilityCost, int chainBreaks, boolean chainSameType, boolean includeLeaves) {
    public BlockBreakEffect(int extraDrops, boolean abilityDurabilityCost, int chainBreaks, boolean chainSameType) {
        this(extraDrops, abilityDurabilityCost, chainBreaks, chainSameType, false);
    }
    public BlockBreakEffect(int extraDrops, boolean abilityDurabilityCost) {
        this(extraDrops, abilityDurabilityCost, 0, false);
    }
    public BlockBreakEffect {
        if (extraDrops < 0 || chainBreaks < 0) throw new IllegalArgumentException("Block effect values cannot be negative");
    }

    public static BlockBreakEffect none() { return new BlockBreakEffect(0, false, 0, false, false); }
}
