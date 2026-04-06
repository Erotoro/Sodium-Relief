package com.erodev.sodiumrelief.resourcepack;

import java.util.List;
import java.util.Objects;

public record ActiveResourcePackState(List<String> enabledPackIds) {
    public ActiveResourcePackState {
        enabledPackIds = List.copyOf(
            Objects.requireNonNull(enabledPackIds, "enabledPackIds").stream()
                .map(ActiveResourcePackState::sanitize)
                .filter(id -> !id.isBlank())
                .toList()
        );
    }

    public boolean matches(ResourcePackSelection selection) {
        return enabledPackIds.equals(selection.enabledPackIds());
    }

    public static ActiveResourcePackState empty() {
        return new ActiveResourcePackState(List.of());
    }

    private static String sanitize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
