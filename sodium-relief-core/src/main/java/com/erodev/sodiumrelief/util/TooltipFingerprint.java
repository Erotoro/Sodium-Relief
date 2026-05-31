package com.erodev.sodiumrelief.util;

import java.util.Objects;

public final class TooltipFingerprint {
    private final Class<?> screenClass;
    private final int slotId;
    private final int itemRawId;
    private final int itemCount;
    private final int componentHash;
    private final boolean advancedTooltips;
    private final String languageCode;
    private final int hashCode;

    public TooltipFingerprint(
        Class<?> screenClass,
        int slotId,
        int itemRawId,
        int itemCount,
        int componentHash,
        boolean advancedTooltips,
        String languageCode
    ) {
        this.screenClass = Objects.requireNonNull(screenClass, "screenClass");
        this.slotId = slotId;
        this.itemRawId = itemRawId;
        this.itemCount = itemCount;
        this.componentHash = componentHash;
        this.advancedTooltips = advancedTooltips;
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode");
        this.hashCode = computeHashCode();
    }

    public Class<?> screenClass() {
        return screenClass;
    }

    public int slotId() {
        return slotId;
    }

    public int itemRawId() {
        return itemRawId;
    }

    public int itemCount() {
        return itemCount;
    }

    public int componentHash() {
        return componentHash;
    }

    public boolean advancedTooltips() {
        return advancedTooltips;
    }

    public String languageCode() {
        return languageCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TooltipFingerprint that)) {
            return false;
        }
        return slotId == that.slotId
            && itemRawId == that.itemRawId
            && itemCount == that.itemCount
            && componentHash == that.componentHash
            && advancedTooltips == that.advancedTooltips
            && screenClass == that.screenClass
            && languageCode.equals(that.languageCode);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        int result = System.identityHashCode(screenClass);
        result = 31 * result + slotId;
        result = 31 * result + itemRawId;
        result = 31 * result + itemCount;
        result = 31 * result + componentHash;
        result = 31 * result + (advancedTooltips ? 1 : 0);
        result = 31 * result + languageCode.hashCode();
        return result;
    }
}
