package com.bigbangcraft.bigbangskills.common.skill;

import java.util.function.DoubleSupplier;

/** Pure Woodcutting formulas shared by both loaders. */
public final class WoodcuttingEngine {
    public int treeFellerXp(int rawXp, int processedLogs, boolean reduced) {
        if (rawXp <= 0) return 0;
        return reduced ? Math.max(1, rawXp - Math.max(0, processedLogs) * 5) : rawXp;
    }

    public boolean normalTreePartDrops(DoubleSupplier random) { return random.getAsDouble() < .75; }

    public boolean knockOnWoodXpOrb(int rank, boolean enabled, DoubleSupplier random) {
        return enabled && rank >= 2 && random.getAsDouble() < .10;
    }
}
