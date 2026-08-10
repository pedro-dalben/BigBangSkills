package com.bigbangcraft.bigbangskills.common.leaderboard;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LeaderboardService {
    private final Clock clock;
    private final Map<SkillId, List<LeaderboardEntry>> snapshots = new ConcurrentHashMap<>();
    private volatile Instant refreshedAt = Instant.MIN;
    public LeaderboardService(Clock clock) { this.clock = clock; }
    public synchronized void refresh(Collection<PlayerProgress> players) {
        var ids = players.stream().flatMap(player -> player.skills().keySet().stream()).collect(Collectors.toSet());
        ids.forEach(skill -> snapshots.put(skill, players.stream().map(player -> {
            var state = player.get(skill);
            return state == null ? null : new LeaderboardEntry(player.playerId(), state.totalXp());
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LeaderboardEntry::totalXp).reversed().thenComparing(entry -> entry.playerId().toString())).limit(100).toList()));
        refreshedAt = clock.instant();
    }
    public List<LeaderboardEntry> top(SkillId skill, int limit) { if (limit < 1) return List.of(); return snapshots.getOrDefault(skill, List.of()).stream().limit(Math.min(limit, 100)).toList(); }
    public Instant refreshedAt() { return refreshedAt; }
}
