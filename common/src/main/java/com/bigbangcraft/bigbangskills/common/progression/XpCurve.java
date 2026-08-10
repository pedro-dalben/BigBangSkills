package com.bigbangcraft.bigbangskills.common.progression;

import java.math.BigDecimal;

public interface XpCurve {
    BigDecimal totalXpForLevel(int level);
    int levelAt(BigDecimal totalXp, int maxLevel);
}
