package com.erodev.sodiumrelief.gametest;

import com.erodev.sodiumrelief.client.SodiumReliefClient;
import com.erodev.sodiumrelief.client.SodiumReliefRuntime;
import com.erodev.sodiumrelief.debug.ReliefLogger;
import com.erodev.sodiumrelief.debug.ReliefMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.Window;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;

/**
 * Drives a real client into the player inventory and measures, in-game, how much
 * tooltip-rebuild work Sodium Relief's layout reuse avoids. Two honest scenarios:
 *
 * <ol>
 *   <li><b>Static hover</b> — the cursor rests on one item for a sustained period,
 *       the best case for reuse.</li>
 *   <li><b>Inventory scan</b> — the cursor sweeps across many distinct items in
 *       several passes (the "comparing items in a chest" case), a realistic mix of
 *       cache misses and reuse.</li>
 * </ol>
 *
 * Each scenario exports a {@code BenchmarkSnapshot} and asserts that reuse occurred,
 * so the published numbers are measured rather than hand-written.
 */
@SuppressWarnings("UnstableApiUsage")
public class TooltipBenchmarkClientGameTest implements FabricClientGameTest {
    private static final int INVENTORY_SLOTS = 36;
    private static final int SLOT_SIZE = 16;
    private static final int INVENTORY_BACKGROUND_WIDTH = 176;
    private static final int INVENTORY_BACKGROUND_HEIGHT = 166;
    private static final int WARMUP_TICKS = 10;
    private static final int STATIC_SAMPLE_TICKS = 400;
    private static final int SWEEP_PASSES = 3;
    private static final int SWEEP_TICKS_PER_SLOT = 2;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            // Benchmark with detailed metrics on so build-cost and text-width timings are captured.
            context.runOnClient(client -> SodiumReliefClient.runtime().metrics().applyDebugMode(true));
            singleplayer.getClientWorld().waitForChunksRender();

            openInventoryWith(context, slot -> new ItemStack(Items.NETHERITE_PICKAXE));
            runStaticHoverScenario(context);

            refillInventory(context, distinctItemStacks());
            runInventoryScanScenario(context);
        }
    }

    private void runStaticHoverScenario(ClientGameTestContext context) {
        double[] cursor = context.computeOnClient(client -> cursorTargetForSlot(client, firstFilledSlot(client)));
        context.getInput().setCursorPos(cursor[0], cursor[1]);

        // Measure from the moment the cursor lands, so the delta includes the one
        // unavoidable first rebuild as well as every subsequent reuse.
        long[] before = context.computeOnClient(TooltipBenchmarkClientGameTest::readCounters);
        context.waitTicks(WARMUP_TICKS);
        context.takeScreenshot("sodium-relief-inventory-tooltip");
        context.waitTicks(STATIC_SAMPLE_TICKS);
        long[] after = context.computeOnClient(TooltipBenchmarkClientGameTest::readCounters);

        context.runOnClient(client -> SodiumReliefClient.runtime().exportBenchmarkSnapshot("gametest-inventory-static"));
        logAndAssertScenario("static-hover", before, after);
    }

    private void runInventoryScanScenario(ClientGameTestContext context) {
        List<double[]> targets = context.computeOnClient(TooltipBenchmarkClientGameTest::filledSlotTargets);
        if (targets.isEmpty()) {
            throw new AssertionError("No filled slots to scan");
        }

        long[] before = context.computeOnClient(TooltipBenchmarkClientGameTest::readCounters);
        for (int pass = 0; pass < SWEEP_PASSES; pass++) {
            for (double[] target : targets) {
                context.getInput().setCursorPos(target[0], target[1]);
                context.waitTicks(SWEEP_TICKS_PER_SLOT);
            }
        }
        long[] after = context.computeOnClient(TooltipBenchmarkClientGameTest::readCounters);

        context.runOnClient(client -> SodiumReliefClient.runtime().exportBenchmarkSnapshot("gametest-inventory-scan"));
        logAndAssertScenario("inventory-scan", before, after);
    }

    private void openInventoryWith(ClientGameTestContext context, IntFunction<ItemStack> stackForSlot) {
        refillInventory(context, stackForSlot);
        context.setScreen(() -> new InventoryScreen(MinecraftClient.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
    }

    private void refillInventory(ClientGameTestContext context, IntFunction<ItemStack> stackForSlot) {
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new AssertionError("Client player was not present after world load");
            }
            for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
                client.player.getInventory().setStack(slot, stackForSlot.apply(slot));
            }
        });
        context.waitTicks(2);
    }

    private static IntFunction<ItemStack> distinctItemStacks() {
        List<Item> items = new ArrayList<>(INVENTORY_SLOTS);
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            items.add(item);
            if (items.size() >= INVENTORY_SLOTS) {
                break;
            }
        }
        return slot -> new ItemStack(items.get(slot % items.size()));
    }

    private static long[] readCounters(MinecraftClient client) {
        SodiumReliefRuntime runtime = SodiumReliefClient.runtime();
        if (runtime == null) {
            throw new AssertionError("Sodium Relief runtime was not initialised");
        }
        ReliefMetrics metrics = runtime.metrics();
        return new long[] {
            metrics.tooltipPathInvocations(),
            metrics.tooltipExpensivePathInvocations(),
            metrics.tooltipHits(),
            metrics.tooltipMisses()
        };
    }

    private static void logAndAssertScenario(String name, long[] before, long[] after) {
        long invocations = after[0] - before[0];
        long expensive = after[1] - before[1];
        long hits = after[2] - before[2];
        long misses = after[3] - before[3];
        long reused = invocations - expensive;
        double reuseRate = invocations <= 0 ? 0.0 : (reused * 100.0) / invocations;

        ReliefLogger.info(String.format(
            "[gametest] %s: invocations=%d expensive=%d reused=%d hits=%d misses=%d reuse=%.2f%%",
            name, invocations, expensive, reused, hits, misses, reuseRate
        ));

        if (invocations <= 0) {
            throw new AssertionError(name + ": cursor never settled on a filled slot (0 invocations)");
        }
        if (reused <= 0) {
            throw new AssertionError(name + ": no reuse occurred (expensive " + expensive + " of " + invocations + " invocations)");
        }
    }

    private static Slot firstFilledSlot(MinecraftClient client) {
        InventoryScreen screen = currentInventoryScreen(client);
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.hasStack()) {
                return slot;
            }
        }
        throw new AssertionError("No filled slot was found in the inventory screen");
    }

    private static List<double[]> filledSlotTargets(MinecraftClient client) {
        InventoryScreen screen = currentInventoryScreen(client);
        List<double[]> targets = new ArrayList<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.hasStack()) {
                targets.add(cursorTargetForSlot(client, slot));
            }
        }
        return targets;
    }

    private static InventoryScreen currentInventoryScreen(MinecraftClient client) {
        if (client.currentScreen instanceof InventoryScreen screen) {
            return screen;
        }
        throw new AssertionError("Inventory screen was not open");
    }

    private static double[] cursorTargetForSlot(MinecraftClient client, Slot slot) {
        Window window = client.getWindow();
        int originX = (window.getScaledWidth() - INVENTORY_BACKGROUND_WIDTH) / 2;
        int originY = (window.getScaledHeight() - INVENTORY_BACKGROUND_HEIGHT) / 2;
        double scaledCenterX = originX + slot.x + SLOT_SIZE / 2.0;
        double scaledCenterY = originY + slot.y + SLOT_SIZE / 2.0;

        double rawX = scaledCenterX * window.getWidth() / window.getScaledWidth();
        double rawY = scaledCenterY * window.getHeight() / window.getScaledHeight();
        return new double[] {rawX, rawY};
    }
}
