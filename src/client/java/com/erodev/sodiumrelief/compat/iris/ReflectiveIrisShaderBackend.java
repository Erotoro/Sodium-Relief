package com.erodev.sodiumrelief.compat.iris;

import com.erodev.sodiumrelief.shader.ActiveShaderState;
import com.erodev.sodiumrelief.shader.ShaderBackend;
import com.erodev.sodiumrelief.shader.ShaderPackSelection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;

public final class ReflectiveIrisShaderBackend implements ShaderBackend {
    private final IrisBindings bindings;
    private final String unavailableReason;

    public ReflectiveIrisShaderBackend() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            this.bindings = null;
            this.unavailableReason = "Iris is not installed.";
            return;
        }

        IrisBindings resolved = IrisBindings.tryResolve();
        this.bindings = resolved;
        this.unavailableReason = resolved == null
            ? "This Iris build does not expose a supported runtime shader switching API."
            : "";
    }

    @Override
    public String name() {
        return "Iris";
    }

    @Override
    public boolean available() {
        return bindings != null;
    }

    @Override
    public String unavailableReason() {
        return unavailableReason;
    }

    @Override
    @SuppressWarnings("null")
    public Optional<ActiveShaderState> queryActiveState() {
        if (bindings == null) {
            return Optional.empty();
        }
        try {
            Object apiInstance = bindings.apiGetInstance.invoke(null);
            String packName = bindings.packNameGetter == null ? "" : normalizePackName(bindings.packNameGetter.invoke(apiInstance));
            boolean enabled = bindings.enabledGetter != null
                ? Boolean.TRUE.equals(bindings.enabledGetter.invoke(apiInstance))
                : !packName.isBlank();
            if (!enabled) {
                return Optional.of(ActiveShaderState.disabled());
            }
            if (packName.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ActiveShaderState(packName, true));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    @Override
    public ApplyOutcome apply(ShaderPackSelection selection) {
        if (bindings == null) {
            return ApplyOutcome.failure(unavailableReason);
        }

        try {
            Object apiInstance = bindings.apiGetInstance.invoke(null);
            if (selection.shadersEnabled()) {
                if (bindings.packSelector == null) {
                    return ApplyOutcome.failure("This Iris build does not expose shader pack selection.");
                }
                bindings.packSelector.invoke(resolveTarget(bindings.packSelector, apiInstance), selection.packName());
                if (bindings.enabledSetter != null) {
                    bindings.enabledSetter.invoke(resolveTarget(bindings.enabledSetter, apiInstance), true);
                }
            } else {
                if (bindings.enabledSetter == null) {
                    return ApplyOutcome.failure("This Iris build does not expose shader enable or disable controls.");
                }
                bindings.enabledSetter.invoke(resolveTarget(bindings.enabledSetter, apiInstance), false);
            }

            if (bindings.reloadMethod != null) {
                bindings.reloadMethod.invoke(resolveTarget(bindings.reloadMethod, apiInstance));
            }
            return ApplyOutcome.success();
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return ApplyOutcome.failure(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            return ApplyOutcome.failure(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private static Object resolveTarget(Method method, Object apiInstance) {
        return Modifier.isStatic(method.getModifiers()) ? null : apiInstance;
    }

    private static String normalizePackName(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(ReflectiveIrisShaderBackend::normalizePackName).orElse("");
        }
        return Objects.toString(value, "").trim();
    }

    private record IrisBindings(
        Method apiGetInstance,
        Method enabledGetter,
        Method packNameGetter,
        Method enabledSetter,
        Method packSelector,
        Method reloadMethod
    ) {
        private static IrisBindings tryResolve() {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method apiGetInstance = apiClass.getMethod("getInstance");
                Method enabledGetter = findMethod(apiClass, new Class<?>[0], "isShaderPackInUse", "isShaderPackEnabled", "areShadersEnabled");
                Method packNameGetter = findMethod(apiClass, new Class<?>[0], "getCurrentPackName", "getCurrentShaderPackName", "getShaderPackName", "getSelectedShaderPackName");
                Method enabledSetter = findMethod(apiClass, new Class<?>[]{boolean.class}, "setShadersEnabled", "setShaderPackEnabled");
                Method packSelector = findMethod(apiClass, new Class<?>[]{String.class}, "setShaderPack", "selectShaderPack", "setActivePack", "setCurrentPack");

                Method reloadMethod = null;
                try {
                    Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
                    reloadMethod = findMethod(irisClass, new Class<?>[0], "reload", "reloadShaderpack", "reloadShaders");
                } catch (ClassNotFoundException ignored) {
                    reloadMethod = findMethod(apiClass, new Class<?>[0], "reload", "reloadShaderpack", "reloadShaders");
                }

                if (enabledGetter == null && packNameGetter == null) {
                    return null;
                }
                if (enabledSetter == null && packSelector == null) {
                    return null;
                }

                return new IrisBindings(apiGetInstance, enabledGetter, packNameGetter, enabledSetter, packSelector, reloadMethod);
            } catch (ReflectiveOperationException exception) {
                return null;
            }
        }
    }

    private static Method findMethod(Class<?> owner, Class<?>[] parameterTypes, String... candidates) {
        for (String candidate : candidates) {
            try {
                return owner.getMethod(candidate, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
