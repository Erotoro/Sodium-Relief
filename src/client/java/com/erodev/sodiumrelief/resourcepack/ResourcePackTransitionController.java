package com.erodev.sodiumrelief.resourcepack;

import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.SafeTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;

public final class ResourcePackTransitionController {
    private static final long APPLY_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(20L);
    private static final long TERMINAL_VISIBILITY_NANOS = TimeUnit.MILLISECONDS.toNanos(1500L);

    private final ResourcePackBackend backend;
    private final ResourcePackTransitionListener listener;
    private final ResourcePackApplyGate applyGate;
    private final BooleanSupplier debugLoggingEnabled;
    private ResourcePackTransitionSnapshot snapshot = ResourcePackTransitionSnapshot.idle();
    private CompletableFuture<Void> completionFuture;
    private long applyDeadlineNanos;
    private boolean applyStarted;
    private ActiveResourcePackState baselineActiveState;
    private String pendingFailureReason;
    private String pendingDiagnosticReason;
    private boolean recoveryInProgress;

    public ResourcePackTransitionController(ResourcePackBackend backend, BooleanSupplier debugLoggingEnabled) {
        this(backend, ResourcePackTransitionListener.NO_OP, ResourcePackApplyGate.IMMEDIATE, debugLoggingEnabled);
    }

    public ResourcePackTransitionController(
        ResourcePackBackend backend,
        ResourcePackTransitionListener listener,
        ResourcePackApplyGate applyGate,
        BooleanSupplier debugLoggingEnabled
    ) {
        this.backend = Objects.requireNonNull(backend);
        this.listener = Objects.requireNonNull(listener);
        this.applyGate = Objects.requireNonNull(applyGate);
        this.debugLoggingEnabled = Objects.requireNonNull(debugLoggingEnabled);
    }

    public ResourcePackSwitchRequestResult requestSwitch(MinecraftClient client, ResourcePackSelection selection) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(selection, "selection");

        if (snapshot.state() == ResourcePackTransitionState.APPLYING) {
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Ignored resource-pack switch request because another switch is already applying");
            return ResourcePackSwitchRequestResult.ALREADY_APPLYING;
        }

        long nowNanos = SafeTime.nowNanos();
        beginApplying(selection, client, nowNanos);
        maybeStartApply(client, nowNanos);
        if (snapshot.state() == ResourcePackTransitionState.APPLYING && applyStarted) {
            pollCompletion(client, nowNanos);
        }
        return snapshot.state() == ResourcePackTransitionState.FAILED
            ? ResourcePackSwitchRequestResult.FAILED_TO_START
            : ResourcePackSwitchRequestResult.STARTED;
    }

    public void tick(MinecraftClient client) {
        if (snapshot.state() == ResourcePackTransitionState.APPLYING) {
            long nowNanos = SafeTime.nowNanos();
            if (recoveryInProgress) {
                pollRecovery(client, nowNanos);
            } else {
                maybeStartApply(client, nowNanos);
                if (applyStarted) {
                    pollCompletion(client, nowNanos);
                } else if (nowNanos >= applyDeadlineNanos) {
                    finalizeFailure("Resource pack switch timed out.", "Resource-pack switch did not begin before the transition deadline elapsed.", nowNanos, baselineActiveState);
                }
            }

            if (snapshot.state() == ResourcePackTransitionState.APPLYING && nowNanos >= applyDeadlineNanos) {
                if (recoveryInProgress) {
                    finalizeFailure(
                        pendingFailureReason == null ? "Resource pack switch failed." : pendingFailureReason,
                        combineDiagnostics(pendingDiagnosticReason, "Resource-pack rollback did not complete before the recovery deadline elapsed."),
                        nowNanos,
                        baselineActiveState
                    );
                } else {
                    finalizeFailure("Resource pack switch timed out.", "Resource-pack reload did not complete before the transition deadline elapsed.", nowNanos, baselineActiveState);
                }
            }
            return;
        }

        if ((snapshot.state() == ResourcePackTransitionState.SUCCESS || snapshot.state() == ResourcePackTransitionState.FAILED)
            && SafeTime.nowNanos() - snapshot.stateSinceNanos() >= TERMINAL_VISIBILITY_NANOS) {
            resetToIdle();
        }
    }

    public ResourcePackTransitionSnapshot snapshot() {
        return snapshot;
    }

    private void beginApplying(ResourcePackSelection selection, MinecraftClient client, long nowNanos) {
        baselineActiveState = backend.queryActiveState(client).orElse(snapshot.activeState());
        snapshot = new ResourcePackTransitionSnapshot(
            ResourcePackTransitionState.APPLYING,
            selection,
            baselineActiveState,
            "",
            nowNanos
        );
        completionFuture = null;
        applyDeadlineNanos = nowNanos + APPLY_TIMEOUT_NANOS;
        applyStarted = false;
        pendingFailureReason = null;
        pendingDiagnosticReason = null;
        recoveryInProgress = false;
        notifyApplying(snapshot);
    }

    private void pollCompletion(MinecraftClient client, long nowNanos) {
        if (completionFuture == null || !completionFuture.isDone()) {
            return;
        }

        try {
            completionFuture.join();
        } catch (CancellationException exception) {
            beginFailureRecovery(client, "Resource pack switch failed.", "Resource-pack reload was cancelled.", nowNanos);
            return;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            beginFailureRecovery(client, "Resource pack switch failed.", cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage(), nowNanos);
            return;
        }

        Optional<ActiveResourcePackState> activeState = backend.queryActiveState(client);
        if (snapshot.requestedSelection() != null && activeState.filter(state -> state.matches(snapshot.requestedSelection())).isPresent()) {
            succeed(activeState.orElse(null), nowNanos);
            return;
        }

        beginFailureRecovery(client, "Resource pack switch failed.", "Reload completed but the active resource-pack state did not match the requested selection.", nowNanos);
    }

    private void succeed(ActiveResourcePackState activeState, long nowNanos) {
        snapshot = new ResourcePackTransitionSnapshot(ResourcePackTransitionState.SUCCESS, snapshot.requestedSelection(), activeState, "", nowNanos);
        completionFuture = null;
        applyStarted = false;
        baselineActiveState = activeState;
        pendingFailureReason = null;
        pendingDiagnosticReason = null;
        recoveryInProgress = false;
        notifyResolved(snapshot);
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Resource-pack switch completed: " + (activeState == null ? "unknown" : activeState.enabledPackIds()));
    }

    private void finalizeFailure(String publicReason, String diagnosticReason, long nowNanos, ActiveResourcePackState stableState) {
        snapshot = new ResourcePackTransitionSnapshot(
            ResourcePackTransitionState.FAILED,
            snapshot.requestedSelection(),
            stableState == null ? baselineActiveState : stableState,
            publicReason,
            nowNanos
        );
        completionFuture = null;
        applyStarted = false;
        pendingFailureReason = null;
        pendingDiagnosticReason = null;
        recoveryInProgress = false;
        notifyResolved(snapshot);
        ReliefLogger.warn("Resource pack switch failed");
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Resource-pack switch failure details: " + diagnosticReason);
    }

    private void maybeStartApply(MinecraftClient client, long nowNanos) {
        if (applyStarted || snapshot.state() != ResourcePackTransitionState.APPLYING || !applyGate.canStartApply(snapshot)) {
            return;
        }

        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Starting resource-pack switch: " + snapshot.requestedSelection().displayName());
        ResourcePackBackend.ApplyOutcome outcome = backend.apply(client, snapshot.requestedSelection());
        if (!outcome.accepted()) {
            finalizeFailure("Resource pack switch failed.", outcome.failureReason(), nowNanos, baselineActiveState);
            return;
        }

        completionFuture = outcome.completionFuture();
        if (completionFuture == null) {
            finalizeFailure("Resource pack switch failed.", "Resource-pack backend did not provide a completion future.", nowNanos, baselineActiveState);
            return;
        }

        applyStarted = true;
    }

    private void beginFailureRecovery(MinecraftClient client, String publicReason, String diagnosticReason, long nowNanos) {
        if (!applyStarted || baselineActiveState == null) {
            finalizeFailure(publicReason, diagnosticReason, nowNanos, baselineActiveState);
            return;
        }

        ResourcePackBackend.ApplyOutcome restoreOutcome = backend.restore(client, baselineActiveState);
        if (!restoreOutcome.accepted() || restoreOutcome.completionFuture() == null) {
            finalizeFailure(
                publicReason,
                combineDiagnostics(diagnosticReason, restoreOutcome.failureReason()),
                nowNanos,
                baselineActiveState
            );
            return;
        }

        completionFuture = restoreOutcome.completionFuture();
        applyStarted = false;
        pendingFailureReason = publicReason;
        pendingDiagnosticReason = diagnosticReason;
        recoveryInProgress = true;
        applyDeadlineNanos = nowNanos + APPLY_TIMEOUT_NANOS;
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Started resource-pack rollback to baseline state.");
    }

    private void pollRecovery(MinecraftClient client, long nowNanos) {
        if (completionFuture == null || !completionFuture.isDone()) {
            return;
        }

        try {
            completionFuture.join();
        } catch (CancellationException exception) {
            finalizeFailure(
                pendingFailureReason == null ? "Resource pack switch failed." : pendingFailureReason,
                combineDiagnostics(pendingDiagnosticReason, "Resource-pack rollback was cancelled."),
                nowNanos,
                baselineActiveState
            );
            return;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            finalizeFailure(
                pendingFailureReason == null ? "Resource pack switch failed." : pendingFailureReason,
                combineDiagnostics(pendingDiagnosticReason, cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()),
                nowNanos,
                baselineActiveState
            );
            return;
        }

        ActiveResourcePackState restoredState = backend.queryActiveState(client).orElse(baselineActiveState);
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Rolled back resource-pack state to " + (restoredState == null ? "unknown" : restoredState.enabledPackIds()));
        finalizeFailure(
            pendingFailureReason == null ? "Resource pack switch failed." : pendingFailureReason,
            pendingDiagnosticReason == null ? "Resource-pack switch failed." : pendingDiagnosticReason,
            nowNanos,
            restoredState
        );
    }

    private void resetToIdle() {
        snapshot = ResourcePackTransitionSnapshot.idle();
        completionFuture = null;
        applyStarted = false;
        baselineActiveState = null;
        pendingFailureReason = null;
        pendingDiagnosticReason = null;
        recoveryInProgress = false;
    }

    private static String combineDiagnostics(String primary, String secondary) {
        if (primary == null || primary.isBlank()) {
            return secondary == null ? "" : secondary;
        }
        if (secondary == null || secondary.isBlank()) {
            return primary;
        }
        return primary + " | " + secondary;
    }

    private void notifyApplying(ResourcePackTransitionSnapshot snapshot) {
        try {
            listener.onApplying(snapshot);
        } catch (Throwable throwable) {
            ReliefLogger.warn("Resource-pack transition listener failed during apply start");
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Resource-pack transition listener failure during apply start", throwable);
        }
    }

    private void notifyResolved(ResourcePackTransitionSnapshot snapshot) {
        try {
            listener.onResolved(snapshot);
        } catch (Throwable throwable) {
            ReliefLogger.warn("Resource-pack transition listener failed during apply resolution");
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Resource-pack transition listener failure during apply resolution", throwable);
        }
    }
}
