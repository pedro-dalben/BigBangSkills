package com.bigbangcraft.bigbangskills.common.xp;

import com.bigbangcraft.bigbangskills.common.progression.*;
import com.bigbangcraft.bigbangskills.common.skill.*;
import java.math.*;
import java.util.Comparator;
import java.util.List;

public final class XpService {
    public record Result(boolean accepted, BigDecimal amount, SkillProgress before, SkillProgress after, String reason) {}
    public Result apply(PlayerProgress player, XpRequest request, SkillRegistry registry, List<XpModifier> modifiers) {
        var definition = registry.get(request.skillId()).filter(SkillDefinition::enabled).orElse(null);
        if (definition == null) return new Result(false, BigDecimal.ZERO, null, null, "skill_disabled_or_unknown");
        var before = player.get(request.skillId());
        if (before == null) before = new SkillProgress(request.skillId(), BigDecimal.ZERO, 1, 0);
        var amount = modifiers.stream().sorted(Comparator.comparingInt(XpModifier::order)).reduce(request.baseAmount(), (value, modifier) -> value.multiply(modifier.multiplier()), BigDecimal::add).setScale(4, RoundingMode.DOWN).stripTrailingZeros();
        if (amount.signum() <= 0) return new Result(false, BigDecimal.ZERO, before, before, "zero_xp");
        var after = before.add(amount, definition.maxLevel(), definition.curve()); player.put(after);
        return new Result(true, amount, before, after, "accepted");
    }
}
