package com.bigbangcraft.bigbangskills.common.progression;

import java.math.BigDecimal;
import java.math.BigInteger;

/** mcMMO standard mode: each BigBangSkills level represents ten retro levels. */
public record McMmoStandardLinearXpCurve(int base, int multiplier) implements XpCurve {
    public McMmoStandardLinearXpCurve {
        if (base <= 0 || multiplier <= 0) throw new IllegalArgumentException("Invalid mcMMO curve");
    }

    @Override
    public BigDecimal totalXpForLevel(int level) {
        if (level <= 1) return BigDecimal.ZERO;
        var n = BigInteger.valueOf(level - 1L);
        var perLevel = BigInteger.valueOf(10L * base + 55L * multiplier);
        var total = n.multiply(perLevel).add(
                BigInteger.valueOf(50L * multiplier).multiply(n).multiply(n.subtract(BigInteger.ONE)));
        return new BigDecimal(total);
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
}
