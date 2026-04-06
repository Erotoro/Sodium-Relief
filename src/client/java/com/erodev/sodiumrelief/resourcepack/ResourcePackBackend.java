package com.erodev.sodiumrelief.resourcepack;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;

public interface ResourcePackBackend {
    Optional<ActiveResourcePackState> queryActiveState(MinecraftClient client);

    ApplyOutcome apply(MinecraftClient client, ResourcePackSelection selection);

    default ApplyOutcome restore(MinecraftClient client, ActiveResourcePackState state) {
        return ApplyOutcome.failure("Resource-pack restore is unavailable.");
    }

    record ApplyOutcome(boolean accepted, String failureReason, CompletableFuture<Void> completionFuture) {
        public static ApplyOutcome success(CompletableFuture<Void> completionFuture) {
            return new ApplyOutcome(true, "", completionFuture);
        }

        public static ApplyOutcome failure(String failureReason) {
            return new ApplyOutcome(false, failureReason, null);
        }
    }
}
