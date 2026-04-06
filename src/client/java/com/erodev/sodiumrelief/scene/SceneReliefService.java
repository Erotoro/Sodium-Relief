package com.erodev.sodiumrelief.scene;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.performance.ExecutionPlan;
import net.minecraft.client.MinecraftClient;

public final class SceneReliefService {
    private final ReliefMetrics metrics;
    private final DenseSceneDetector detector = new DenseSceneDetector();
    private final CameraMotionTracker cameraMotionTracker = new CameraMotionTracker();

    private boolean enabled = true;
    private boolean denseSceneDetectorEnabled = true;
    private boolean cameraMotionReliefEnabled = true;
    private boolean foliageReliefEnabled = true;
    private boolean transparencyReliefEnabled = true;
    private int sceneStabilizationFrames = 4;
    private int stabilizationFramesRemaining;
    private DenseSceneSnapshot snapshot = DenseSceneSnapshot.low();

    public SceneReliefService(ReliefMetrics metrics) {
        this.metrics = metrics;
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableDenseSceneRelief;
        denseSceneDetectorEnabled = config.enableDenseSceneDetector;
        cameraMotionReliefEnabled = config.enableCameraMotionRelief;
        foliageReliefEnabled = config.enableFoliageSceneStabilizer;
        transparencyReliefEnabled = config.enableTransparencySceneRelief;
        sceneStabilizationFrames = Math.max(1, config.sceneStabilizationFrames);
        if (!enabled) {
            reset();
        }
    }

    public void tick(MinecraftClient client, AdaptiveMode mode, ReliefConfig config) {
        if (!enabled || client.player == null || client.world == null) {
            reset();
            return;
        }

        boolean cameraSweep = cameraMotionReliefEnabled && cameraMotionTracker.update(client.player, config, mode);
        if (denseSceneDetectorEnabled) {
            snapshot = detector.sample(client, config, mode);
        } else {
            snapshot = DenseSceneSnapshot.low();
        }

        if (hasRelevantSceneDensity() && cameraSweep) {
            stabilizationFramesRemaining = Math.max(stabilizationFramesRemaining, sceneStabilizationFrames);
        } else if (stabilizationFramesRemaining > 0) {
            stabilizationFramesRemaining--;
        }

        metrics.sceneState(
            snapshot.densityState(),
            snapshot.foliageHeavy(),
            snapshot.transparencyHeavy(),
            cameraSweep,
            stabilizationFramesRemaining > 0,
            stabilizationFramesRemaining,
            false
        );
    }

    public ExecutionPlan adjustExecutionPlan(ExecutionPlan plan, AdaptiveMode mode) {
        if (!enabled || !hasSceneSchedulingPressure()) {
            metrics.sceneBudgetClamped(false);
            return plan;
        }

        double factor = baseClampFactor(mode);

        if (snapshot.foliageHeavy() && foliageReliefEnabled) {
            factor *= switch (mode) {
                case SAFE -> 0.99D;
                case BALANCED -> 0.97D;
                case AGGRESSIVE -> 0.95D;
            };
        }
        if (snapshot.transparencyHeavy() && transparencyReliefEnabled) {
            factor *= switch (mode) {
                case SAFE -> 0.99D;
                case BALANCED -> 0.97D;
                case AGGRESSIVE -> 0.95D;
            };
        }
        if (cameraMotionTracker.isSweepActive()) {
            factor *= switch (mode) {
                case SAFE -> 0.94D;
                case BALANCED -> 0.90D;
                case AGGRESSIVE -> 0.86D;
            };
        }
        if (stabilizationFramesRemaining > 0) {
            factor *= switch (mode) {
                case SAFE -> 0.96D;
                case BALANCED -> 0.92D;
                case AGGRESSIVE -> 0.88D;
            };
        }

        factor = Math.max(0.72D, Math.min(1.0D, factor));
        int adjustedBudget = Math.min(plan.budgetMicros(), Math.max(0, (int) Math.round(plan.budgetMicros() * factor)));
        int adjustedTasks = Math.min(plan.maxTasks(), Math.max(0, (int) Math.round(plan.maxTasks() * factor)));
        boolean clamped = adjustedBudget < plan.budgetMicros() || adjustedTasks < plan.maxTasks();

        if (!clamped) {
            metrics.sceneBudgetClamped(false);
            return plan;
        }

        metrics.sceneWorkDeferred();
        metrics.sceneBudgetClamped(true);
        metrics.sceneState(
            snapshot.densityState(),
            snapshot.foliageHeavy(),
            snapshot.transparencyHeavy(),
            cameraMotionTracker.isSweepActive(),
            stabilizationFramesRemaining > 0,
            stabilizationFramesRemaining,
            true
        );

        return new ExecutionPlan(adjustedBudget, adjustedTasks, plan.shouldDeferNonCriticalWork() || clamped, plan.budgetState());
    }

    public boolean shouldDelayNonCriticalWork() {
        return enabled && (cameraMotionTracker.isSweepActive() || stabilizationFramesRemaining > 0);
    }

    public void reset() {
        snapshot = DenseSceneSnapshot.low();
        cameraMotionTracker.reset();
        stabilizationFramesRemaining = 0;
        metrics.sceneState(SceneDensityState.LOW, false, false, false, false, 0, false);
    }

    private boolean hasSceneSchedulingPressure() {
        if (cameraMotionTracker.isSweepActive() || stabilizationFramesRemaining > 0) {
            return true;
        }

        return switch (snapshot.densityState()) {
            case LOW -> false;
            case MODERATE -> snapshot.foliageHeavy() || snapshot.transparencyHeavy();
            case DENSE -> true;
        };
    }

    private boolean hasRelevantSceneDensity() {
        return switch (snapshot.densityState()) {
            case LOW -> false;
            case MODERATE -> snapshot.foliageHeavy() || snapshot.transparencyHeavy();
            case DENSE -> true;
        };
    }

    private double baseClampFactor(AdaptiveMode mode) {
        return switch (snapshot.densityState()) {
            case LOW -> 1.0D;
            case MODERATE -> switch (mode) {
                case SAFE -> 0.97D;
                case BALANCED -> 0.94D;
                case AGGRESSIVE -> 0.90D;
            };
            case DENSE -> switch (mode) {
                case SAFE -> 0.93D;
                case BALANCED -> 0.88D;
                case AGGRESSIVE -> 0.82D;
            };
        };
    }
}
