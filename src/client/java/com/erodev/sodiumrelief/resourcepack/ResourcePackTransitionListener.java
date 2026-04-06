package com.erodev.sodiumrelief.resourcepack;

public interface ResourcePackTransitionListener {
    ResourcePackTransitionListener NO_OP = new ResourcePackTransitionListener() {
    };

    default void onApplying(ResourcePackTransitionSnapshot snapshot) {
    }

    default void onResolved(ResourcePackTransitionSnapshot snapshot) {
    }
}
