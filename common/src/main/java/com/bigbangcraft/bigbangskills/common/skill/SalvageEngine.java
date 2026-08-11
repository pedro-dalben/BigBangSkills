package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;
import com.bigbangcraft.bigbangskills.common.config.SkillItemTables;
import com.bigbangcraft.bigbangskills.common.config.SkillFormulaConfig;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import java.util.function.DoubleSupplier;

/** Pure durability-to-yield calculation; item mutation stays in the loader. */
public final class SalvageEngine {
    private static final SkillId SALVAGE = SkillId.parse("bigbangskills:salvage");
    private final SkillFormulaConfig formulas;

    public SalvageEngine() { this(SkillFormulaConfig.defaults()); }
    public SalvageEngine(SkillFormulaConfig formulas) { this.formulas = java.util.Objects.requireNonNull(formulas); }

    public SalvageResolution resolve(PlayerProgress progress, String itemId, int damage, int maxDamage, SkillItemTables.SalvageRule rule) {
        return resolve(progress, itemId, damage, maxDamage, rule, false);
    }

    public SalvageResolution resolve(PlayerProgress progress, String itemId, int damage, int maxDamage, SkillItemTables.SalvageRule rule, boolean enchanted) {
        if (rule == null) return new SalvageResolution(false, 0, "item_not_salvageable");
        var state = progress.get(SALVAGE);
        var level = state == null ? 1 : state.level();
        if (level < rule.minimumLevel()) return new SalvageResolution(false, 0, "level_requirement");
        var arcane = DefaultAbilityCatalog.all().getOrDefault(SALVAGE, java.util.List.of()).stream()
                .anyMatch(value -> value.id().equals("salvage.arcane_salvage") && level >= value.unlockLevel());
        if (enchanted && !arcane) return new SalvageResolution(false, 0, "arcane_salvage_required");
        if (maxDamage <= 0 || damage < 0 || damage >= maxDamage) return new SalvageResolution(false, 0, "item_too_damaged");
        var yield = (int) Math.floor(rule.maximumQuantity() * (double) (maxDamage - damage) / maxDamage);
        var scrapRank = DefaultAbilityCatalog.all().getOrDefault(SALVAGE, java.util.List.of()).stream()
                .filter(value -> value.id().equals("salvage.scrap_collector"))
                .mapToInt(value -> value.rankForLevel(level)).findFirst().orElse(0);
        var salvageLimit = scrapRank == 1 ? 1 : scrapRank * 2;
        yield = Math.min(yield, salvageLimit);
        return yield > 0 ? new SalvageResolution(true, yield, "accepted") : new SalvageResolution(false, 0, "item_too_damaged");
    }

    public int arcaneSalvageLevel(int rank, int enchantmentLevel, DoubleSupplier random) {
        if (rank <= 0 || enchantmentLevel <= 0) return 0;
        var full = new double[]{0, 2.5, 5, 7.5, 10, 12.5, 17.5, 25, 32.5}[Math.min(8, rank)];
        var partial = new double[]{0, 2, 2.5, 5, 7.5, 10, 12.5, 15, 17.5}[Math.min(8, rank)];
        var roll = random.getAsDouble() * 100;
        var maxLevel = (int) formulas.value("salvage.arcane_salvage_max_enchant");
        if (formulas.value("salvage.arcane_salvage_enchant_loss_enabled") <= 0 || roll < full) return Math.min(maxLevel, enchantmentLevel);
        if (roll < full + partial && formulas.value("salvage.arcane_salvage_downgrade_enabled") > 0 && enchantmentLevel > 1) {
            return Math.min(maxLevel, enchantmentLevel - 1);
        }
        return 0;
    }
}
