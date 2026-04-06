package com.erodev.sodiumrelief.resourcepack;

public record ResourcePackTransitionSnapshot(
    ResourcePackTransitionState state,
    ResourcePackSelection requestedSelection,
    ActiveResourcePackState activeState,
    String failureReason,
    long stateSinceNanos
) {
    public static ResourcePackTransitionSnapshot idle() {
        return new ResourcePackTransitionSnapshot(ResourcePackTransitionState.IDLE, null, null, "", 0L);
    }
}
