package com.erodev.sodiumrelief.shader;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class ShaderTransitionStatusOverlay {
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int APPLYING_COLOR = 0xF2F2F2;
    private static final int SUCCESS_COLOR = 0xCFF7D3;
    private static final int FAILURE_COLOR = 0xFFD5D5;

    private final Supplier<ShaderTransitionSnapshot> snapshotSupplier;
    private boolean supported = true;
    private boolean enabled = true;
    private BooleanSupplier debugLoggingEnabled = () -> false;

    public ShaderTransitionStatusOverlay(Supplier<ShaderTransitionSnapshot> snapshotSupplier) {
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier);
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableShaderStatusOverlay;
    }

    public void debugLoggingEnabled(BooleanSupplier debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled == null ? () -> false : debugLoggingEnabled;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!supported || !enabled) {
            return;
        }

        ShaderTransitionSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || snapshot.state() == ShaderTransitionState.IDLE) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || client.options.hudHidden) {
            return;
        }

        Text message = message(snapshot);
        int textWidth = client.textRenderer.getWidth(message);
        int contentWidth = textWidth + 12;
        int contentHeight = 16;
        int x = (client.getWindow().getScaledWidth() - contentWidth) / 2;
        int y = 16;

        context.fill(x, y, x + contentWidth, y + contentHeight, BACKGROUND_COLOR);
        context.drawCenteredTextWithShadow(
            client.textRenderer,
            message,
            x + (contentWidth / 2),
            y + 4,
            color(snapshot.state())
        );
    }

    public void disableSupport(Throwable throwable) {
        supported = false;
        ReliefLogger.warn("Disabled shader status overlay");
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Shader status overlay support failure", throwable);
    }

    private static Text message(ShaderTransitionSnapshot snapshot) {
        return switch (snapshot.state()) {
            case APPLYING -> Text.literal("Applying shaders...");
            case SUCCESS -> successMessage(snapshot);
            case FAILED -> Text.literal("Shader switch failed");
            case IDLE -> Text.empty();
        };
    }

    private static Text successMessage(ShaderTransitionSnapshot snapshot) {
        ActiveShaderState activeSelection = snapshot.activeSelection();
        if (activeSelection != null && !activeSelection.shadersEnabled()) {
            return Text.literal("Shaders disabled");
        }

        ShaderPackSelection requestedSelection = snapshot.requestedSelection();
        if (requestedSelection != null && !requestedSelection.shadersEnabled()) {
            return Text.literal("Shaders disabled");
        }

        return Text.literal("Shaders applied");
    }

    private static int color(ShaderTransitionState state) {
        return switch (state) {
            case APPLYING -> APPLYING_COLOR;
            case SUCCESS -> SUCCESS_COLOR;
            case FAILED -> FAILURE_COLOR;
            case IDLE -> APPLYING_COLOR;
        };
    }
}
