package com.bigbangcraft.bigbangskills.common.ability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownService {
    private final Clock clock;
    private final Map<UUID, Map<String, Instant>> availableAt = new ConcurrentHashMap<>();
    public CooldownService(Clock clock) { this.clock = clock; }
    public boolean ready(UUID playerId, String abilityId) { return !availableAt.getOrDefault(playerId, Map.of()).getOrDefault(abilityId, Instant.MIN).isAfter(clock.instant()); }
    public boolean consume(UUID playerId, String abilityId, Duration cooldown) {
        var now = clock.instant();
        var states = availableAt.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        synchronized (states) {
            var existing = states.get(abilityId);
            if (existing != null && existing.isAfter(now)) return false;
            states.put(abilityId, now.plus(cooldown));
            return true;
        }
    }
    public void clear(UUID playerId) { availableAt.remove(playerId); }
}
