package com.erodev.sodiumrelief.cache;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.util.SafeTime;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public final class TextMeasurementCache {
    private final ReliefMetrics metrics;
    private final BoundedLruCache<String, Integer> widthCache = new BoundedLruCache<>(512, 60_000L);
    private final BoundedLruCache<WrapKey, List<OrderedText>> wrapCache = new BoundedLruCache<>(256, 60_000L);
    private boolean enabled = true;

    public TextMeasurementCache(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableTextMeasurementCache;
        widthCache.reconfigure(config.textCacheSize, 60_000L);
        wrapCache.reconfigure(Math.max(64, config.textCacheSize / 2), 60_000L);
        if (!enabled) {
            clear();
        }
    }

    public int measure(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return 0;
        }
        if (!enabled || !isPlainCacheable(text)) {
            return client.textRenderer.getWidth(text);
        }
        return measure(text.getString());
    }

    public int measure(String value) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return 0;
        }
        if (!enabled) {
            return client.textRenderer.getWidth(value);
        }

        return widthCache.get(value, SafeTime.nowMillis()).map(width -> {
            metrics.textHit();
            return width;
        }).orElseGet(() -> {
            metrics.textMiss();
            int width = client.textRenderer.getWidth(value);
            widthCache.put(value, width, SafeTime.nowMillis());
            return width;
        });
    }

    public List<OrderedText> wrap(Text text, int maxWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return List.of();
        }
        if (!enabled) {
            return List.copyOf(client.textRenderer.wrapLines(text, maxWidth));
        }
        if (!isPlainCacheable(text)) {
            return List.copyOf(client.textRenderer.wrapLines(text, maxWidth));
        }

        WrapKey key = new WrapKey(text.getString(), maxWidth);
        return wrapCache.get(key, SafeTime.nowMillis()).map(lines -> {
            metrics.textHit();
            return lines;
        }).orElseGet(() -> {
            metrics.textMiss();
            List<OrderedText> lines = List.copyOf(client.textRenderer.wrapLines(text, maxWidth));
            wrapCache.put(key, lines, SafeTime.nowMillis());
            return lines;
        });
    }

    public void clear() {
        widthCache.clear();
        wrapCache.clear();
    }

    public int size() {
        return widthCache.size() + wrapCache.size();
    }

    private static boolean isPlainCacheable(Text text) {
        return text.getSiblings().isEmpty() && text.getStyle().isEmpty();
    }

    private record WrapKey(String value, int maxWidth) {
    }
}
