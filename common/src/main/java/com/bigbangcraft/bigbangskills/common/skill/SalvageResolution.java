package com.bigbangcraft.bigbangskills.common.skill;

public record SalvageResolution(boolean accepted, int yield, String reason) {
    public SalvageResolution {
        if (yield < 0) throw new IllegalArgumentException("Salvage yield cannot be negative");
    }
}
