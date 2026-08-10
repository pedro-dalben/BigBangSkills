package com.bigbangcraft.bigbangskills.common.progression;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerProgress {
    private final UUID playerId;
    private final Map<SkillId, SkillProgress> skills = new ConcurrentHashMap<>();
    public PlayerProgress(UUID playerId) { this.playerId = playerId; }
    public UUID playerId() { return playerId; }
    public Map<SkillId, SkillProgress> skills() { return Map.copyOf(skills); }
    public SkillProgress get(SkillId id) { return skills.get(id); }
    public void put(SkillProgress progress) { skills.put(progress.skillId(), progress); }
}
