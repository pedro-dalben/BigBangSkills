package com.bigbangcraft.bigbangskills.common.ability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityService {
    private final CooldownService cooldowns;
    private final Map<UUID, Map<String, Instant>> active = new ConcurrentHashMap<>();
    public AbilityService(Clock clock) { cooldowns = new CooldownService(clock); }
    public boolean activate(UUID playerId, AbilityDefinition ability, int level, Instant now) {
        var duration = ability.duration().isZero() && ability.type() == AbilityType.ACTIVE
                ? Duration.ofSeconds(2L + Math.min(50, Math.max(0, level)) / 5L) : ability.duration();
        return activate(playerId, ability, level, now, ability.cooldown(), duration);
    }
    public boolean activate(UUID playerId, AbilityDefinition ability, int level, Instant now, Duration cooldown, Duration duration) {
        if (level < ability.unlockLevel() || !cooldowns.consume(playerId, ability.id(), cooldown)) return false;
        if (ability.type() == AbilityType.ACTIVE) active.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(ability.id(), now.plus(duration));
        return true;
    }
    public boolean isActive(UUID playerId, String abilityId, Instant now) { return active.getOrDefault(playerId, Map.of()).getOrDefault(abilityId, Instant.MIN).isAfter(now); }
    public void expire(Instant now) { active.values().forEach(states -> states.entrySet().removeIf(entry -> !entry.getValue().isAfter(now))); }
    public void clear(UUID playerId) { active.remove(playerId); cooldowns.clear(playerId); }
}
