package com.erodev.sodiumrelief.compat.resourcepack;

import com.erodev.sodiumrelief.resourcepack.ActiveResourcePackState;
import com.erodev.sodiumrelief.resourcepack.ResourcePackBackend;
import com.erodev.sodiumrelief.resourcepack.ResourcePackSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

public final class MinecraftClientResourcePackBackend implements ResourcePackBackend {
    @Override
    @SuppressWarnings("null")
    public Optional<ActiveResourcePackState> queryActiveState(MinecraftClient client) {
        if (client.options == null) {
            return Optional.empty();
        }
        return Optional.of(new ActiveResourcePackState(List.copyOf(client.options.resourcePacks)));
    }

    @Override
    public ApplyOutcome apply(MinecraftClient client, ResourcePackSelection selection) {
        if (client.options == null) {
            return ApplyOutcome.failure("Minecraft client options are not initialized.");
        }

        ResourcePackManager resourcePackManager = client.getResourcePackManager();
        resourcePackManager.scanPacks();
        if (!selection.enabledPackIds().stream().allMatch(resourcePackManager::hasProfile)) {
            return ApplyOutcome.failure("One or more requested resource packs are not available.");
        }

        resourcePackManager.setEnabledProfiles(selection.enabledPackIds());
        client.options.resourcePacks = new ArrayList<>(selection.enabledPackIds());
        client.options.incompatibleResourcePacks = incompatiblePackIds(resourcePackManager);
        client.options.write();

        CompletableFuture<Void> reloadFuture = client.reloadResourcesConcurrently();
        return ApplyOutcome.success(reloadFuture);
    }

    @Override
    public ApplyOutcome restore(MinecraftClient client, ActiveResourcePackState state) {
        if (client.options == null || state == null) {
            return ApplyOutcome.failure("Minecraft client options are not initialized.");
        }

        ResourcePackManager resourcePackManager = client.getResourcePackManager();
        resourcePackManager.scanPacks();
        if (!state.enabledPackIds().stream().allMatch(resourcePackManager::hasProfile)) {
            return ApplyOutcome.failure("One or more baseline resource packs are no longer available.");
        }

        resourcePackManager.setEnabledProfiles(state.enabledPackIds());
        client.options.resourcePacks = new ArrayList<>(state.enabledPackIds());
        client.options.incompatibleResourcePacks = incompatiblePackIds(resourcePackManager);
        client.options.write();
        return ApplyOutcome.success(client.reloadResourcesConcurrently());
    }

    private static List<String> incompatiblePackIds(ResourcePackManager resourcePackManager) {
        return resourcePackManager.getEnabledProfiles().stream()
            .filter(profile -> !profile.getCompatibility().isCompatible())
            .map(profile -> profile.getId().toLowerCase(java.util.Locale.ROOT))
            .toList();
    }
}
