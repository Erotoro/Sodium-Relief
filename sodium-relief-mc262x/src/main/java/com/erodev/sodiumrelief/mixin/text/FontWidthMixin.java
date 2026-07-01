package com.erodev.sodiumrelief.mixin.text;

import com.erodev.sodiumrelief.cache.TextWidthCache;
import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Memoizes {@code Font.width(String)} results. The width of a string is deterministic
 * for the current font generation, and the cache is cleared on every client resource
 * reload (which covers language and forced-unicode changes), so this can never return
 * a stale or wrong width. Targets the repeated per-frame measurement of small strings
 * such as item stack counts across all visible inventory slots.
 */
@Mixin(Font.class)
public abstract class FontWidthMixin {
    @WrapMethod(method = "width(Ljava/lang/String;)I")
    private int sodiumRelief$cacheStringWidth(String text, Operation<Integer> original) {
        SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
        if (runtime == null || text == null || text.isEmpty()) {
            return original.call(text);
        }
        TextWidthCache cache = runtime.textWidthCache();
        boolean detailed = runtime.metrics().detailedMetricsEnabled();
        Integer cached = cache.get(text);
        if (cached != null) {
            if (detailed) {
                runtime.metrics().textWidthCacheHit(text.length());
            }
            return cached;
        }
        if (detailed) {
            runtime.metrics().textWidthCacheMiss(text.length());
        }
        int width = original.call(text);
        cache.put(text, width);
        return width;
    }
}
