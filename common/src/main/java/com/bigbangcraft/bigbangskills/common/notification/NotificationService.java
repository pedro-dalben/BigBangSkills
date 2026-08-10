package com.bigbangcraft.bigbangskills.common.notification;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/** THREAD SAFE; contains no Minecraft objects. */
public final class NotificationService {
    public record Feedback(UUID playerId, SkillId skillId, BigDecimal amount, int fromLevel, int toLevel) {}
    private record Bucket(UUID playerId, SkillId skillId, BigDecimal amount, int fromLevel, int toLevel, Instant lastUpdate) {}
    private final Duration window;
    private final HashMap<UUID, Bucket> buckets = new HashMap<>();

    public NotificationService(Duration window) {
        if (window == null || window.isNegative() || window.isZero()) throw new IllegalArgumentException("window must be positive");
        this.window = window;
    }

    public synchronized List<Feedback> recordXp(UUID playerId, SkillId skillId, BigDecimal amount, int fromLevel, int toLevel, Instant now) {
        var current = buckets.get(playerId);
        if (current == null) {
            buckets.put(playerId, new Bucket(playerId, skillId, amount, fromLevel, toLevel, now));
            return List.of();
        }
        if (current.skillId().equals(skillId) && now.isBefore(current.lastUpdate().plus(window))) {
            buckets.put(playerId, new Bucket(playerId, skillId, current.amount().add(amount), current.fromLevel(), toLevel, now));
            return List.of();
        }
        buckets.put(playerId, new Bucket(playerId, skillId, amount, fromLevel, toLevel, now));
        return List.of(feedback(current));
    }

    public synchronized List<Feedback> flush(Instant now) {
        var result = new ArrayList<Feedback>();
        buckets.values().removeIf(bucket -> {
            if (!now.isBefore(bucket.lastUpdate().plus(window))) { result.add(feedback(bucket)); return true; }
            return false;
        });
        return List.copyOf(result);
    }

    public synchronized void clear(UUID playerId) { buckets.remove(playerId); }

    private static Feedback feedback(Bucket bucket) { return new Feedback(bucket.playerId(), bucket.skillId(), bucket.amount(), bucket.fromLevel(), bucket.toLevel()); }
}
