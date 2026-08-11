package com.bigbangcraft.bigbangskills.common.skill;

public record AcrobaticsEffect(boolean rollTriggered, double damageMultiplier, boolean dodgeTriggered) {
    public AcrobaticsEffect(boolean rollTriggered, double damageMultiplier) { this(rollTriggered, damageMultiplier, false); }
    public AcrobaticsEffect {
        if (!Double.isFinite(damageMultiplier) || damageMultiplier < 0) throw new IllegalArgumentException("Invalid acrobatics effect");
    }
    public static AcrobaticsEffect none() { return new AcrobaticsEffect(false, 1, false); }
}
