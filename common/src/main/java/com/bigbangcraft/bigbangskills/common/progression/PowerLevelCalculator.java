package com.bigbangcraft.bigbangskills.common.progression;

import com.bigbangcraft.bigbangskills.common.skill.SkillRelationships;

public final class PowerLevelCalculator {
    public int calculate(PlayerProgress progress) { return progress.skills().values().stream().filter(state -> !SkillRelationships.isChild(state.skillId())).mapToInt(SkillProgress::level).sum(); }
}
