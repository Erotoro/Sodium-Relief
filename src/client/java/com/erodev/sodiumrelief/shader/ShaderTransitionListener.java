package com.erodev.sodiumrelief.shader;

public interface ShaderTransitionListener {
    ShaderTransitionListener NO_OP = new ShaderTransitionListener() {
    };

    default void onApplying(ShaderTransitionSnapshot snapshot) {
    }

    default void onResolved(ShaderTransitionSnapshot snapshot) {
    }
}
