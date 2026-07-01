package com.erodev.sodiumrelief.cache;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.TooltipFingerprint;
import java.util.List;
import net.minecraft.text.Text;

public final class TooltipLayoutCache {
    private final ReliefMetrics metrics;
    private final BoundedLruCache<TooltipFingerprint, CachedTooltipLayout> cache = new BoundedLruCache<>(256, 30_000L);
    private boolean enabled = true;

    public TooltipLayoutCache(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public CachedTooltipLayout get(TooltipFingerprint fingerprint, long nowMillis) {
        if (!enabled) {
            return null;
        }
        CachedTooltipLayout cachedLayout = cache.get(fingerprint, nowMillis);
        if (cachedLayout != null) {
            metrics.tooltipHit();
        } else {
            metrics.tooltipMiss();
        }
        return cachedLayout;
    }

    public void put(TooltipFingerprint fingerprint, CachedTooltipLayout layout, long nowMillis) {
        if (!enabled) {
            return;
        }
        cache.put(fingerprint, layout, nowMillis);
    }

    public void clear() {
        if (cache.size() <= 0) {
            return;
        }
        cache.clear();
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableTooltipLayoutCache;
        cache.reconfigure(config.tooltipCacheSize, config.tooltipCacheTtlMs);
        if (!enabled) {
            clear();
        }
    }

    public int size() {
        return cache.size();
    }

    public record CachedTooltipLayout(List<Text> lines) {
    }
}
