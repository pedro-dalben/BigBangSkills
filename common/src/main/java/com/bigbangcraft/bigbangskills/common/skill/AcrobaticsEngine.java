package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;

import java.util.function.DoubleSupplier;

/** Fall resolution shared by Fabric and NeoForge. */
public final class AcrobaticsEngine {
    private static final SkillId ACROBATICS = SkillId.parse("bigbangskills:acrobatics");
    private final DoubleSupplier randomUnit;
    private final SkillFormulaConfig formulas;

    public AcrobaticsEngine() { this(SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public AcrobaticsEngine(DoubleSupplier randomUnit) { this(SkillFormulaConfig.defaults(), randomUnit); }
    public AcrobaticsEngine(SkillFormulaConfig formulas, DoubleSupplier randomUnit) { this.formulas = java.util.Objects.requireNonNull(formulas); this.randomUnit = java.util.Objects.requireNonNull(randomUnit); }

    public AcrobaticsEffect resolve(PlayerProgress progress, float distance) {
        return resolve(progress, distance, false, Double.POSITIVE_INFINITY);
    }

    public AcrobaticsEffect resolve(PlayerProgress progress, float distance, boolean sneaking) {
        return resolve(progress, distance, sneaking, Double.POSITIVE_INFINITY);
    }

    public AcrobaticsEffect resolve(PlayerProgress progress, float distance, boolean sneaking, double health) {
        var state = progress.get(ACROBATICS);
        var level = state == null ? 1 : state.level();
        if (distance <= 3 || !unlocked("roll", level)) return AcrobaticsEffect.none();
        var damage = distance - 3;
        var threshold = sneaking ? formulas.value("acrobatics.graceful_roll_damage_threshold") : formulas.value("acrobatics.roll_damage_threshold");
        var chance = SkillChance.linearPercent(level, (int) formulas.value("acrobatics.roll_max_level"), formulas.value("acrobatics.roll_chance_max"));
        if (!SkillChance.succeeds(chance, randomUnit)) return AcrobaticsEffect.none();
        var modifiedDamage = Math.max(damage - threshold, 0);
        if (modifiedDamage >= health) return AcrobaticsEffect.none();
        var multiplier = damage <= 0 ? 1 : modifiedDamage / damage;
        return new AcrobaticsEffect(true, multiplier);
    }

    public AcrobaticsEffect resolveDodge(PlayerProgress progress) {
        return resolveDodge(progress, 0, Double.POSITIVE_INFINITY);
    }

    public AcrobaticsEffect resolveDodge(PlayerProgress progress, double damage, double health) {
        var state = progress.get(ACROBATICS);
        var level = state == null ? 1 : state.level();
        if (!unlocked("dodge", level)) return AcrobaticsEffect.none();
        var modifiedDamage = Math.max(damage / formulas.value("acrobatics.dodge_damage_divisor"), 1.0);
        if (modifiedDamage >= health) return AcrobaticsEffect.none();
        var chance = SkillChance.linearPercent(level, (int) formulas.value("acrobatics.dodge_max_level"), formulas.value("acrobatics.dodge_chance_max"));
        return SkillChance.succeeds(chance, randomUnit) ? new AcrobaticsEffect(false, 1 / formulas.value("acrobatics.dodge_damage_divisor"), true) : AcrobaticsEffect.none();
    }

    private static boolean unlocked(String ability, int level) {
        return DefaultAbilityCatalog.all().getOrDefault(ACROBATICS, java.util.List.of()).stream()
                .anyMatch(definition -> definition.id().equals("acrobatics." + ability) && level >= definition.unlockLevel());
    }
}
