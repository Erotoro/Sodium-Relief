package com.erodev.sodiumrelief.util;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public record ItemStackFingerprint(int itemRawId, int count, int componentHash) {
    public static ItemStackFingerprint of(ItemStack stack) {
        ComponentChanges changes = stack.getComponentChanges();
        return new ItemStackFingerprint(
            Registries.ITEM.getRawId(stack.getItem()),
            stack.getCount(),
            changes.hashCode()
        );
    }
}
