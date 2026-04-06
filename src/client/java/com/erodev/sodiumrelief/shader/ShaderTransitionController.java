package com.erodev.sodiumrelief.shader;

import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.util.SafeTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;

public final class ShaderTransitionController {
    private static final long APPLY_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(12L);
    private static final long TERMINAL_VISIBILITY_NANOS = TimeUnit.MILLISECONDS.toNanos(1500L);

    private final ShaderBackend backend;
    private final ShaderTransitionListener listener;
    private final ShaderApplyGate applyGate;
    private final BooleanSupplier debugLoggingEnabled;
    private ShaderTransitionSnapshot snapshot = ShaderTransitionSnapshot.idle();
    private long applyDeadlineNanos;
    private boolean applyStarted;

    public ShaderTransitionController(ShaderBackend backend) {
        this(backend, ShaderTransitionListener.NO_OP, ShaderApplyGate.IMMEDIATE, () -> false);
    }

    public ShaderTransitionController(ShaderBackend backend, ShaderTransitionListener listener) {
        this(backend, listener, ShaderApplyGate.IMMEDIATE, () -> false);
    }

    public ShaderTransitionController(ShaderBackend backend, ShaderTransitionListener listener, BooleanSupplier debugLoggingEnabled) {
        this(backend, listener, ShaderApplyGate.IMMEDIATE, debugLoggingEnabled);
    }

    public ShaderTransitionController(
        ShaderBackend backend,
        ShaderTransitionListener listener,
        ShaderApplyGate applyGate,
        BooleanSupplier debugLoggingEnabled
    ) {
        this.backend = Objects.requireNonNull(backend);
        this.listener = Objects.requireNonNull(listener);
        this.applyGate = Objects.requireNonNull(applyGate);
        this.debugLoggingEnabled = Objects.requireNonNull(debugLoggingEnabled);
    }

    public ShaderSwitchRequestResult requestSwitch(MinecraftClient client, ShaderPackSelection selection) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(selection, "selection");

        if (snapshot.state() == ShaderTransitionState.APPLYING) {
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Ignored shader switch request because another switch is already applying");
            return ShaderSwitchRequestResult.ALREADY_APPLYING;
        }

        long nowNanos = SafeTime.nowNanos();
        beginApplying(selection, nowNanos);
        maybeStartApply(nowNanos);
        if (snapshot.state() == ShaderTransitionState.APPLYING && applyStarted) {
            resolveApplying(nowNanos);
        }
        return snapshot.state() == ShaderTransitionState.FAILED
            ? ShaderSwitchRequestResult.FAILED_TO_START
            : ShaderSwitchRequestResult.STARTED;
    }

    public void tick(MinecraftClient client) {
        if (snapshot.state() == ShaderTransitionState.APPLYING) {
            long nowNanos = SafeTime.nowNanos();
            maybeStartApply(nowNanos);
            if (applyStarted) {
                resolveApplying(nowNanos);
            } else if (nowNanos >= applyDeadlineNanos) {
                fail("Shader switching timed out.", "Shader switch did not begin before the transition deadline elapsed.", nowNanos);
            }
            return;
        }

        if ((snapshot.state() == ShaderTransitionState.SUCCESS || snapshot.state() == ShaderTransitionState.FAILED)
            && SafeTime.nowNanos() - snapshot.stateSinceNanos() >= TERMINAL_VISIBILITY_NANOS) {
            snapshot = ShaderTransitionSnapshot.idle();
            applyStarted = false;
        }
    }

    public ShaderTransitionSnapshot snapshot() {
        return snapshot;
    }

    private void resolveApplying(long nowNanos) {
        Optional<ActiveShaderState> activeState = backend.queryActiveState();
        activeState.ifPresent(state -> snapshot = new ShaderTransitionSnapshot(
            snapshot.state(),
            snapshot.requestedSelection(),
            state,
            snapshot.failureReason(),
            snapshot.stateSinceNanos()
        ));

        if (snapshot.requestedSelection() != null && activeState.filter(state -> state.matches(snapshot.requestedSelection())).isPresent()) {
            succeed(activeState.orElse(null), nowNanos);
            return;
        }

        if (nowNanos >= applyDeadlineNanos) {
            fail("Shader switching timed out.", "Shader switch timed out before Iris reported the requested state.", nowNanos);
        }
    }

    private void beginApplying(ShaderPackSelection selection, long nowNanos) {
        snapshot = new ShaderTransitionSnapshot(ShaderTransitionState.APPLYING, selection, snapshot.activeSelection(), "", nowNanos);
        applyDeadlineNanos = nowNanos + APPLY_TIMEOUT_NANOS;
        applyStarted = false;
        notifyApplying(snapshot);
    }

    private void succeed(ActiveShaderState activeState, long nowNanos) {
        String displayName = activeState == null ? snapshot.requestedSelection().displayName() : activeState.shadersEnabled() ? activeState.packName() : "Off";
        snapshot = new ShaderTransitionSnapshot(ShaderTransitionState.SUCCESS, snapshot.requestedSelection(), activeState, "", nowNanos);
        applyStarted = false;
        notifyResolved(snapshot);
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Shader switch completed: " + displayName);
    }

    private void fail(String publicReason, String diagnosticReason, long nowNanos) {
        snapshot = new ShaderTransitionSnapshot(ShaderTransitionState.FAILED, snapshot.requestedSelection(), snapshot.activeSelection(), publicReason, nowNanos);
        applyStarted = false;
        notifyResolved(snapshot);
        ReliefLogger.warn("Shader switch failed");
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Shader switch failure details: " + diagnosticReason);
    }

    private void maybeStartApply(long nowNanos) {
        if (applyStarted || snapshot.state() != ShaderTransitionState.APPLYING || !applyGate.canStartApply(snapshot)) {
            return;
        }

        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Starting shader switch via " + backend.name() + ": " + snapshot.requestedSelection().displayName());

        if (!backend.available()) {
            fail("Shader switching is unavailable in the current environment.", backend.unavailableReason(), nowNanos);
            return;
        }

        ShaderBackend.ApplyOutcome outcome = backend.apply(snapshot.requestedSelection());
        if (!outcome.accepted()) {
            fail("Shader switching failed.", outcome.failureReason(), nowNanos);
            return;
        }

        applyStarted = true;
    }

    private void notifyApplying(ShaderTransitionSnapshot snapshot) {
        try {
            listener.onApplying(snapshot);
        } catch (Throwable throwable) {
            ReliefLogger.warn("Shader transition listener failed during apply start");
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Shader transition listener failure during apply start", throwable);
        }
    }

    private void notifyResolved(ShaderTransitionSnapshot snapshot) {
        try {
            listener.onResolved(snapshot);
        } catch (Throwable throwable) {
            ReliefLogger.warn("Shader transition listener failed during apply resolution");
            ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Shader transition listener failure during apply resolution", throwable);
        }
    }
}
