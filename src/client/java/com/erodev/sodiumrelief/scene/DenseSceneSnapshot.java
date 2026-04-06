package com.erodev.sodiumrelief.scene;

public record DenseSceneSnapshot(
    SceneDensityState densityState,
    boolean foliageHeavy,
    boolean transparencyHeavy
) {
    public static DenseSceneSnapshot low() {
        return new DenseSceneSnapshot(SceneDensityState.LOW, false, false);
    }
}
