package com.bigbangcraft.bigbangskills.common.config;

import java.math.BigDecimal;
import java.util.Map;

public record ProgressionConfig(BigDecimal globalMultiplier, BigDecimal perActionCap, boolean rejectUnknownProvenance, Map<String, BigDecimal> blockXp) {
    public ProgressionConfig {
        if (globalMultiplier == null || globalMultiplier.signum() < 0 || perActionCap == null || perActionCap.signum() < 0) throw new IllegalArgumentException("Invalid progression config");
        blockXp = Map.copyOf(blockXp);
        blockXp.forEach((id, xp) -> { if (id == null || id.isBlank() || xp == null || xp.signum() < 0) throw new IllegalArgumentException("Invalid block XP: " + id); });
    }
    public static ProgressionConfig defaults() { return new ProgressionConfig(BigDecimal.ONE, BigDecimal.valueOf(10_000), true, Map.of("minecraft:stone", BigDecimal.ONE, "minecraft:oak_log", BigDecimal.ONE)); }
}
