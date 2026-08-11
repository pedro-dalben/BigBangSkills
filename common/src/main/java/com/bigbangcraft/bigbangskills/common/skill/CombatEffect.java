package com.bigbangcraft.bigbangskills.common.skill;

public record CombatEffect(double damageMultiplier, double bonusDamage, double aoeDamage,
                           boolean daze, boolean rupture, boolean disarm, boolean arrowDeflect,
                           int armorDurabilityDamage, boolean cripple, boolean momentum,
                           int statusDurationTicks, int statusAmplifier,
                           boolean fastFood, boolean pummel,
                           double ruptureTickDamage, int ruptureDurationTicks,
                           boolean greaterImpact) {
    public CombatEffect(double damageMultiplier, double bonusDamage, double aoeDamage, boolean daze, boolean rupture,
                        boolean disarm, boolean arrowDeflect, int armorDurabilityDamage) {
        this(damageMultiplier, bonusDamage, aoeDamage, daze, rupture, disarm, arrowDeflect, armorDurabilityDamage, false, false, 40, 0, false, false, 0, 0, false);
    }
    public CombatEffect(double damageMultiplier, double bonusDamage, double aoeDamage, boolean daze, boolean rupture,
                        boolean disarm, boolean arrowDeflect, int armorDurabilityDamage, boolean cripple, boolean momentum) {
        this(damageMultiplier, bonusDamage, aoeDamage, daze, rupture, disarm, arrowDeflect, armorDurabilityDamage, cripple, momentum, 40, 0, false, false, 0, 0, false);
    }
    public CombatEffect(double damageMultiplier, double bonusDamage, double aoeDamage, boolean daze, boolean rupture,
                        boolean disarm, boolean arrowDeflect, int armorDurabilityDamage, boolean cripple, boolean momentum,
                        int statusDurationTicks, int statusAmplifier) {
        this(damageMultiplier, bonusDamage, aoeDamage, daze, rupture, disarm, arrowDeflect, armorDurabilityDamage, cripple, momentum, statusDurationTicks, statusAmplifier, false, false, 0, 0, false);
    }
    public CombatEffect(double damageMultiplier, double bonusDamage, double aoeDamage, boolean daze, boolean rupture,
                        boolean disarm, boolean arrowDeflect, int armorDurabilityDamage, boolean cripple, boolean momentum,
                        int statusDurationTicks, int statusAmplifier, boolean fastFood, boolean pummel) {
        this(damageMultiplier, bonusDamage, aoeDamage, daze, rupture, disarm, arrowDeflect, armorDurabilityDamage,
                cripple, momentum, statusDurationTicks, statusAmplifier, fastFood, pummel, 0, 0, false);
    }
    public CombatEffect {
        if (!Double.isFinite(damageMultiplier) || damageMultiplier < 0 || !Double.isFinite(bonusDamage)
                || bonusDamage < 0 || !Double.isFinite(aoeDamage) || aoeDamage < 0 || armorDurabilityDamage < 0
                || statusDurationTicks < 0 || statusAmplifier < 0 || !Double.isFinite(ruptureTickDamage)
                || ruptureTickDamage < 0 || ruptureDurationTicks < 0) {
            throw new IllegalArgumentException("Invalid combat effect");
        }
    }
    public static CombatEffect none() { return new CombatEffect(1, 0, 0, false, false, false, false, 0, false, false, 40, 0, false, false, 0, 0, false); }

    public CombatEffect withBonusDamage(double value) {
        return new CombatEffect(damageMultiplier, value, aoeDamage, daze, rupture, disarm, arrowDeflect,
                armorDurabilityDamage, cripple, momentum, statusDurationTicks, statusAmplifier,
                fastFood, pummel, ruptureTickDamage, ruptureDurationTicks, greaterImpact);
    }
}
