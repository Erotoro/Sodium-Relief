package com.erodev.sodiumrelief.shader;

public interface ShaderApplyGate {
    ShaderApplyGate IMMEDIATE = snapshot -> true;

    boolean canStartApply(ShaderTransitionSnapshot snapshot);
}
