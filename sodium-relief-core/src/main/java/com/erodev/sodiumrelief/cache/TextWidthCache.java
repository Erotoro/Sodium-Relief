package com.erodev.sodiumrelief.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches {@code Font.width(String)} / {@code TextRenderer.getWidth(String)} results.
 *
 * <p>The width of a given string is deterministic for the current font generation, so
 * this is a correctness-safe cache as long as it is cleared whenever font metrics can
 * change. That happens on a client resource reload (which also covers language changes
 * and toggling forced-unicode), and the runtime wires {@link #clear()} into exactly
 * that path. Bounded so memory stays flat.
 *
 * <p>Uses <b>insertion order</b>, not access order: in-game measurement showed this cache's
 * real traffic is exclusively 1-3 character stack-count strings with a tiny distinct working
 * set far below the size cap, so eviction effectively never fires. Access-order would then pay
 * a structural relink on every read (a write-on-read) for an eviction policy that never
 * matters — pure overhead. Insertion order keeps reads to a plain lookup.
 */
public final class TextWidthCache {
    private final LinkedHashMap<String, Integer> entries;
    private int maxSize;
    private boolean enabled = true;

    public TextWidthCache(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.entries = new LinkedHashMap<>(256, 0.75F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > TextWidthCache.this.maxSize;
            }
        };
    }

    public synchronized Integer get(String key) {
        return enabled ? entries.get(key) : null;
    }

    public synchronized void put(String key, int width) {
        if (enabled) {
            entries.put(key, width);
        }
    }

    public synchronized void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void applyConfig(boolean enabled, int maxSize) {
        this.enabled = enabled;
        this.maxSize = Math.max(1, maxSize);
        if (!enabled) {
            entries.clear();
            return;
        }
        while (entries.size() > this.maxSize) {
            entries.remove(entries.keySet().iterator().next());
        }
    }
}
