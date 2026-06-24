package com.canoestudio.retrofuturemc.contents.world.biome;

import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;

public final class ModCaveBiomes {
    public static final Biome LUSH_CAVES = new CaveBiome("lush_caves", "Lush Caves", 0x7AB86A, 0.5F, 0.9F);
    public static final Biome DRIPSTONE_CAVES = new CaveBiome("dripstone_caves", "Dripstone Caves", 0x8C7B6A, 0.8F, 0.4F);
    public static final Biome[] BIOMES = new Biome[] {LUSH_CAVES, DRIPSTONE_CAVES};

    private ModCaveBiomes() {
    }

    private static final class CaveBiome extends Biome {
        private CaveBiome(String registryName, String displayName, int waterColor, float temperature, float rainfall) {
            super(new BiomeProperties(displayName)
                    .setBaseHeight(0.0F)
                    .setHeightVariation(0.0F)
                    .setTemperature(temperature)
                    .setRainfall(rainfall)
                    .setWaterColor(waterColor));
            setRegistryName(Tags.MOD_ID, registryName);

            topBlock = Blocks.STONE.getDefaultState();
            fillerBlock = Blocks.STONE.getDefaultState();
            spawnableMonsterList.clear();
            spawnableCreatureList.clear();
            spawnableWaterCreatureList.clear();
            spawnableCaveCreatureList.clear();
        }
    }
}
