package com.erodev.sodiumrelief.cache;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.SafeTime;
import com.erodev.sodiumrelief.util.TooltipFingerprint;
import java.util.List;
import java.util.Optional;
import net.minecraft.text.Text;

public final class TooltipLayoutCache {
    private final ReliefMetrics metrics;
    private final BoundedLruCache<TooltipFingerprint, CachedTooltipLayout> cache = new BoundedLruCache<>(256, 1_500L);
    private boolean enabled = true;

    public TooltipLayoutCache(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public Optional<CachedTooltipLayout> get(TooltipFingerprint fingerprint) {
        if (!enabled) {
            return Optional.empty();
        }
        Optional<CachedTooltipLayout> cachedLayout = cache.get(fingerprint, SafeTime.nowMillis());
        cachedLayout.ifPresent(layout -> {
            metrics.tooltipHit();
        });
        if (cachedLayout.isEmpty()) {
            metrics.tooltipMiss();
        }
        return cachedLayout;
    }

    public void put(TooltipFingerprint fingerprint, CachedTooltipLayout layout) {
        if (!enabled) {
            return;
        }
        cache.put(fingerprint, layout, SafeTime.nowMillis());
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
