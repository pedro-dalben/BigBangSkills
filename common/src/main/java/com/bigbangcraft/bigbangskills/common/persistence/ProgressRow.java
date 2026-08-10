package com.bigbangcraft.bigbangskills.common.persistence;

import com.bigbangcraft.bigbangskills.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgressRow(UUID playerId, SkillId skillId, ProgressionScope scope, BigDecimal totalXp, long revision, int definitionVersion, Instant updatedAt) {}
