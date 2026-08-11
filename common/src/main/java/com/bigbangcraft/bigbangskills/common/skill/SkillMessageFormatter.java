package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.ability.AbilityDefinition;
import com.bigbangcraft.bigbangskills.common.ability.DefaultAbilityCatalog;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public final class SkillMessageFormatter {
    private SkillMessageFormatter() {}

    public static List<String> overview(PlayerProgress progress, SkillRegistry registry) { return overview(progress, registry, Locale.US); }

    public static List<String> overview(PlayerProgress progress, SkillRegistry registry, Locale locale) {
        var lines = new ArrayList<String>();
        lines.add(SkillMessages.text("overview.title", locale));
        lines.add(SkillMessages.text("overview.power", locale, progress.skills().values().stream().filter(state -> !SkillRelationships.isChild(state.skillId())).mapToInt(SkillProgress::level).sum()));
        registry.snapshot().keySet().stream().sorted((a, b) -> a.toString().compareTo(b.toString())).forEach(id -> {
            var state = progress.get(id);
            if (state != null) lines.add(SkillMessages.text("overview.skill", locale, name(registry.get(id).orElseThrow()), state.level(), number(state.totalXp()), number(nextLevelXp(registry.get(id).orElseThrow(), state))));
        });
        return List.copyOf(lines);
    }

    public static List<String> skill(PlayerProgress progress, SkillRegistry registry, String requested) { return skill(progress, registry, requested, Locale.US); }

    public static List<String> skill(PlayerProgress progress, SkillRegistry registry, String requested, Locale locale) {
        return skill(progress, registry, requested, locale, com.bigbangcraft.bigbangskills.common.config.SkillConfig.defaults());
    }

    public static List<String> skill(PlayerProgress progress, SkillRegistry registry, String requested, Locale locale,
                                     com.bigbangcraft.bigbangskills.common.config.SkillConfig config) {
        var id = registry.snapshot().keySet().stream().filter(skill -> skill.path().equalsIgnoreCase(requested) || skill.toString().equalsIgnoreCase(requested)).findFirst().orElse(null);
        if (id == null || progress.get(id) == null) return List.of(SkillMessages.text("skill.not_found", locale, requested));
        var definition = registry.get(id).orElseThrow();
        var state = progress.get(id);
        var next = state.level() >= definition.maxLevel() ? BigDecimal.ZERO : definition.curve().totalXpForLevel(state.level() + 1);
        var current = state.totalXp().subtract(definition.curve().totalXpForLevel(state.level())).max(BigDecimal.ZERO);
        var toNext = next.subtract(state.totalXp()).max(BigDecimal.ZERO);
        var abilities = DefaultAbilityCatalog.all().getOrDefault(id, List.of());
        var unlocked = abilities.stream().filter(ability -> ability.unlockLevel() <= state.level()).map(ability -> abilityName(ability, locale)).toList();
        var locked = abilities.stream().filter(ability -> ability.unlockLevel() > state.level()).map(ability -> abilityName(ability, locale) + " (" + ability.unlockLevel() + ")").toList();
        var cooldowns = abilities.stream().filter(ability -> ability.type() == com.bigbangcraft.bigbangskills.common.ability.AbilityType.ACTIVE)
                .map(ability -> abilityName(ability, locale) + "=" + config.rule(id).abilityCooldownSeconds() + "s").toList();
        return List.of(name(definition, locale), SkillMessages.text("skill.level", locale, state.level()), SkillMessages.text("skill.current_xp", locale, number(current)), SkillMessages.text("skill.next_xp", locale, state.level() >= definition.maxLevel() ? SkillMessages.text("skill.max", locale) : number(toNext)), SkillMessages.text("skill.total_xp", locale, number(state.totalXp())), SkillMessages.text("skill.abilities_unlocked", locale, unlocked.isEmpty() ? SkillMessages.text("skill.none", locale) : String.join(", ", unlocked)), SkillMessages.text("skill.abilities_locked", locale, locked.isEmpty() ? SkillMessages.text("skill.none", locale) : String.join(", ", locked)), SkillMessages.text("skill.cooldowns", locale, cooldowns.isEmpty() ? SkillMessages.text("skill.none", locale) : String.join(", ", cooldowns)));
    }

    private static BigDecimal nextLevelXp(SkillDefinition definition, SkillProgress state) {
        return state.level() >= definition.maxLevel() ? state.totalXp() : definition.curve().totalXpForLevel(state.level() + 1);
    }

    private static String name(SkillDefinition definition, Locale locale) { return SkillMessages.text(definition.nameKey(), locale); }
    private static String abilityName(AbilityDefinition ability, Locale locale) {
        var key = "bigbangskills.ability." + ability.id();
        var translated = SkillMessages.text(key, locale);
        return translated.equals(key) ? ability.id().substring(ability.id().indexOf('.') + 1).replace('_', ' ') : translated;
    }
    private static String name(SkillDefinition definition) { return name(definition, Locale.US); }
    private static String number(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
