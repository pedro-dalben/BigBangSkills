package com.bigbangcraft.bigbangskills.common.leaderboard;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaderboardEntry(UUID playerId, BigDecimal totalXp) {}
