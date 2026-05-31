package com.erodev.sodiumrelief.util;

public final class SafeTime {
    private static final long NANOS_PER_MICRO = 1_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long MICROS_PER_MILLI = 1_000L;

    private SafeTime() {
    }

    public static long nowMillis() {
        return System.nanoTime() / NANOS_PER_MILLI;
    }

    public static long nowNanos() {
        return System.nanoTime();
    }

    public static long nowMicros() {
        return nowNanos() / NANOS_PER_MICRO;
    }

    public static long millisToNanos(long millis) {
        return Math.max(0L, millis) * NANOS_PER_MILLI;
    }

    public static long millisToMicros(long millis) {
        return Math.max(0L, millis) * MICROS_PER_MILLI;
    }

    public static long microsToNanos(long micros) {
        return Math.max(0L, micros) * NANOS_PER_MICRO;
    }

    public static long nanosToMicros(long nanos) {
        return Math.max(0L, nanos) / MICROS_PER_MILLI;
    }
}
