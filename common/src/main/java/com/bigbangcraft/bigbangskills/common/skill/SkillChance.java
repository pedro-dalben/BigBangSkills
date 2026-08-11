package com.bigbangcraft.bigbangskills.common.skill;

import java.util.function.DoubleSupplier;

public final class SkillChance {
    private SkillChance() {}

    public static double linearPercent(int level, int maxBonusLevel, double maximumPercent) {
        if (level <= 0 || maxBonusLevel <= 0 || maximumPercent <= 0) return 0;
        return Math.min(maximumPercent, maximumPercent * level / maxBonusLevel);
    }

    public static boolean succeeds(double percent, DoubleSupplier randomUnit) {
        var random = randomUnit.getAsDouble();
        if (Double.isNaN(random) || random < 0 || random >= 1) throw new IllegalArgumentException("Random value must be in [0, 1)");
        return percent > 0 && random < percent / 100.0;
    }
}
