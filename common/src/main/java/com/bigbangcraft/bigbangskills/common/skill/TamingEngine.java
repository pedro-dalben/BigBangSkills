package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;

import java.util.function.DoubleSupplier;

/** Pure Taming combat/defence formulas; loaders only apply the returned effects. */
public final class TamingEngine {
    private static final SkillId TAMING = SkillId.parse("bigbangskills:taming");
    private final SkillFormulaConfig formulas;
    private final DoubleSupplier random;

    public TamingEngine() { this(SkillFormulaConfig.defaults(), java.util.concurrent.ThreadLocalRandom.current()::nextDouble); }
    public TamingEngine(SkillFormulaConfig formulas, DoubleSupplier random) { this.formulas = formulas; this.random = random; }

    public TamingAttack resolveAttack(PlayerProgress progress, double damage) {
        int level = progress.get(TAMING) == null ? 1 : progress.get(TAMING).level();
        boolean gore = unlocked("gore", level) && succeeds(SkillChance.linearPercent(level, 100, formulas.value("combat.taming.gore_max_percent")));
        boolean claws = unlocked("sharpened_claws", level);
        boolean fastFood = unlocked("fast_food_service", level) && succeeds(formulas.value("taming.fast_food_chance"));
        boolean pummel = unlocked("pummel", level) && succeeds(formulas.value("taming.pummel_chance"));
        return new TamingAttack(gore ? formulas.value("combat.taming.gore_multiplier") : 1,
                claws ? formulas.value("taming.sharpened_claws_bonus") : 0, fastFood, pummel);
    }

    public double incomingDamage(PlayerProgress progress, double damage, boolean explosion, boolean fire, boolean fall) {
        return incomingDamage(progress, damage, explosion, fire, fall, false);
    }

    public double incomingDamage(PlayerProgress progress, double damage, boolean explosion, boolean fire, boolean fall, boolean environmental) {
        return incomingDamage(progress, damage, explosion, fire, fall, environmental, Double.POSITIVE_INFINITY);
    }

    public double incomingDamage(PlayerProgress progress, double damage, boolean explosion, boolean fire, boolean fall,
                                 boolean environmental, double health) {
        int level = progress.get(TAMING) == null ? 1 : progress.get(TAMING).level();
        if ((fall || environmental) && damage <= health && unlocked("environmentally_aware", level)) return 0;
        if (explosion && unlocked("shock_proof", level)) return damage / formulas.value("taming.shock_proof_divisor");
        if (fire && unlocked("thick_fur", level)) return damage / formulas.value("taming.thick_fur_divisor");
        return damage;
    }

    public boolean hasAbility(PlayerProgress progress, String ability) {
        int level = progress.get(TAMING) == null ? 1 : progress.get(TAMING).level();
        return unlocked(ability, level);
    }

    private boolean unlocked(String ability, int level) {
        return DefaultAbilityCatalog.all().getOrDefault(TAMING, java.util.List.of()).stream()
                .anyMatch(definition -> definition.id().equals("taming." + ability) && level >= definition.unlockLevel());
    }
    private boolean succeeds(double percent) { return SkillChance.succeeds(percent, random); }
    public record TamingAttack(double damageMultiplier, double bonusDamage, boolean fastFood, boolean pummel) {}
}
