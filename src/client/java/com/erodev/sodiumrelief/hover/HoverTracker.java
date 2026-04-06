package com.erodev.sodiumrelief.hover;

public final class HoverTracker {
    private Class<?> lastScreenClass;
    private int lastSlotId = Integer.MIN_VALUE;
    private int lastItemRawId = Integer.MIN_VALUE;
    private int lastItemCount = Integer.MIN_VALUE;
    private int lastComponentHash = Integer.MIN_VALUE;

    public boolean matches(Class<?> screenClass, int slotId, int itemRawId, int itemCount, int componentHash) {
        return lastScreenClass == screenClass
            && lastSlotId == slotId
            && lastItemRawId == itemRawId
            && lastItemCount == itemCount
            && lastComponentHash == componentHash;
    }

    public void set(Class<?> screenClass, int slotId, int itemRawId, int itemCount, int componentHash) {
        lastScreenClass = screenClass;
        lastSlotId = slotId;
        lastItemRawId = itemRawId;
        lastItemCount = itemCount;
        lastComponentHash = componentHash;
    }

    public void reset() {
        lastScreenClass = null;
        lastSlotId = Integer.MIN_VALUE;
        lastItemRawId = Integer.MIN_VALUE;
        lastItemCount = Integer.MIN_VALUE;
        lastComponentHash = Integer.MIN_VALUE;
    }
}
