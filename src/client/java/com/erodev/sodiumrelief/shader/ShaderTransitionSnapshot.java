package com.erodev.sodiumrelief.shader;

public record ShaderTransitionSnapshot(
    ShaderTransitionState state,
    ShaderPackSelection requestedSelection,
    ActiveShaderState activeSelection,
    String failureReason,
    long stateSinceNanos
) {
    public static ShaderTransitionSnapshot idle() {
        return new ShaderTransitionSnapshot(ShaderTransitionState.IDLE, null, null, "", 0L);
    }
}
