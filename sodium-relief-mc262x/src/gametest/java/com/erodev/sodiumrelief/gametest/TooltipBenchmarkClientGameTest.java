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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * MC 26.2 port of the tooltip-reuse benchmark gametest. Same two scenarios as the
 * 1.21.11 module (static hover + inventory scan), written against the official,
 * unobfuscated names that 26.1+ ships with ({@code Minecraft}, {@code getMenu()},
 * {@code hoveredSlot}/{@code hasItem()}, {@code getGuiScaledWidth()}, etc.).
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
            singleplayer.getClientLevel().waitForChunksRender();

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
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
    }

    private void refillInventory(ClientGameTestContext context, IntFunction<ItemStack> stackForSlot) {
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new AssertionError("Client player was not present after world load");
            }
            for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
                client.player.getInventory().setItem(slot, stackForSlot.apply(slot));
            }
        });
        context.waitTicks(2);
    }

    private static IntFunction<ItemStack> distinctItemStacks() {
        List<Item> items = new ArrayList<>(INVENTORY_SLOTS);
        for (Item item : BuiltInRegistries.ITEM) {
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

    private static long[] readCounters(Minecraft client) {
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

    private static Slot firstFilledSlot(Minecraft client) {
        InventoryScreen screen = currentInventoryScreen(client);
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem()) {
                return slot;
            }
        }
        throw new AssertionError("No filled slot was found in the inventory screen");
    }

    private static List<double[]> filledSlotTargets(Minecraft client) {
        InventoryScreen screen = currentInventoryScreen(client);
        List<double[]> targets = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem()) {
                targets.add(cursorTargetForSlot(client, slot));
            }
        }
        return targets;
    }

    private static InventoryScreen currentInventoryScreen(Minecraft client) {
        if (client.gui.screen() instanceof InventoryScreen screen) {
            return screen;
        }
        throw new AssertionError("Inventory screen was not open");
    }

    private static double[] cursorTargetForSlot(Minecraft client, Slot slot) {
        Window window = client.getWindow();
        int originX = (window.getGuiScaledWidth() - INVENTORY_BACKGROUND_WIDTH) / 2;
        int originY = (window.getGuiScaledHeight() - INVENTORY_BACKGROUND_HEIGHT) / 2;
        double scaledCenterX = originX + slot.x + SLOT_SIZE / 2.0;
        double scaledCenterY = originY + slot.y + SLOT_SIZE / 2.0;

        double rawX = scaledCenterX * window.getWidth() / window.getGuiScaledWidth();
        double rawY = scaledCenterY * window.getHeight() / window.getGuiScaledHeight();
        return new double[] {rawX, rawY};
    }
}
