package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;

/** Pure TNT ability values from the fixed mcMMO baseline. */
public final class BlastMiningEngine {
    private final double baseRadius;
    private final double[] radiusBonus;
    private final double[] damageReduction;
    private final double[] oreBonus;

    public BlastMiningEngine() { this(SkillFormulaConfig.defaults()); }

    public BlastMiningEngine(SkillFormulaConfig formulas) {
        baseRadius = formulas.value("mining.blast_base_radius");
        radiusBonus = ranks(formulas, "mining.blast_radius_bonus_rank_", new double[]{0, 1, 1, 2, 2, 3, 3, 4, 4});
        damageReduction = ranks(formulas, "mining.blast_damage_reduction_rank_", new double[]{0, 0, 0, 0, 25, 25, 50, 50, 100});
        oreBonus = ranks(formulas, "mining.blast_ore_bonus_rank_", new double[]{0, 35, 40, 45, 50, 55, 60, 65, 70});
    }

    public float radius(int rank) { return radius(rank, true); }
    public float radius(int rank, boolean biggerBombs) {
        return (float) (baseRadius + (biggerBombs ? radiusBonus[Math.max(0, Math.min(8, rank))] : 0));
    }
    public double damageReductionPercent(int rank) { return damageReduction[Math.max(0, Math.min(8, rank))]; }
    public double oreBonusPercent(int rank) { return oreBonus[Math.max(0, Math.min(8, rank))]; }

    public double oreYield(int rank) {
        return Math.min(3.0, 1.0 + oreBonusPercent(rank) / 100.0);
    }

    public int bonusDropMultiplier(int rank, boolean enabled) {
        if (!enabled) return 0;
        return rank >= 7 ? 3 : rank >= 3 ? 2 : rank >= 1 ? 1 : 0;
    }

    public boolean illegalDrop(String blockId) {
        if (blockId == null || blockId.isBlank()) return true;
        var path = blockId.substring(blockId.indexOf(':') + 1);
        return path.startsWith("infested_") || path.equals("budding_amethyst") || path.equals("spawner");
    }

    private static double[] ranks(SkillFormulaConfig formulas, String prefix, double[] defaults) {
        var values = defaults.clone();
        for (var i = 0; i < values.length; i++) {
            var value = formulas.value(prefix + i);
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid Blast Mining formula: " + prefix + i);
            values[i] = value;
        }
        return values;
    }
}
