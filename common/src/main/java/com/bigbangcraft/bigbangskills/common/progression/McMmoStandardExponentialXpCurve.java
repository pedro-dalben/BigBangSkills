package com.bigbangcraft.bigbangskills.common.progression;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Standard-mode exponential curve matching mcMMO's ten-retro-level grouping. */
public final class McMmoStandardExponentialXpCurve implements XpCurve {
    private final int base;
    private final BigDecimal multiplier;
    private final BigDecimal exponent;

    public McMmoStandardExponentialXpCurve(int base, BigDecimal multiplier, BigDecimal exponent) {
        if (base <= 0 || multiplier.signum() <= 0 || exponent.signum() <= 0) throw new IllegalArgumentException("Invalid exponential mcMMO curve");
        this.base = base;
        this.multiplier = multiplier;
        this.exponent = exponent;
    }

    @Override
    public BigDecimal totalXpForLevel(int level) {
        if (level <= 1) return BigDecimal.ZERO;
        var retroLevels = Math.multiplyExact((long) level - 1, 10L);
        var total = BigDecimal.ZERO;
        for (long retroLevel = 1; retroLevel <= retroLevels; retroLevel++) total = total.add(xpForRetroLevel(retroLevel));
        return total;
    }

    @Override
    public int levelAt(BigDecimal totalXp, int maxLevel) {
        if (totalXp.signum() < 0) throw new IllegalArgumentException("XP cannot be negative");
        var low = 1;
        var high = maxLevel;
        while (low < high) {
            var middle = (low + high + 1) >>> 1;
            if (totalXpForLevel(middle).compareTo(totalXp) <= 0) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private BigDecimal xpForRetroLevel(long level) {
        var raw = multiplier.doubleValue() * Math.pow(level, exponent.doubleValue()) + base;
        return BigDecimal.valueOf(Math.floor(raw)).setScale(0, RoundingMode.DOWN);
    }
}
