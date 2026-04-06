package com.erodev.sodiumrelief.resourcepack;

public interface ResourcePackApplyGate {
    ResourcePackApplyGate IMMEDIATE = snapshot -> true;

    boolean canStartApply(ResourcePackTransitionSnapshot snapshot);
}
