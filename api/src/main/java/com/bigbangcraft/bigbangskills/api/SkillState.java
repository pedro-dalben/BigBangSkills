package com.bigbangcraft.bigbangskills.api;

import java.math.BigDecimal;

public record SkillState(SkillId skillId, BigDecimal totalXp, int level, long revision) {}
