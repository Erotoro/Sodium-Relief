package com.erodev.sodiumrelief.resourcepack;

import java.util.List;
import java.util.Objects;

public record ResourcePackSelection(List<String> enabledPackIds) {
    public ResourcePackSelection {
        enabledPackIds = List.copyOf(
            Objects.requireNonNull(enabledPackIds, "enabledPackIds").stream()
                .map(ResourcePackSelection::sanitize)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList()
        );
    }

    public static ResourcePackSelection of(List<String> enabledPackIds) {
        return new ResourcePackSelection(enabledPackIds);
    }

    public boolean isEmpty() {
        return enabledPackIds.isEmpty();
    }

    public String displayName() {
        return enabledPackIds.isEmpty() ? "Default" : String.join(", ", enabledPackIds);
    }

    private static String sanitize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
