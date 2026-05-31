package com.erodev.sodiumrelief.debug;

import com.erodev.sodiumrelief.client.SodiumReliefConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReliefLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SodiumReliefConstants.MOD_NAME);

    private ReliefLogger() {
    }

    public static void info(String message) {
        LOGGER.info("[{}] {}", SodiumReliefConstants.MOD_ID, message);
    }

    public static void warn(String message) {
        LOGGER.warn("[{}] {}", SodiumReliefConstants.MOD_ID, message);
    }

    public static void warn(String message, Throwable throwable) {
        LOGGER.warn("[{}] {}", SodiumReliefConstants.MOD_ID, message, throwable);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error("[{}] {}", SodiumReliefConstants.MOD_ID, message, throwable);
    }

    public static void debug(boolean enabled, String message) {
        if (enabled) {
            LOGGER.info("[{}] {}", SodiumReliefConstants.MOD_ID, message);
        }
    }

    public static void debug(boolean enabled, String message, Throwable throwable) {
        if (enabled) {
            LOGGER.info("[{}] {}", SodiumReliefConstants.MOD_ID, message, throwable);
        }
    }
}
