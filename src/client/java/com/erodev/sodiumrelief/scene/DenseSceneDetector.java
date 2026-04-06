package com.erodev.sodiumrelief.scene;

import com.erodev.sodiumrelief.config.AdaptiveMode;
import com.erodev.sodiumrelief.config.ReliefConfig;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class DenseSceneDetector {
    private static final int[][] SAMPLE_OFFSETS = {
        {0, 0},
        {-1, 0},
        {1, 0},
        {0, -1},
        {0, 1}
    };

    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
    private int ticksUntilScan;
    private DenseSceneSnapshot lastSnapshot = DenseSceneSnapshot.low();

    public DenseSceneSnapshot sample(MinecraftClient client, ReliefConfig config, AdaptiveMode mode) {
        if (--ticksUntilScan > 0) {
            return lastSnapshot;
        }

        ticksUntilScan = Math.max(1, adjustedScanInterval(config.denseSceneScanIntervalTicks, mode));

        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null || client.currentScreen != null) {
            lastSnapshot = DenseSceneSnapshot.low();
            return lastSnapshot;
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d forward = player.getRotationVec(1.0F).normalize();
        Vec3d right = forward.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
        if (right.lengthSquared() < 1.0E-4D) {
            right = new Vec3d(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        int radius = Math.max(3, adjustedRadius(config.denseSceneScanRadius, mode));
        int sampledPositions = 0;
        int occupiedSamples = 0;
        int foliageSamples = 0;
        int transparencySamples = 0;
        int decorativeSamples = 0;

        for (int distance = 2; distance <= radius; distance++) {
            for (int[] offset : SAMPLE_OFFSETS) {
                sampledPositions++;

                Vec3d sample = eyePos
                    .add(forward.multiply(distance))
                    .add(right.multiply(offset[0] * 1.35D))
                    .add(0.0D, offset[1] * 0.9D, 0.0D);
                mutablePos.set(sample.x, sample.y, sample.z);
                BlockState state = world.getBlockState(mutablePos);
                if (state.isAir()) {
                    continue;
                }

                occupiedSamples++;
                Block block = state.getBlock();
                boolean foliageBlock = isFoliageBlock(block);
                boolean transparencyBlock = !foliageBlock && isTransparencyHeavyBlock(block);
                boolean decorativeBlock = !foliageBlock && !transparencyBlock && isDecorativeBlock(block);

                if (foliageBlock) {
                    foliageSamples++;
                }
                if (transparencyBlock) {
                    transparencySamples++;
                }
                if (decorativeBlock) {
                    decorativeSamples++;
                }
            }
        }

        double occupiedRatio = ratio(occupiedSamples, sampledPositions);
        double foliageRatio = ratio(foliageSamples, occupiedSamples);
        double transparencyRatio = ratio(transparencySamples, occupiedSamples);
        double decorativeRatio = ratio(decorativeSamples, occupiedSamples);
        double densityScore = occupiedRatio * ((foliageRatio * 0.60D) + (transparencyRatio * 0.45D) + (decorativeRatio * 0.20D));

        boolean foliageHeavy = occupiedSamples >= 4 && occupiedRatio >= 0.24D && foliageRatio >= 0.46D;
        boolean transparencyHeavy = occupiedSamples >= 4 && occupiedRatio >= 0.24D && transparencyRatio >= 0.36D;
        SceneDensityState densityState = classifyDensity(densityScore, occupiedRatio, foliageHeavy, transparencyHeavy);
        lastSnapshot = new DenseSceneSnapshot(densityState, foliageHeavy, transparencyHeavy);
        return lastSnapshot;
    }

    private static int adjustedScanInterval(int baseInterval, AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> Math.max(2, baseInterval + 1);
            case BALANCED -> baseInterval;
            case AGGRESSIVE -> Math.max(1, baseInterval - 1);
        };
    }

    private static int adjustedRadius(int baseRadius, AdaptiveMode mode) {
        return switch (mode) {
            case SAFE -> Math.max(3, baseRadius - 1);
            case BALANCED -> baseRadius;
            case AGGRESSIVE -> baseRadius + 1;
        };
    }

    private static SceneDensityState classifyDensity(double densityScore, double occupiedRatio, boolean foliageHeavy, boolean transparencyHeavy) {
        if (occupiedRatio <= 0.0D) {
            return SceneDensityState.LOW;
        }
        if (densityScore >= 0.34D || (densityScore >= 0.26D && occupiedRatio >= 0.52D && (foliageHeavy || transparencyHeavy))) {
            return SceneDensityState.DENSE;
        }
        if (densityScore >= 0.18D || ((foliageHeavy || transparencyHeavy) && occupiedRatio >= 0.20D) || occupiedRatio >= 0.55D) {
            return SceneDensityState.MODERATE;
        }
        return SceneDensityState.LOW;
    }

    private static double ratio(int numerator, int denominator) {
        if (numerator <= 0 || denominator <= 0) {
            return 0.0D;
        }
        return numerator / (double) denominator;
    }

    private static boolean isFoliageBlock(Block block) {
        return block instanceof LeavesBlock
            || block instanceof TallPlantBlock
            || block instanceof FlowerBlock
            || block instanceof BambooBlock
            || block instanceof CropBlock
            || block instanceof SaplingBlock
            || block instanceof PlantBlock;
    }

    private static boolean isTransparencyHeavyBlock(Block block) {
        return block instanceof TransparentBlock
            || block instanceof TintedGlassBlock
            || block instanceof PaneBlock
            || block instanceof LanternBlock;
    }

    private static boolean isDecorativeBlock(Block block) {
        return block instanceof LanternBlock
            || block instanceof CandleBlock
            || block instanceof ChainBlock
            || block instanceof CarpetBlock;
    }
}
