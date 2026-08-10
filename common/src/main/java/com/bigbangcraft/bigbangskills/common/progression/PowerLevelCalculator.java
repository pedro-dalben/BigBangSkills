package com.bigbangcraft.bigbangskills.common.progression;

public final class PowerLevelCalculator {
    public int calculate(PlayerProgress progress) { return progress.skills().values().stream().mapToInt(SkillProgress::level).sum(); }
}
