package com.erodev.sodiumrelief.shader;

import java.util.Optional;

public interface ShaderBackend {
    String name();

    boolean available();

    String unavailableReason();

    Optional<ActiveShaderState> queryActiveState();

    ApplyOutcome apply(ShaderPackSelection selection);

    record ApplyOutcome(boolean accepted, String failureReason) {
        public static ApplyOutcome success() {
            return new ApplyOutcome(true, "");
        }

        public static ApplyOutcome failure(String failureReason) {
            return new ApplyOutcome(false, failureReason);
        }
    }
}
