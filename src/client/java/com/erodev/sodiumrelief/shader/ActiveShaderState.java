package com.erodev.sodiumrelief.shader;

import java.util.Objects;

public record ActiveShaderState(String packName, boolean shadersEnabled) {
    public ActiveShaderState {
        packName = normalize(packName);
    }

    public static ActiveShaderState disabled() {
        return new ActiveShaderState("", false);
    }

    public boolean matches(ShaderPackSelection selection) {
        return shadersEnabled == selection.shadersEnabled() && canonical(packName).equals(canonical(selection.packName()));
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private static String canonical(String value) {
        String normalized = normalize(value).replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.endsWith(".zip")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
