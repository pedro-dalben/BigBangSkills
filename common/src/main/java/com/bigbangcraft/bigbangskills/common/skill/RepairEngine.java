package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;

import java.util.function.DoubleSupplier;

/** Reference repair math; inventory/anvil mutation remains loader-owned. */
public final class RepairEngine {
    private final SkillFormulaConfig formulas;
    private final DoubleSupplier randomUnit;

    public RepairEngine(SkillFormulaConfig formulas, DoubleSupplier randomUnit) {
        this.formulas = java.util.Objects.requireNonNull(formulas);
        this.randomUnit = java.util.Objects.requireNonNull(randomUnit);
    }

    public int repairedDurability(int currentDamage, int baseAmount, int level) {
        if (currentDamage <= 0 || baseAmount <= 0) return 0;
        var mastery = formulas.value("repair.mastery_max_percent")
                * Math.min(1.0, Math.max(0, level) / formulas.value("repair.mastery_max_level")) / 100.0;
        var amount = (int) Math.round(baseAmount * (1.0 + mastery));
        if (SkillChance.succeeds(SkillChance.linearPercent(level, (int) formulas.value("repair.super_repair_max_level"), formulas.value("repair.super_repair_max_percent")), randomUnit)) amount *= 2;
        return Math.min(currentDamage, Math.max(1, amount));
    }

    public int arcaneForgingLevel(int rank, int enchantmentLevel) {
        if (enchantmentLevel <= 0 || rank <= 0) return 0;
        var boundedRank = Math.min(8, rank);
        var keep = formulas.value("repair.arcane_forging_keep_rank_" + boundedRank);
        var downgrade = formulas.value("repair.arcane_forging_downgrade_rank_" + boundedRank);
        if (randomUnit.getAsDouble() * 100 >= keep) return 0;
        var level = Math.min(5, enchantmentLevel);
        return enchantmentLevel > 1 && randomUnit.getAsDouble() * 100 >= 100 - downgrade
                ? level - 1 : level;
    }
}
