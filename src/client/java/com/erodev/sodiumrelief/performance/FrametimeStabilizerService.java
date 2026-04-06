package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import com.erodev.sodiumrelief.scene.SceneReliefService;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;

public final class FrametimeStabilizerService {
    private final DeferredWorkQueue workQueue;
    private final FrametimeTracker frametimeTracker;
    private final FrameBudgetManager frameBudgetManager;
    private final MicroStutterDetector microStutterDetector;
    private final WorkDistributionSystem workDistributionSystem;
    private final RefreshRateResolver refreshRateResolver;
    private final TaskCostEstimator taskCostEstimator;
    private final SceneReliefService sceneReliefService;
    private final ReliefMetrics metrics;

    private boolean enabled = true;
    private boolean taskSchedulerEnabled = true;
    private boolean framePacingEnabled = true;
    private int budgetMicros = 1_500;
    private int maxTasksPerTick = 4;
    private AdaptiveMode adaptiveMode = AdaptiveMode.BALANCED;
    private ReliefConfig config;

    public FrametimeStabilizerService(
        DeferredWorkQueue workQueue,
        FrametimeTracker frametimeTracker,
        FrameBudgetManager frameBudgetManager,
        MicroStutterDetector microStutterDetector,
        WorkDistributionSystem workDistributionSystem,
        RefreshRateResolver refreshRateResolver,
        TaskCostEstimator taskCostEstimator,
        SceneReliefService sceneReliefService,
        ReliefMetrics metrics
    ) {
        this.workQueue = workQueue;
        this.frametimeTracker = frametimeTracker;
        this.frameBudgetManager = frameBudgetManager;
        this.microStutterDetector = microStutterDetector;
        this.workDistributionSystem = workDistributionSystem;
        this.refreshRateResolver = refreshRateResolver;
        this.taskCostEstimator = taskCostEstimator;
        this.sceneReliefService = sceneReliefService;
        this.metrics = metrics;
    }

    public void applyConfig(ReliefConfig config) {
        this.config = config;
        enabled = config.enableMod && config.enableFrametimeStabilizer;
        taskSchedulerEnabled = config.enableTaskScheduler;
        framePacingEnabled = config.enableFramePacingStabilizer;
        budgetMicros = config.nonCriticalWorkBudgetMicros;
        maxTasksPerTick = config.maxDeferredTasksPerTick;
        adaptiveMode = Objects.requireNonNull(config.adaptiveMode);

        refreshRateResolver.applyConfig(config);
        frameBudgetManager.applyConfig(config, refreshRateResolver.resolvedRefreshRate());
        microStutterDetector.applyConfig(config);
        workDistributionSystem.applyConfig(config);
        sceneReliefService.applyConfig(config);
        metrics.refreshState(config.refreshMode, refreshRateResolver.resolvedRefreshRate(), refreshRateResolver.autoFallbackActive());
        updateTaskCostMetrics();
        if (!canDeferWork() && workQueue.size() > 0) {
            workQueue.drain(new ExecutionPlan(Integer.MAX_VALUE, Integer.MAX_VALUE, false, BudgetState.UNDER));
        }
    }

    public void scheduleNonCritical(Runnable task) {
        scheduleNonCritical(TaskType.UI_MAINTENANCE, TaskPriority.NORMAL, task);
    }

    public void scheduleNonCritical(Runnable task, int estimatedCostMicros) {
        scheduleNonCritical(TaskType.UI_MAINTENANCE, TaskPriority.NORMAL, task, estimatedCostMicros);
    }

    public void scheduleNonCritical(TaskType type, TaskPriority priority, Runnable task) {
        scheduleNonCritical(type, priority, task, taskCostEstimator.estimate(type));
    }

    public void scheduleNonCritical(TaskType type, TaskPriority priority, Runnable task, int estimatedCostMicros) {
        scheduleNonCritical(type, priority, task, estimatedCostMicros, null);
    }

    public void scheduleNonCritical(TaskType type, TaskPriority priority, Runnable task, int estimatedCostMicros, String coalescingKey) {
        if (canDeferWork()) {
            workQueue.enqueue(new DeferredTask(task, type, priority, Math.max(1, estimatedCostMicros), coalescingKey));
        } else {
            executeSafely(type, priority, task, coalescingKey);
        }
    }

    public void executeCritical(TaskType type, TaskPriority priority, Runnable task, int estimatedCostMicros, String coalescingKey) {
        if (type.correctnessCritical()) {
            executeSafely(type, priority, task, coalescingKey);
            return;
        }
        scheduleNonCritical(type, priority, task, estimatedCostMicros, coalescingKey);
    }

    public void tick(MinecraftClient client) {
        if (config == null) {
            return;
        }
        if (refreshRateResolver.refreshIfNeeded(client)) {
            frameBudgetManager.applyConfig(config, refreshRateResolver.resolvedRefreshRate());
            metrics.refreshState(config.refreshMode, refreshRateResolver.resolvedRefreshRate(), refreshRateResolver.autoFallbackActive());
        }
        sceneReliefService.tick(client, adaptiveMode, config);
    }

    public void onRenderFrame() {
        FrameSample sample = frametimeTracker.recordFrame();
        frameBudgetManager.observe(sample);
        microStutterDetector.observe(sample, frameBudgetManager.effectiveFrameBudgetMicros());

        if (frameBudgetManager.isOverBudget()) {
            metrics.overBudgetFrame();
        }

        if (!enabled || !taskSchedulerEnabled) {
            updateTaskCostMetrics();
            return;
        }

        ExecutionPlan plan;
        if (framePacingEnabled) {
            plan = frameBudgetManager.createPlan(
                budgetMicros,
                maxTasksPerTick,
                adaptiveMode,
                microStutterDetector.isStabilizing(),
                true
            );

            plan = workDistributionSystem.distribute(plan, workQueue.size(), adaptiveMode);
            plan = sceneReliefService.adjustExecutionPlan(plan, adaptiveMode);
        } else {
            plan = new ExecutionPlan(budgetMicros, maxTasksPerTick, false, frameBudgetManager.currentBudgetState());
        }
        workQueue.drain(plan);
        updateTaskCostMetrics();
    }

    public boolean shouldDelayNonCriticalUiWork() {
        return enabled && (
            frameBudgetManager.currentBudgetState() != BudgetState.UNDER
                || microStutterDetector.isStabilizing()
                || sceneReliefService.shouldDelayNonCriticalWork()
        );
    }

    public int estimatedFps() {
        return frametimeTracker.estimatedFps();
    }

    public int pendingTasks() {
        return workQueue.size();
    }

    public AdaptiveMode adaptiveMode() {
        return adaptiveMode;
    }

    public boolean isStabilizing() {
        return microStutterDetector.isStabilizing();
    }

    public int stabilizationFramesRemaining() {
        return microStutterDetector.stabilizationFramesRemaining();
    }

    public int targetRefreshRate() {
        return refreshRateResolver.resolvedRefreshRate();
    }

    public boolean refreshFallbackActive() {
        return refreshRateResolver.autoFallbackActive();
    }

    public boolean canDeferWork() {
        return enabled && taskSchedulerEnabled && framePacingEnabled;
    }

    private void executeSafely(TaskType type, TaskPriority priority, Runnable task, String coalescingKey) {
        try {
            task.run();
        } catch (Throwable throwable) {
            metrics.deferredFailure();
            ReliefLogger.warn(
                "Task failed [type=" + type.name()
                    + ", priority=" + priority.name()
                    + ", key=" + String.valueOf(coalescingKey) + "]",
                throwable
            );
        }
    }

    private void updateTaskCostMetrics() {
        metrics.taskCostState(
            taskCostEstimator.averageMicros(TaskType.TOOLTIP_LAYOUT),
            taskCostEstimator.averageMicros(TaskType.TEXT_LAYOUT),
            taskCostEstimator.averageMicros(TaskType.CACHE_INVALIDATION)
        );
    }
}
