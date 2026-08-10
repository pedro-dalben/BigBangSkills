package com.bigbangcraft.bigbangskills.common.progression;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LinearXpCurve(BigDecimal base, BigDecimal step) implements XpCurve {
    public LinearXpCurve {
        if (base.signum() < 0 || step.signum() <= 0) throw new IllegalArgumentException("Invalid linear curve");
    }
    @Override public BigDecimal totalXpForLevel(int level) {
        if (level <= 1) return BigDecimal.ZERO;
        var n = BigDecimal.valueOf(level - 1);
        return n.multiply(base).add(step.multiply(n.multiply(n.subtract(BigDecimal.ONE)).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP))).stripTrailingZeros();
    }
    @Override public int levelAt(BigDecimal totalXp, int maxLevel) {
        if (totalXp.signum() < 0) throw new IllegalArgumentException("XP cannot be negative");
        int low = 1, high = maxLevel;
        while (low < high) { int mid = (low + high + 1) >>> 1; if (totalXpForLevel(mid).compareTo(totalXp) <= 0) low = mid; else high = mid - 1; }
        return low;
    }
}
