package com.bigbangcraft.bigbangskills.api;

import java.util.Objects;

public record ProgressionScope(Type type, String id) {
    public enum Type { SERVER, NETWORK, WORLD }
    public ProgressionScope {
        Objects.requireNonNull(type, "type");
        if (id == null || id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid scope id");
    }
    public static ProgressionScope server(String id) { return new ProgressionScope(Type.SERVER, id); }
}
