package com.erodev.sodiumrelief.performance;

public record FrameSample(long frameNanos, long frameMicros, double frameMillis, double averageMillis, double worstMillis) {
}
