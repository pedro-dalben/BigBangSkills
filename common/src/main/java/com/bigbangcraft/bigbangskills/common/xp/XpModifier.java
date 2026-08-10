package com.bigbangcraft.bigbangskills.common.xp;

import java.math.BigDecimal;

public record XpModifier(String id, int order, BigDecimal multiplier) {
    public XpModifier { if (id.isBlank() || multiplier.signum() < 0) throw new IllegalArgumentException("Invalid modifier"); }
}
