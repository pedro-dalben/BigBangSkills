package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillRegistry {
    private final Map<SkillId, SkillDefinition> definitions = new ConcurrentHashMap<>();
    public void register(SkillDefinition definition) {
        if (definitions.putIfAbsent(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate skill: " + definition.id());
    }
    public Optional<SkillDefinition> get(SkillId id) { return Optional.ofNullable(definitions.get(id)); }
    public Map<SkillId, SkillDefinition> snapshot() { return Map.copyOf(definitions); }
}
