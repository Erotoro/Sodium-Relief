package com.erodev.sodiumrelief.shader;

import java.util.Objects;

public record ShaderPackSelection(String packName, boolean shadersEnabled) {
    public ShaderPackSelection {
        packName = normalize(packName);
        if (shadersEnabled && packName.isBlank()) {
            throw new IllegalArgumentException("Shader pack name must be present when shaders are enabled");
        }
    }

    public static ShaderPackSelection enable(String packName) {
        return new ShaderPackSelection(packName, true);
    }

    public static ShaderPackSelection disable() {
        return new ShaderPackSelection("", false);
    }

    public String displayName() {
        return shadersEnabled ? packName : "Off";
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
