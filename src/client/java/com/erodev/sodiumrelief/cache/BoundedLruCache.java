package com.erodev.sodiumrelief.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BoundedLruCache<K, V> {
    private final LinkedHashMap<K, Entry<V>> entries;
    private int maxSize;
    private long ttlMs;

    public BoundedLruCache(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(1L, ttlMs);
        this.entries = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Entry<V>> eldest) {
                return size() > BoundedLruCache.this.maxSize;
            }
        };
    }

    public synchronized Optional<V> get(K key, long now) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (now - entry.createdAtMs() > ttlMs) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public synchronized void put(K key, V value, long now) {
        entries.put(key, new Entry<>(value, now));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void reconfigure(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(1L, ttlMs);
        while (entries.size() > this.maxSize) {
            K eldest = entries.keySet().iterator().next();
            entries.remove(eldest);
        }
    }

    private record Entry<V>(V value, long createdAtMs) {
    }
}
