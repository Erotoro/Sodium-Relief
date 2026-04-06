package com.erodev.sodiumrelief.performance;

import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import java.util.HashMap;
import java.util.Map;

public final class DeferredWorkQueue {
    private static final int MAX_TASKS = 128;
    private static final int PRIORITY_COUNT = TaskPriority.values().length;
    private static final int COST_CLASS_COUNT = 3;
    private static final int COST_CHEAP_THRESHOLD_MICROS = 250;
    private static final int COST_MEDIUM_THRESHOLD_MICROS = 900;
    private static final int STARVATION_RELIEF_THRESHOLD = 3;
    private static final int[] NON_CRITICAL_EXECUTION_ORDER = {
        nonCriticalBucketIndex(TaskPriority.HIGH, 0),
        nonCriticalBucketIndex(TaskPriority.HIGH, 1),
        nonCriticalBucketIndex(TaskPriority.HIGH, 2),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 0),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 1),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 2),
        nonCriticalBucketIndex(TaskPriority.LOW, 0),
        nonCriticalBucketIndex(TaskPriority.LOW, 1),
        nonCriticalBucketIndex(TaskPriority.LOW, 2)
    };
    private static final int[] NON_CRITICAL_EVICTION_ORDER = {
        nonCriticalBucketIndex(TaskPriority.LOW, 2),
        nonCriticalBucketIndex(TaskPriority.LOW, 1),
        nonCriticalBucketIndex(TaskPriority.LOW, 0),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 2),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 1),
        nonCriticalBucketIndex(TaskPriority.NORMAL, 0),
        nonCriticalBucketIndex(TaskPriority.HIGH, 2),
        nonCriticalBucketIndex(TaskPriority.HIGH, 1),
        nonCriticalBucketIndex(TaskPriority.HIGH, 0)
    };

    private final ReliefMetrics metrics;
    private final TaskCostEstimator costEstimator;
    private final Bucket[] criticalBuckets = createBuckets(PRIORITY_COUNT);
    private final Bucket[] nonCriticalBuckets = createBuckets(PRIORITY_COUNT * COST_CLASS_COUNT);
    private final Map<String, QueuedTask> coalescedTasks = new HashMap<>();
    private int queuedTasks;
    private int blockedDrainPasses;

    public DeferredWorkQueue(ReliefMetrics metrics, TaskCostEstimator costEstimator) {
        this.metrics = metrics;
        this.costEstimator = costEstimator;
    }

    public synchronized void enqueue(DeferredTask task) {
        QueuedTask existing = findCoalesced(task);
        if (existing != null) {
            replaceCoalesced(existing, task);
            metrics.taskRescheduled();
            return;
        }

        if (queuedTasks >= MAX_TASKS && !evictFor(task)) {
            metrics.deferredSkipped();
            return;
        }

        QueuedTask queuedTask = new QueuedTask(task);
        enqueueNode(queuedTask);
        metrics.deferredQueued();
    }

    public synchronized int size() {
        return queuedTasks;
    }

    public void drain(ExecutionPlan plan) {
        int maxTasks = Math.max(0, plan.maxTasks());
        int initialBudgetMicros = Math.max(0, plan.budgetMicros());
        if (maxTasks <= 0 && !hasCriticalTasks()) {
            noteBlockedDrain();
            metrics.deferredSkipped();
            return;
        }

        int executed = 0;
        int spentMicros = 0;
        boolean progressed = false;
        int executionLimit = Math.max(1, maxTasks);

        while (executed < executionLimit) {
            int remainingBudgetMicros = Math.max(0, initialBudgetMicros - spentMicros);
            QueuedTask queuedTask = pollNextTask(remainingBudgetMicros, plan.shouldDeferNonCriticalWork());
            if (queuedTask == null) {
                break;
            }

            progressed = true;
            long startNanos = System.nanoTime();
            long observedMicros;
            try {
                queuedTask.task.action().run();
                observedMicros = elapsedMicros(startNanos);
                metrics.deferredRun();
            } catch (Throwable throwable) {
                observedMicros = elapsedMicros(startNanos);
                metrics.deferredFailure();
                ReliefLogger.warn(
                    "Deferred task failed [type=" + queuedTask.task.type().name()
                        + ", priority=" + queuedTask.task.priority().name()
                        + ", key=" + String.valueOf(queuedTask.task.coalescingKey()) + "]",
                    throwable
                );
            }

            costEstimator.record(queuedTask.task.type(), observedMicros);
            executed++;
            spentMicros += Math.max(queuedTask.task.estimatedCostMicros(), (int) observedMicros);

            if (spentMicros >= initialBudgetMicros && !queuedTask.task.correctnessCritical()) {
                break;
            }
        }

        if (progressed || size() == 0) {
            resetBlockedDrainPasses();
            return;
        }

        noteBlockedDrain();
        metrics.deferredSkipped();
    }

    private synchronized boolean hasCriticalTasks() {
        for (Bucket bucket : criticalBuckets) {
            if (!bucket.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private synchronized QueuedTask findCoalesced(DeferredTask task) {
        if (!task.isCoalescable()) {
            return null;
        }
        return coalescedTasks.get(task.coalescingKey());
    }

    private synchronized void replaceCoalesced(QueuedTask existing, DeferredTask replacement) {
        Bucket targetBucket = bucketFor(replacement);
        untrackCoalescingKey(existing);
        existing.task = replacement;
        if (existing.bucket != targetBucket) {
            existing.bucket.remove(existing);
            targetBucket.addLast(existing);
            existing.bucket = targetBucket;
        }
        trackCoalescingKey(existing);
    }

    private synchronized boolean evictFor(DeferredTask incoming) {
        QueuedTask evicted = pollWorstNonCritical();
        if (evicted != null) {
            metrics.taskRescheduled();
            return true;
        }

        if (incoming.correctnessCritical()) {
            return false;
        }

        return false;
    }

    private synchronized void enqueueNode(QueuedTask queuedTask) {
        Bucket bucket = bucketFor(queuedTask.task);
        bucket.addLast(queuedTask);
        queuedTask.bucket = bucket;
        queuedTasks++;
        trackCoalescingKey(queuedTask);
    }

    private synchronized QueuedTask pollNextTask(int remainingBudgetMicros, boolean shouldDeferNonCriticalWork) {
        QueuedTask criticalTask = pollFirstCritical();
        if (criticalTask != null) {
            return criticalTask;
        }

        QueuedTask budgetFit = pollFirstNonCriticalWithinBudget(remainingBudgetMicros);
        if (budgetFit != null) {
            return budgetFit;
        }

        if (!shouldDeferNonCriticalWork || blockedDrainPasses >= STARVATION_RELIEF_THRESHOLD) {
            return pollFirstNonCriticalIgnoringBudget();
        }

        return null;
    }

    private QueuedTask pollFirstCritical() {
        for (int priorityIndex = 0; priorityIndex < criticalBuckets.length; priorityIndex++) {
            QueuedTask queuedTask = pollFirst(criticalBuckets[priorityIndex]);
            if (queuedTask != null) {
                return queuedTask;
            }
        }
        return null;
    }

    private QueuedTask pollFirstNonCriticalWithinBudget(int remainingBudgetMicros) {
        if (remainingBudgetMicros <= 0) {
            return null;
        }

        for (int bucketIndex : NON_CRITICAL_EXECUTION_ORDER) {
            Bucket bucket = nonCriticalBuckets[bucketIndex];
            QueuedTask head = bucket.head;
            if (head != null && head.task.estimatedCostMicros() <= remainingBudgetMicros) {
                return pollFirst(bucket);
            }
        }
        return null;
    }

    private QueuedTask pollFirstNonCriticalIgnoringBudget() {
        for (int bucketIndex : NON_CRITICAL_EXECUTION_ORDER) {
            QueuedTask queuedTask = pollFirst(nonCriticalBuckets[bucketIndex]);
            if (queuedTask != null) {
                return queuedTask;
            }
        }
        return null;
    }

    private synchronized QueuedTask pollWorstNonCritical() {
        for (int bucketIndex : NON_CRITICAL_EVICTION_ORDER) {
            QueuedTask queuedTask = pollFirst(nonCriticalBuckets[bucketIndex]);
            if (queuedTask != null) {
                return queuedTask;
            }
        }
        return null;
    }

    private synchronized QueuedTask pollFirst(Bucket bucket) {
        QueuedTask queuedTask = bucket.pollFirst();
        if (queuedTask == null) {
            return null;
        }
        queuedTask.bucket = null;
        queuedTasks--;
        untrackCoalescingKey(queuedTask);
        return queuedTask;
    }

    private synchronized void trackCoalescingKey(QueuedTask queuedTask) {
        if (queuedTask.task.isCoalescable()) {
            coalescedTasks.put(queuedTask.task.coalescingKey(), queuedTask);
        }
    }

    private synchronized void untrackCoalescingKey(QueuedTask queuedTask) {
        if (queuedTask.task.isCoalescable()) {
            coalescedTasks.remove(queuedTask.task.coalescingKey(), queuedTask);
        }
    }

    private synchronized void noteBlockedDrain() {
        blockedDrainPasses = Math.min(STARVATION_RELIEF_THRESHOLD, blockedDrainPasses + 1);
    }

    private synchronized void resetBlockedDrainPasses() {
        blockedDrainPasses = 0;
    }

    private static long elapsedMicros(long startNanos) {
        return Math.max(1L, (System.nanoTime() - startNanos) / 1_000L);
    }

    private static Bucket[] createBuckets(int size) {
        Bucket[] buckets = new Bucket[size];
        for (int index = 0; index < size; index++) {
            buckets[index] = new Bucket();
        }
        return buckets;
    }

    private static Bucket bucketForPriority(Bucket[] buckets, TaskPriority priority) {
        return buckets[priorityIndex(priority)];
    }

    private static int nonCriticalBucketIndex(TaskPriority priority, int costClass) {
        return (priorityIndex(priority) * COST_CLASS_COUNT) + costClass;
    }

    private static Bucket bucketForNonCritical(Bucket[] buckets, TaskPriority priority, int estimatedCostMicros) {
        return buckets[nonCriticalBucketIndex(priority, costClass(estimatedCostMicros))];
    }

    private static int priorityIndex(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case NORMAL -> 1;
            case LOW -> 2;
        };
    }

    private static int costClass(int estimatedCostMicros) {
        if (estimatedCostMicros <= COST_CHEAP_THRESHOLD_MICROS) {
            return 0;
        }
        if (estimatedCostMicros <= COST_MEDIUM_THRESHOLD_MICROS) {
            return 1;
        }
        return 2;
    }

    private Bucket bucketFor(DeferredTask task) {
        if (task.correctnessCritical()) {
            return bucketForPriority(criticalBuckets, task.priority());
        }
        return bucketForNonCritical(nonCriticalBuckets, task.priority(), task.estimatedCostMicros());
    }

    private static final class Bucket {
        private QueuedTask head;
        private QueuedTask tail;

        private boolean isEmpty() {
            return head == null;
        }

        private void addLast(QueuedTask queuedTask) {
            queuedTask.previous = tail;
            queuedTask.next = null;
            if (tail == null) {
                head = queuedTask;
            } else {
                tail.next = queuedTask;
            }
            tail = queuedTask;
        }

        private QueuedTask pollFirst() {
            QueuedTask queuedTask = head;
            if (queuedTask == null) {
                return null;
            }
            remove(queuedTask);
            return queuedTask;
        }

        private void remove(QueuedTask queuedTask) {
            QueuedTask previous = queuedTask.previous;
            QueuedTask next = queuedTask.next;
            if (previous == null) {
                head = next;
            } else {
                previous.next = next;
            }
            if (next == null) {
                tail = previous;
            } else {
                next.previous = previous;
            }
            queuedTask.previous = null;
            queuedTask.next = null;
        }
    }

    private static final class QueuedTask {
        private DeferredTask task;
        private Bucket bucket;
        private QueuedTask previous;
        private QueuedTask next;

        private QueuedTask(DeferredTask task) {
            this.task = task;
        }
    }
}
