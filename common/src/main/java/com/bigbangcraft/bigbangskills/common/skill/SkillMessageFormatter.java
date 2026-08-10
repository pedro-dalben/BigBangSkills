package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SkillMessageFormatter {
    private SkillMessageFormatter() {}

    public static List<String> overview(PlayerProgress progress, SkillRegistry registry) {
        var lines = new ArrayList<String>();
        lines.add("BigBangSkills");
        lines.add("Power Level: " + progress.skills().values().stream().mapToInt(SkillProgress::level).sum());
        registry.snapshot().keySet().stream().sorted((a, b) -> a.toString().compareTo(b.toString())).forEach(id -> {
            var state = progress.get(id);
            if (state != null) lines.add(label(id.path()) + ": Level " + state.level() + " | XP " + number(state.totalXp()) + " / " + number(nextLevelXp(registry.get(id).orElseThrow(), state)));
        });
        return List.copyOf(lines);
    }

    public static List<String> skill(PlayerProgress progress, SkillRegistry registry, String requested) {
        var id = registry.snapshot().keySet().stream().filter(skill -> skill.path().equalsIgnoreCase(requested) || skill.toString().equalsIgnoreCase(requested)).findFirst().orElse(null);
        if (id == null || progress.get(id) == null) return List.of("Skill não encontrada: " + requested);
        var definition = registry.get(id).orElseThrow();
        var state = progress.get(id);
        var next = state.level() >= definition.maxLevel() ? BigDecimal.ZERO : definition.curve().totalXpForLevel(state.level() + 1);
        var current = state.totalXp().subtract(definition.curve().totalXpForLevel(state.level())).max(BigDecimal.ZERO);
        var toNext = next.subtract(state.totalXp()).max(BigDecimal.ZERO);
        return List.of(label(id.path()), "Level: " + state.level(), "Current XP: " + number(current), "XP next level: " + (state.level() >= definition.maxLevel() ? "MAX" : number(toNext)), "Total XP: " + number(state.totalXp()), "Abilities unlocked: none", "Cooldown: n/a");
    }

    private static BigDecimal nextLevelXp(SkillDefinition definition, SkillProgress state) {
        return state.level() >= definition.maxLevel() ? state.totalXp() : definition.curve().totalXpForLevel(state.level() + 1);
    }

    private static String label(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static String number(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
