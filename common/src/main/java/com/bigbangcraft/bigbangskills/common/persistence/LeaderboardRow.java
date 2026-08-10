package com.bigbangcraft.bigbangskills.common.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeaderboardRow(UUID playerId, BigDecimal totalXp, Instant updatedAt) {}
