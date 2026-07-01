package com.erodev.sodiumrelief.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BoundedLruCacheTest {

    @Test
    void returnsStoredValueWithinTtl() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(8, 1_000L);
        cache.put("k", "v", 0L);
        assertEquals("v", cache.get("k", 500L));
    }

    @Test
    void returnsNullForMissingKey() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(8, 1_000L);
        assertNull(cache.get("absent", 0L));
    }

    @Test
    void expiresEntriesPastTtl() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(8, 1_000L);
        cache.put("k", "v", 0L);
        assertNull(cache.get("k", 1_001L), "entry older than the TTL must be treated as a miss");
        // Expired entry is dropped on read, so a later lookup is still a miss.
        assertNull(cache.get("k", 1_002L));
    }

    @Test
    void evictsLeastRecentlyUsedBeyondMaxSize() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2, 10_000L);
        cache.put("a", "1", 0L);
        cache.put("b", "2", 0L);
        // Touch "a" so "b" becomes least-recently-used.
        assertEquals("1", cache.get("a", 0L));
        cache.put("c", "3", 0L);
        assertNull(cache.get("b", 0L), "the least-recently-used entry must be evicted");
        assertEquals("1", cache.get("a", 0L));
        assertEquals("3", cache.get("c", 0L));
        assertEquals(2, cache.size());
    }

    @Test
    void reconfigureShrinksToNewMaxSize() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(4, 10_000L);
        cache.put("a", "1", 0L);
        cache.put("b", "2", 0L);
        cache.put("c", "3", 0L);
        cache.reconfigure(1, 10_000L);
        assertEquals(1, cache.size());
    }
}
