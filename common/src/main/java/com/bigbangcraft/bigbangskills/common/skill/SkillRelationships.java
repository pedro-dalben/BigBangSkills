package com.bigbangcraft.bigbangskills.common.skill;

import com.bigbangcraft.bigbangskills.api.SkillId;

import java.util.List;

/** Functional parent/child relationships from the fixed mcMMO baseline. */
public final class SkillRelationships {
    private static final SkillId SALVAGE = id("salvage");
    private static final SkillId SMELTING = id("smelting");
    private SkillRelationships() {}

    public static boolean isChild(SkillId skill) { return SALVAGE.equals(skill) || SMELTING.equals(skill); }

    public static List<SkillId> parents(SkillId child) {
        if (SALVAGE.equals(child)) return List.of(id("repair"), id("fishing"));
        if (SMELTING.equals(child)) return List.of(id("mining"), id("repair"));
        return List.of();
    }

    private static SkillId id(String path) { return SkillId.parse("bigbangskills:" + path); }
}
