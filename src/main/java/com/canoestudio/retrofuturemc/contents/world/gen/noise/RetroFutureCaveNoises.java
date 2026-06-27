package com.canoestudio.retrofuturemc.contents.world.gen.noise;

public final class RetroFutureCaveNoises {
    public final RetroFutureNormalNoise caveTypeSelector;
    public final RetroFutureNormalNoise lushRegion;
    public final RetroFutureNormalNoise lushDetail;
    public final RetroFutureNormalNoise lushPatch;
    public final RetroFutureNormalNoise lushPatchDetail;
    public final RetroFutureNormalNoise dripstoneRegion;
    public final RetroFutureNormalNoise dripstoneDetail;
    public final RetroFutureNormalNoise dripstoneRidge;

    public RetroFutureCaveNoises(long worldSeed) {
        this.caveTypeSelector = register(worldSeed, "retro_cave_type_selector", -8, 1.0D, 1.0D);
        this.lushRegion = register(worldSeed, "retro_lush_caves_region", -8, 1.0D, 1.0D);
        this.lushDetail = register(worldSeed, "retro_lush_caves_detail", -7, 1.0D, 1.0D);
        this.lushPatch = register(worldSeed, "retro_lush_caves_patch", -7, 1.0D, 1.0D);
        this.lushPatchDetail = register(worldSeed, "retro_lush_caves_patch_detail", -6, 1.0D, 1.0D);
        this.dripstoneRegion = register(worldSeed, "retro_dripstone_caves_region", -8, 1.0D, 1.0D);
        this.dripstoneDetail = register(worldSeed, "retro_dripstone_caves_detail", -7, 1.0D, 1.0D);
        this.dripstoneRidge = register(worldSeed, "retro_dripstone_caves_ridge", -6, 1.0D, 1.0D);
    }

    private static RetroFutureNormalNoise register(long worldSeed, String key, int firstOctave, double firstAmplitude, double... amplitudes) {
        return RetroFutureNormalNoise.create(worldSeed, key, firstOctave, firstAmplitude, amplitudes);
    }
}
