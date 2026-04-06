package com.erodev.sodiumrelief.resourcepack;

import com.erodev.sodiumrelief.config.ReliefConfig;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class ResourcePackTransitionStatusOverlay {
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int APPLYING_COLOR = 0xF2F2F2;
    private static final int SUCCESS_COLOR = 0xCFF7D3;
    private static final int FAILURE_COLOR = 0xFFD5D5;

    private final Supplier<ResourcePackTransitionSnapshot> snapshotSupplier;
    private boolean supported = true;
    private boolean enabled = true;
    private BooleanSupplier debugLoggingEnabled = () -> false;

    public ResourcePackTransitionStatusOverlay(Supplier<ResourcePackTransitionSnapshot> snapshotSupplier) {
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier);
    }

    public void debugLoggingEnabled(BooleanSupplier debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled == null ? () -> false : debugLoggingEnabled;
    }

    public void applyConfig(ReliefConfig config) {
        enabled = config.enableMod && config.enableResourcePackStatusOverlay;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!supported || !enabled) {
            return;
        }

        ResourcePackTransitionSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || snapshot.state() == ResourcePackTransitionState.IDLE) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || client.options.hudHidden) {
            return;
        }

        Text message = switch (snapshot.state()) {
            case APPLYING -> Text.literal("Applying resource pack...");
            case SUCCESS -> Text.literal("Resource pack applied");
            case FAILED -> Text.literal("Resource pack switch failed");
            case IDLE -> Text.empty();
        };
        int textWidth = client.textRenderer.getWidth(message);
        int contentWidth = textWidth + 12;
        int contentHeight = 16;
        int x = (client.getWindow().getScaledWidth() - contentWidth) / 2;
        int y = 36;

        context.fill(x, y, x + contentWidth, y + contentHeight, BACKGROUND_COLOR);
        context.drawCenteredTextWithShadow(client.textRenderer, message, x + (contentWidth / 2), y + 4, color(snapshot.state()));
    }

    public void disableSupport(Throwable throwable) {
        supported = false;
        ReliefLogger.warn("Disabled resource-pack status overlay");
        ReliefLogger.debug(debugLoggingEnabled.getAsBoolean(), "Resource-pack status overlay support failure", throwable);
    }

    private static int color(ResourcePackTransitionState state) {
        return switch (state) {
            case APPLYING -> APPLYING_COLOR;
            case SUCCESS -> SUCCESS_COLOR;
            case FAILED -> FAILURE_COLOR;
            case IDLE -> APPLYING_COLOR;
        };
    }
}
