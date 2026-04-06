package com.erodev.sodiumrelief.scene;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class CameraMotionTracker {
    private boolean initialized;
    private float lastYaw;
    private float lastPitch;
    private int sweepFramesRemaining;

    public boolean update(ClientPlayerEntity player, ReliefConfig config, AdaptiveMode mode) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        if (!initialized) {
            initialized = true;
            lastYaw = yaw;
            lastPitch = pitch;
            return false;
        }

        double yawDelta = Math.abs(MathHelper.wrapDegrees(yaw - lastYaw));
        double pitchDelta = Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;

        double weightedDelta = yawDelta + (pitchDelta * 0.75D);
        double threshold = adjustedThreshold(config.cameraSweepThresholdDegrees, mode);

        if (weightedDelta >= threshold) {
            sweepFramesRemaining = Math.max(sweepFramesRemaining, sweepWindow(mode));
        } else if (sweepFramesRemaining > 0) {
            sweepFramesRemaining--;
        }

        return isSweepActive();
    }

    public boolean isSweepActive() {
        return sweepFramesRemaining > 0;
    }

    public void reset() {
        initialized = false;
        sweepFramesRemaining = 0;
    }

    private static double adjustedThreshold(int baseThreshold, AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> baseThreshold + 6.0D;
            case BALANCED -> baseThreshold;
            case AGGRESSIVE -> Math.max(6.0D, baseThreshold - 3.0D);
        };
    }

    private static int sweepWindow(AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> 2;
            case BALANCED -> 3;
            case AGGRESSIVE -> 4;
        };
    }
}
