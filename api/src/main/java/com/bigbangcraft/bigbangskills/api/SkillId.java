package com.bigbangcraft.bigbangskills.api;

import java.util.Objects;
import java.util.Locale;
import java.util.regex.Pattern;

public record SkillId(String namespace, String path) {
    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");

    public SkillId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!PART.matcher(namespace).matches() || !PART.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid skill id: " + namespace + ":" + path);
        }
    }

    public static SkillId parse(String id) {
        var parts = Objects.requireNonNull(id, "id").split(":", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Skill id must be namespace:path");
        return new SkillId(parts[0], parts[1]);
    }

    public static SkillId parseUserInput(String id) {
        var normalized = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("woodcuting") || normalized.equals("woodcut")) normalized = "woodcutting";
        return parse(normalized.contains(":") ? normalized : "bigbangskills:" + normalized);
    }

    @Override public String toString() { return namespace + ":" + path; }
}
