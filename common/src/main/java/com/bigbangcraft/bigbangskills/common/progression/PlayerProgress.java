package com.bigbangcraft.bigbangskills.common.progression;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.skill.SkillRelationships;
import java.math.BigDecimal;
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

    /** Rebuilds non-persisted child skill views from their parent skills. */
    public void refreshDerived() {
        for (var child : new SkillId[]{SkillId.parse("bigbangskills:salvage"), SkillId.parse("bigbangskills:smelting")}) {
            var parents = SkillRelationships.parents(child);
            var states = parents.stream().map(skills::get).filter(java.util.Objects::nonNull).toList();
            if (states.isEmpty()) continue;
            var level = states.stream().mapToInt(SkillProgress::level).sum() / states.size();
            var revision = states.stream().mapToLong(SkillProgress::revision).max().orElse(0);
            skills.put(child, new SkillProgress(child, BigDecimal.ZERO, level, revision));
        }
    }
}
