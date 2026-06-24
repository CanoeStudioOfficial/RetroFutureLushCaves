package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.PointedDripstoneBlock;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVine;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVinePlant;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.BigDripleaf;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.DripleafStem;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.SmallDripleaf;
import com.canoestudio.retrofuturemc.contents.mobs.axolotl.EntityAxolotl;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesAPI;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesConfig;
import com.yungnickyoung.minecraft.bettercaves.noise.MojangNormalNoise;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.mojang.Mojang118CaveDensitySampler;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RetroFutureCaveWorldGenerator implements IWorldGenerator {
    private static final int CHUNK_SIZE = 16;
    private static final int AZALEA_TREE_CHANCE = 18;
    private static final int HUMID_AZALEA_TREE_CHANCE = 6;
    private static final int WET_AZALEA_TREE_CHANCE = 9;
    private static final int AZALEA_TREE_MIN_SPACING = 24;
    private static final int LUSH_CAVE_MIN_Y = 8;
    private static final int LUSH_CAVE_MAX_Y = 62;
    private static final int DRIPSTONE_CAVE_MIN_Y = 8;
    private static final int DRIPSTONE_CAVE_MAX_Y = 58;
    private static final int LUSH_MIN_SURFACE_DEPTH = 12;
    private static final int DRIPSTONE_MIN_SURFACE_DEPTH = 12;
    private static final int DRIPSTONE_CLUSTER_SEARCH_RANGE = 12;
    private static final int DRIPSTONE_CLUSTER_MIN_ATTEMPTS = 4;
    private static final int DRIPSTONE_CLUSTER_MAX_ATTEMPTS = 7;
    private static final int DRIPSTONE_LARGE_FEATURE_CHANCE = 24;
    private static final int DRIPSTONE_MAX_BLOCKS_PER_CHUNK = 180;
    private static final int DRIPSTONE_MAX_COLUMNS_PER_CHUNK = 72;
    private static final int LUSH_CLAY_DRIPLEAF_PATCH_CHANCE = 8;
    private static final int LUSH_DRIPLEAF_PATCH_RADIUS_MIN = 4;
    private static final int LUSH_DRIPLEAF_PATCH_RADIUS_MAX = 7;
    private static final int LUSH_DRIPLEAF_PATCH_BASE_DEPTH = 3;
    private static final float LUSH_DRIPLEAF_PATCH_EXTRA_BOTTOM_CHANCE = 0.8F;
    private static final float LUSH_DRIPLEAF_PATCH_EDGE_CHANCE = 0.7F;
    private static final int LUSH_CLAY_PATCH_VERTICAL_RANGE = 2;
    private static final int LUSH_CLAY_POOL_VERTICAL_RANGE = 5;
    private static final float LUSH_CLAY_PATCH_VEGETATION_CHANCE = 0.05F;
    private static final float LUSH_CLAY_POOL_VEGETATION_CHANCE = 0.1F;
    private static final double LUSH_REGION_SCALE = 0.012D;
    private static final double LUSH_REGION_THRESHOLD = -0.22D;
    private static final double DRIPSTONE_REGION_SCALE = 0.011D;
    private static final double DRIPSTONE_REGION_THRESHOLD = 0.1D;
    private static final double DENSITY_COLUMN_OPEN_MARGIN = 0.24D;
    private static final double DENSITY_DECORATION_OPEN_MARGIN = 0.34D;
    private static final long LUSH_PATCH_SALT = 0x4C55534843415645L;
    private static final long DRIPSTONE_PATCH_SALT = 0x4452495053544F4EL;
    private static final int NO_SURFACE = -1;
    private static final int MIN_LUSH_CAVE_DISTANCE = 1000;
    private static final int MIN_LUSH_CAVE_DISTANCE_SQ = MIN_LUSH_CAVE_DISTANCE * MIN_LUSH_CAVE_DISTANCE;
    private static final EnumFacing[] DRIPLEAF_FEATURE_FACINGS = new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.NORTH};
    private BetterCavesConfig cachedBetterCavesConfig;
    private int cachedBetterCavesConfigDimension = Integer.MIN_VALUE;
    private Mojang118CaveDensitySampler cachedDensitySampler;
    private long cachedDensitySeed = Long.MIN_VALUE;
    private float cachedDensityHorizontalScale;
    private float cachedDensityVerticalScale;
    private CaveBiomeNoise cachedBiomeNoise;
    private long cachedBiomeNoiseSeed = Long.MIN_VALUE;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) {
            return;
        }

        if (!BetterCavesAPI.canGenerateInDimension(world.provider.getDimension())) {
            return;
        }

        int blockX = chunkX * CHUNK_SIZE;
        int blockZ = chunkZ * CHUNK_SIZE;
        CaveDensityContext caveContext = createCaveDensityContext(world, blockX, blockZ);

        if (caveContext == null) {
            return;
        }

        generateNoiseLushRegions(caveContext);
        generateDripstoneRegions(caveContext);
    }

    private CaveDensityContext createCaveDensityContext(World world, int blockX, int blockZ) {
        BetterCavesConfig betterCavesConfig = getBetterCavesConfig(world);

        if (!betterCavesConfig.isMojang118StyleCavesEnabled()) {
            return null;
        }

        return new CaveDensityContext(world, blockX, blockZ, betterCavesConfig, getDensitySampler(world, betterCavesConfig));
    }

    private BetterCavesConfig getBetterCavesConfig(World world) {
        int dimension = world.provider.getDimension();

        if (cachedBetterCavesConfig == null || cachedBetterCavesConfigDimension != dimension) {
            cachedBetterCavesConfigDimension = dimension;
            cachedBetterCavesConfig = BetterCavesAPI.getConfigForDimension(dimension);
        }

        return cachedBetterCavesConfig;
    }

    private Mojang118CaveDensitySampler getDensitySampler(World world, BetterCavesConfig config) {
        float horizontalScale = config.getMojang118StyleCaveHorizontalScale();
        float verticalScale = config.getMojang118StyleCaveVerticalScale();

        if (cachedDensitySampler == null || cachedDensitySeed != world.getSeed() || cachedDensityHorizontalScale != horizontalScale || cachedDensityVerticalScale != verticalScale) {
            cachedDensitySeed = world.getSeed();
            cachedDensityHorizontalScale = horizontalScale;
            cachedDensityVerticalScale = verticalScale;
            cachedDensitySampler = new Mojang118CaveDensitySampler(world.getSeed(), horizontalScale, verticalScale);
        }

        return cachedDensitySampler;
    }

    private CaveBiomeNoise getBiomeNoise(World world) {
        if (cachedBiomeNoise == null || cachedBiomeNoiseSeed != world.getSeed()) {
            cachedBiomeNoiseSeed = world.getSeed();
            cachedBiomeNoise = new CaveBiomeNoise(world.getSeed());
        }

        return cachedBiomeNoise;
    }

    private static final class CaveBiomeNoise {
        private final MojangNormalNoise lushRegion;
        private final MojangNormalNoise lushDetail;
        private final MojangNormalNoise lushPatch;
        private final MojangNormalNoise lushPatchDetail;
        private final MojangNormalNoise dripstoneRegion;
        private final MojangNormalNoise dripstoneDetail;
        private final MojangNormalNoise dripstoneRidge;

        private CaveBiomeNoise(long seed) {
            this.lushRegion = MojangNormalNoise.create(seed, "retro_lush_caves_region", -8, 1.0D);
            this.lushDetail = MojangNormalNoise.create(seed, "retro_lush_caves_detail", -7, 1.0D);
            this.lushPatch = MojangNormalNoise.create(seed, "retro_lush_caves_patch", -7, 1.0D);
            this.lushPatchDetail = MojangNormalNoise.create(seed, "retro_lush_caves_patch_detail", -6, 1.0D);
            this.dripstoneRegion = MojangNormalNoise.create(seed, "retro_dripstone_caves_region", -8, 1.0D);
            this.dripstoneDetail = MojangNormalNoise.create(seed, "retro_dripstone_caves_detail", -7, 1.0D);
            this.dripstoneRidge = MojangNormalNoise.create(seed, "retro_dripstone_caves_ridge", -6, 1.0D);
        }
    }

    private final class CaveDensityContext {
        private final World world;
        private final int blockX;
        private final int blockZ;
        private final Mojang118CaveDensitySampler densitySampler;
        private final CaveBiomeNoise biomeNoise;
        private final int bottomY;
        private final int topY;
        private final int surfaceCutoff;
        private final double densityThreshold;
        private final int[] surfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];
        private final int[] terrainSurfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];

        private CaveDensityContext(World world, int blockX, int blockZ, BetterCavesConfig config, Mojang118CaveDensitySampler densitySampler) {
            this.world = world;
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.densitySampler = densitySampler;
            this.biomeNoise = getBiomeNoise(world);
            this.bottomY = Math.max(0, config.getMojang118StyleCaveBottom());
            this.topY = Math.min(world.getActualHeight() - 1, config.getMojang118StyleCaveTop());
            this.surfaceCutoff = Math.max(0, config.getMojang118StyleCaveSurfaceCutoffDepth());
            this.densityThreshold = config.getMojang118StyleCaveDensityThreshold();
        }

        private boolean containsY(int y) {
            return y >= bottomY && y <= topY;
        }

        private boolean isLikelyOpen(BlockPos pos, double margin) {
            return containsY(pos.getY()) && sampleCarverDensity(pos) <= densityThreshold + margin;
        }

        private boolean isDeepEnough(BlockPos pos, int minSurfaceDepth) {
            return getTerrainSurfaceY(pos.getX(), pos.getZ()) - pos.getY() >= minSurfaceDepth;
        }

        private boolean isUndergroundOpenSpace(BlockPos pos, int minSurfaceDepth, double margin) {
            return isDeepEnough(pos, minSurfaceDepth) && !world.canSeeSky(pos) && isLikelyOpen(pos, margin);
        }

        private double caveAffinity(BlockPos pos) {
            double density = sampleCarverDensity(pos);
            return clamp((densityThreshold + 0.2D - density) / 0.4D, 0.0D, 1.0D);
        }

        private double sampleCarverDensity(BlockPos pos) {
            double density = densitySampler.sampleDensity(pos.getX(), pos.getY(), pos.getZ());

            if (surfaceCutoff <= 0) {
                return density;
            }

            int surfaceY = Math.min(topY, getSurfaceY(pos.getX(), pos.getZ()));
            int transitionBoundary = Math.max(bottomY, surfaceY - surfaceCutoff);

            if (pos.getY() >= transitionBoundary) {
                int transitionHeight = Math.max(1, surfaceY - transitionBoundary);
                double surfaceFactor = clamp((pos.getY() - transitionBoundary) / (double)transitionHeight, 0.0D, 1.0D);
                density += surfaceFactor * 0.45D;
            }

            return density;
        }

        private int getSurfaceY(int x, int z) {
            if (isInsideChunk(x, z)) {
                int localX = x - blockX;
                int localZ = z - blockZ;
                int index = localX * CHUNK_SIZE + localZ;
                int cached = surfaceYCache[index];

                if (cached != 0) {
                    return cached;
                }

                cached = world.getHeight(new BlockPos(x, 0, z)).getY();
                surfaceYCache[index] = cached;
                return cached;
            }

            return world.getHeight(new BlockPos(x, 0, z)).getY();
        }

        private int getTerrainSurfaceY(int x, int z) {
            if (isInsideChunk(x, z)) {
                int localX = x - blockX;
                int localZ = z - blockZ;
                int index = localX * CHUNK_SIZE + localZ;
                int cached = terrainSurfaceYCache[index];

                if (cached != 0) {
                    return cached;
                }

                cached = findTerrainSurfaceY(x, z);
                terrainSurfaceYCache[index] = cached;
                return cached;
            }

            return findTerrainSurfaceY(x, z);
        }

        private int findTerrainSurfaceY(int x, int z) {
            int top = Math.min(world.getActualHeight() - 1, getSurfaceY(x, z));

            for (int y = top; y > 0; y--) {
                IBlockState state = world.getBlockState(new BlockPos(x, y, z));

                if (state.getBlock() == Blocks.AIR || isIgnoredSurfaceCover(state)) {
                    continue;
                }

                return y;
            }

            return top;
        }

        private boolean isIgnoredSurfaceCover(IBlockState state) {
            Material material = state.getMaterial();
            return material == Material.LEAVES
                    || material == Material.WOOD
                    || material == Material.PLANTS
                    || material == Material.VINE;
        }

        private boolean isInsideChunk(BlockPos pos) {
            return RetroFutureCaveWorldGenerator.this.isInsideChunk(pos, blockX, blockZ);
        }

        private boolean isInsideChunk(int x, int z) {
            return x >= blockX && x < blockX + CHUNK_SIZE && z >= blockZ && z < blockZ + CHUNK_SIZE;
        }
    }

    private int getAzaleaTreeChanceForChunk(World world, int blockX, int blockZ) {
        Biome biome = world.getBiome(new BlockPos(blockX + CHUNK_SIZE / 2, 0, blockZ + CHUNK_SIZE / 2));

        if (isHumidLushSurfaceBiome(biome)) {
            return HUMID_AZALEA_TREE_CHANCE;
        }

        if (isWetLushSurfaceBiome(biome)) {
            return WET_AZALEA_TREE_CHANCE;
        }

        return AZALEA_TREE_CHANCE;
    }

    private boolean isHumidLushSurfaceBiome(Biome biome) {
        String biomeName = String.valueOf(biome.getRegistryName());

        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)
                || biomeName.contains("jungle")
                || biomeName.contains("roofed_forest")
                || biomeName.contains("dark_forest")
                || (BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST) && BiomeDictionary.hasType(biome, BiomeDictionary.Type.DENSE) && biome.getRainfall() >= 0.6F);
    }

    private boolean isWetLushSurfaceBiome(Biome biome) {
        return biome.getRainfall() >= 0.8F
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.WET)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.LUSH);
    }

    private BlockPos findSurfaceAzaleaTreePos(CaveDensityContext context, Random random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos start = context.world.getHeight(new BlockPos(context.blockX + random.nextInt(CHUNK_SIZE), 0, context.blockZ + random.nextInt(CHUNK_SIZE)));

            if (start.getY() >= 50 && start.getY() <= context.world.getActualHeight() - 16 && isLushRegionColumn(context, start.getX(), start.getZ())) {
                return start;
            }
        }

        return null;
    }

    private boolean generateSurfaceAzaleaTree(World world, Random random, BlockPos start) {
        return new WorldGenBigAzaleaTree(true).generate(world, random, start);
    }

    private boolean hasNearbyAzaleaTree(World world, BlockPos center, int radius) {
        int minY = Math.max(1, center.getY() - 6);
        int maxY = Math.min(world.getActualHeight() - 1, center.getY() + 16);
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockState(new BlockPos(center.getX() + dx, y, center.getZ() + dz)).getBlock();

                    if (isAzaleaTreeBlock(block)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isAzaleaTreeBlock(Block block) {
        return block == ModBlocks.Azalea_Leaves || block == ModBlocks.Flowering_Azalea_Leaves || block == ModBlocks.Azalea || block == ModBlocks.Flowering_Azalea;
    }

    private void generateNoiseLushRegions(CaveDensityContext context) {
        if (!isLushRegionChunk(context)) {
            return;
        }

        World world = context.world;
        Random featureRandom = createFeatureRandom(world, LUSH_PATCH_SALT, Math.floorDiv(context.blockX, CHUNK_SIZE), Math.floorDiv(context.blockZ, CHUNK_SIZE));
        int minY = Math.max(LUSH_CAVE_MIN_Y, context.bottomY);
        int maxY = Math.min(LUSH_CAVE_MAX_Y, context.topY);

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(context.blockX + localX, y, context.blockZ + localZ);

                    if (!context.isDeepEnough(pos, LUSH_MIN_SURFACE_DEPTH)) {
                        continue;
                    }

                    if (!isLushBiomePatchNoise(context, pos)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isLushReplaceable(state.getBlock()) || world.canSeeSky(pos)) {
                        continue;
                    }

                    BlockPos above = pos.up();
                    BlockPos below = pos.down();
                    boolean openAbove = isAirOrWater(world, above) && context.isUndergroundOpenSpace(above, LUSH_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);
                    boolean openBelow = world.isAirBlock(below) && context.isUndergroundOpenSpace(below, LUSH_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);

                    if (openAbove && featureRandom.nextInt(100) < 42) {
                        decorateLushFloor(world, featureRandom, pos, context.blockX, context.blockZ);
                    }

                    if (openBelow && featureRandom.nextInt(100) < 38) {
                        decorateLushCeiling(world, featureRandom, pos);
                    }
                }
            }
        }
    }

    private void generateDripstoneRegions(CaveDensityContext context) {
        if (!hasDripstoneRegionInChunk(context)) {
            return;
        }

        World world = context.world;
        Random featureRandom = createFeatureRandom(world, DRIPSTONE_PATCH_SALT, Math.floorDiv(context.blockX, CHUNK_SIZE), Math.floorDiv(context.blockZ, CHUNK_SIZE));
        int minY = Math.max(DRIPSTONE_CAVE_MIN_Y, context.bottomY);
        int maxY = Math.min(DRIPSTONE_CAVE_MAX_Y, context.topY);
        int[] budget = new int[] {DRIPSTONE_MAX_BLOCKS_PER_CHUNK, DRIPSTONE_MAX_COLUMNS_PER_CHUNK};
        int clusterAttempts = DRIPSTONE_CLUSTER_MIN_ATTEMPTS + featureRandom.nextInt(DRIPSTONE_CLUSTER_MAX_ATTEMPTS - DRIPSTONE_CLUSTER_MIN_ATTEMPTS + 1);

        for (int attempt = 0; attempt < clusterAttempts; attempt++) {
            if (budget[0] <= 0 && budget[1] <= 0) {
                return;
            }

            BlockPos origin = findDripstoneClusterOrigin(context, featureRandom, minY, maxY);

            if (origin == null) {
                continue;
            }

            if (featureRandom.nextInt(100) < DRIPSTONE_LARGE_FEATURE_CHANCE) {
                placeLargeDripstoneFeature(context, featureRandom, origin, budget);
            }

            placeDripstoneCluster(context, featureRandom, origin, budget);
        }
    }

    private boolean hasDripstoneRegionInChunk(CaveDensityContext context) {
        for (int dx = 4; dx < CHUNK_SIZE; dx += 4) {
            for (int dz = 4; dz < CHUNK_SIZE; dz += 4) {
                if (isDripstoneRegionColumn(context, context.blockX + dx, context.blockZ + dz)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLushBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double vertical = 1.0D - Math.min(1.0D, Math.abs(pos.getY() - 34.0D) / 42.0D);
        double region = context.biomeNoise.lushRegion.getValue(pos.getX() * LUSH_REGION_SCALE, 0.0D, pos.getZ() * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushPatch.getValue(pos.getX() * 0.065D, pos.getY() * 0.085D, pos.getZ() * 0.065D);
        double caveAffinity = context.caveAffinity(pos);

        return region * 0.52D + detail * 0.22D + vertical * 0.16D + caveAffinity * 0.34D > -0.1D;
    }

    private boolean isDripstoneBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double region = context.biomeNoise.dripstoneRegion.getValue(pos.getX() * DRIPSTONE_REGION_SCALE, 0.0D, pos.getZ() * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(pos.getX() * 0.07D, pos.getY() * 0.1D, pos.getZ() * 0.07D);
        double ridged = 1.0D - Math.abs(context.biomeNoise.dripstoneRidge.getValue(pos.getX() * 0.13D, pos.getY() * 0.16D, pos.getZ() * 0.13D));
        double caveAffinity = context.caveAffinity(pos);

        return region * 0.42D + detail * 0.18D + ridged * 0.26D + caveAffinity * 0.28D > 0.14D;
    }

    private BlockPos findDripstoneClusterOrigin(CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < 36; attempt++) {
            BlockPos pos = new BlockPos(
                    context.blockX + random.nextInt(CHUNK_SIZE),
                    minY + random.nextInt(Math.max(1, maxY - minY + 1)),
                    context.blockZ + random.nextInt(CHUNK_SIZE));

            if (!isAirOrWater(context.world, pos) || !context.isUndergroundOpenSpace(pos, DRIPSTONE_MIN_SURFACE_DEPTH, DENSITY_COLUMN_OPEN_MARGIN)) {
                continue;
            }

            if (!isDripstoneBiomePatchNoise(context, pos)) {
                continue;
            }

            if (findDripstoneFloorY(context, pos, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE && findDripstoneCeilingY(context, pos, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE) {
                return pos;
            }
        }

        return null;
    }

    private void placeDripstoneCluster(CaveDensityContext context, Random random, BlockPos origin, int[] budget) {
        int xRadius = 2 + random.nextInt(7);
        int zRadius = 2 + random.nextInt(7);
        int clusterHeight = 3 + random.nextInt(4);
        double density = 0.3D + random.nextDouble() * 0.4D;
        int baseLayerThickness = 2 + random.nextInt(3);

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dz = -zRadius; dz <= zRadius; dz++) {
                if (budget[0] <= 0 && budget[1] <= 0) {
                    return;
                }

                double ellipse = (dx * dx) / (double)(xRadius * xRadius) + (dz * dz) / (double)(zRadius * zRadius);

                if (ellipse > 1.0D) {
                    continue;
                }

                BlockPos columnOrigin = origin.add(dx, 0, dz);

                if (!context.isInsideChunk(columnOrigin) || !context.isDeepEnough(columnOrigin, DRIPSTONE_MIN_SURFACE_DEPTH) || !isDripstoneBiomePatchNoise(context, columnOrigin)) {
                    continue;
                }

                double chance = getClusterColumnChance(xRadius, zRadius, dx, dz) * density;

                if (random.nextDouble() > chance) {
                    continue;
                }

                int floorY = findDripstoneFloorY(context, columnOrigin, DRIPSTONE_CLUSTER_SEARCH_RANGE);
                int ceilingY = findDripstoneCeilingY(context, columnOrigin, DRIPSTONE_CLUSTER_SEARCH_RANGE);

                if (floorY == NO_SURFACE && ceilingY == NO_SURFACE) {
                    continue;
                }

                int maxHeight = getClusterMaxHeight(clusterHeight, xRadius, zRadius, dx, dz);
                int stalactiteHeight = ceilingY == NO_SURFACE ? 0 : getClusterSpeleothemHeight(random, density, maxHeight);
                int stalagmiteHeight = floorY == NO_SURFACE ? 0 : Math.max(0, stalactiteHeight + random.nextInt(3) - 1);

                if (ceilingY != NO_SURFACE && floorY != NO_SURFACE) {
                    int caveHeight = Math.max(1, ceilingY - floorY - 1);
                    int combined = stalactiteHeight + stalagmiteHeight;

                    if (combined >= caveHeight) {
                        stalactiteHeight = Math.max(0, caveHeight / 2);
                        stalagmiteHeight = Math.max(0, caveHeight - stalactiteHeight - 1);
                    }
                }

                if (ceilingY != NO_SURFACE) {
                    placeDripstoneBaseLayer(context, new BlockPos(columnOrigin.getX(), ceilingY, columnOrigin.getZ()), EnumFacing.UP, baseLayerThickness, budget);
                    placePointedDripstoneWithBudget(context.world, new BlockPos(columnOrigin.getX(), ceilingY - 1, columnOrigin.getZ()), EnumFacing.DOWN, stalactiteHeight, budget);
                }

                if (floorY != NO_SURFACE) {
                    placeDripstoneBaseLayer(context, new BlockPos(columnOrigin.getX(), floorY, columnOrigin.getZ()), EnumFacing.DOWN, baseLayerThickness, budget);
                    placePointedDripstoneWithBudget(context.world, new BlockPos(columnOrigin.getX(), floorY + 1, columnOrigin.getZ()), EnumFacing.UP, stalagmiteHeight, budget);
                }
            }
        }
    }

    private void placeLargeDripstoneFeature(CaveDensityContext context, Random random, BlockPos origin, int[] budget) {
        int floorY = findDripstoneFloorY(context, origin, 16);
        int ceilingY = findDripstoneCeilingY(context, origin, 16);

        if (floorY == NO_SURFACE || ceilingY == NO_SURFACE) {
            return;
        }

        int caveHeight = ceilingY - floorY - 1;

        if (caveHeight < 6) {
            return;
        }

        int maxRadius = Math.max(2, Math.min(5, caveHeight / 3));
        int radius = 2 + random.nextInt(maxRadius - 1);
        double scale = 0.55D + random.nextDouble() * 1.25D;
        double stalactiteBluntness = 0.3D + random.nextDouble() * 0.6D;
        double stalagmiteBluntness = 0.4D + random.nextDouble() * 0.6D;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > radius) {
                    continue;
                }

                int stalactiteHeight = Math.min(caveHeight - 2, getLargeDripstoneHeight(distance, radius, scale, stalactiteBluntness));
                int stalagmiteHeight = Math.min(caveHeight - 2, getLargeDripstoneHeight(distance, radius, scale, stalagmiteBluntness));

                if (stalactiteHeight + stalagmiteHeight >= caveHeight) {
                    int total = Math.max(1, caveHeight - 1);
                    stalactiteHeight = total / 2;
                    stalagmiteHeight = total - stalactiteHeight;
                }

                for (int y = 0; y < stalactiteHeight && budget[0] > 0; y++) {
                    placeLargeDripstoneBlock(context, origin.add(dx, ceilingY - origin.getY() - 1 - y, dz), budget);
                }

                for (int y = 0; y < stalagmiteHeight && budget[0] > 0; y++) {
                    placeLargeDripstoneBlock(context, origin.add(dx, floorY - origin.getY() + 1 + y, dz), budget);
                }
            }
        }
    }

    private void placeLargeDripstoneBlock(CaveDensityContext context, BlockPos pos, int[] budget) {
        if (budget[0] <= 0 || !context.isInsideChunk(pos) || !isAirOrWater(context.world, pos)) {
            return;
        }

        context.world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
        budget[0]--;
    }

    private int findDripstoneFloorY(CaveDensityContext context, BlockPos pos, int range) {
        for (int dy = 0; dy <= range && pos.getY() - dy > 0; dy++) {
            BlockPos check = pos.down(dy);
            IBlockState state = context.world.getBlockState(check);

            if (isAirOrWater(context.world, check)) {
                continue;
            }

            return isNaturalCaveBlock(state.getBlock()) ? check.getY() : NO_SURFACE;
        }

        return NO_SURFACE;
    }

    private int findDripstoneCeilingY(CaveDensityContext context, BlockPos pos, int range) {
        for (int dy = 0; dy <= range && pos.getY() + dy < context.world.getActualHeight() - 1; dy++) {
            BlockPos check = pos.up(dy);
            IBlockState state = context.world.getBlockState(check);

            if (isAirOrWater(context.world, check)) {
                continue;
            }

            return isNaturalCaveBlock(state.getBlock()) ? check.getY() : NO_SURFACE;
        }

        return NO_SURFACE;
    }

    private void placeDripstoneBaseLayer(CaveDensityContext context, BlockPos firstPos, EnumFacing intoStone, int maxCount, int[] budget) {
        for (int i = 0; i < maxCount && budget[0] > 0; i++) {
            BlockPos pos = firstPos.offset(intoStone, i);
            IBlockState state = context.world.getBlockState(pos);

            if (!context.isInsideChunk(pos) || !isNaturalCaveBlock(state.getBlock())) {
                return;
            }

            context.world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
            budget[0]--;
        }
    }

    private void placePointedDripstoneWithBudget(World world, BlockPos start, EnumFacing direction, int height, int[] budget) {
        if (height <= 0 || budget[1] <= 0 || !isAirOrWater(world, start)) {
            return;
        }

        placePointedDripstone(world, start, direction, height);
        budget[1]--;
    }

    private double getClusterColumnChance(int xRadius, int zRadius, int dx, int dz) {
        int xDistanceFromEdge = xRadius - Math.abs(dx);
        int zDistanceFromEdge = zRadius - Math.abs(dz);
        int distanceFromEdge = Math.min(xDistanceFromEdge, zDistanceFromEdge);
        return 0.1D + 0.9D * Math.min(1.0D, distanceFromEdge / 3.0D);
    }

    private int getClusterMaxHeight(int clusterHeight, int xRadius, int zRadius, int dx, int dz) {
        double distanceFromCenter = Math.abs(dx) + Math.abs(dz);
        double maxDistance = Math.max(1.0D, xRadius + zRadius);
        double centerBias = 1.0D - Math.min(1.0D, distanceFromCenter / maxDistance);
        return Math.max(1, (int)Math.round(clusterHeight * (0.3D + centerBias * 0.7D)));
    }

    private int getClusterSpeleothemHeight(Random random, double density, int maxHeight) {
        if (maxHeight <= 0 || random.nextDouble() > density) {
            return 0;
        }

        return 1 + random.nextInt(maxHeight);
    }

    private int getLargeDripstoneHeight(double distance, int radius, double scale, double bluntness) {
        distance = Math.max(distance, bluntness);
        double normalized = distance / radius * 0.384D;
        double height = scale * (0.75D * Math.pow(normalized, 4.0D / 3.0D) - Math.pow(normalized, 2.0D / 3.0D) - Math.log(normalized) / 3.0D);
        return Math.max(0, (int)Math.round(Math.max(0.0D, height) / 0.384D * radius));
    }

    private void decorateDripstoneSurface(CaveDensityContext context, Random random, BlockPos pos, int[] budget) {
        World world = context.world;
        IBlockState state = world.getBlockState(pos);

        if (!context.isDeepEnough(pos, DRIPSTONE_MIN_SURFACE_DEPTH) || !isNaturalCaveBlock(state.getBlock()) || world.canSeeSky(pos)) {
            return;
        }

        BlockPos above = pos.up();
        BlockPos below = pos.down();
        boolean floorFace = world.isAirBlock(above) && context.isUndergroundOpenSpace(above, DRIPSTONE_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);
        boolean ceilingFace = world.isAirBlock(below) && context.isUndergroundOpenSpace(below, DRIPSTONE_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);

        if (!floorFace && !ceilingFace) {
            return;
        }

        if (budget[0] > 0 && random.nextInt(100) < 34) {
            world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
            budget[0]--;
        }

        if (ceilingFace && budget[1] > 0) {
            int ceilingRoll = random.nextInt(100);

            if (ceilingRoll < 4) {
                placePointedDripstone(world, below, EnumFacing.DOWN, 3 + random.nextInt(5));
                budget[1]--;
            } else if (ceilingRoll < 24) {
                placePointedDripstone(world, random, below, EnumFacing.DOWN);
                budget[1]--;
            }
        }

        if (floorFace && budget[1] > 0) {
            int floorRoll = random.nextInt(100);

            if (floorRoll < 3) {
                placePointedDripstone(world, above, EnumFacing.UP, 3 + random.nextInt(4));
                budget[1]--;
            } else if (floorRoll < 18) {
                placePointedDripstone(world, random, above, EnumFacing.UP);
                budget[1]--;
            }
        }
    }

    private BlockPos findCavePocketBelowAzalea(CaveDensityContext context, Random random, BlockPos azaleaTree) {
        int minY = LUSH_CAVE_MIN_Y;
        int maxY = Math.min(Math.min(LUSH_CAVE_MAX_Y, context.topY), azaleaTree.getY() - 8);
        minY = Math.max(minY, context.bottomY);

        if (maxY <= minY) {
            return null;
        }

        for (int attempt = 0; attempt < 144; attempt++) {
            BlockPos pos = new BlockPos(
                    azaleaTree.getX() + random.nextInt(21) - 10,
                    minY + random.nextInt(maxY - minY),
                    azaleaTree.getZ() + random.nextInt(21) - 10);

            if (!context.isInsideChunk(pos)) {
                continue;
            }

            if (isDensityCaveAir(context, pos, 3) && isLushCaveRegion(context, pos, azaleaTree)) {
                return pos;
            }
        }

        for (int y = maxY; y >= minY; y--) {
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -12; dz <= 12; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 18) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(azaleaTree.getX() + dx, y, azaleaTree.getZ() + dz);

                    if (context.isInsideChunk(pos) && isDensityCaveAir(context, pos, 3) && isLushCaveRegion(context, pos, azaleaTree)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private void placeRootTrail(World world, Random random, BlockPos azaleaTree, BlockPos lushCenter) {
        int bottomY = Math.max(lushCenter.getY() + 2, 4);

        for (int y = azaleaTree.getY() - 2; y >= bottomY; y--) {
            int spread = y - lushCenter.getY() < 14 ? 2 : 1;

            for (int dx = -spread; dx <= spread; dx++) {
                for (int dz = -spread; dz <= spread; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > spread + 1 || random.nextInt(100) >= 48) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(azaleaTree.getX() + dx, y, azaleaTree.getZ() + dz);
                    Block block = world.getBlockState(pos).getBlock();

                    if (block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.STONE || block == Blocks.GRAVEL || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.DeepSlate) {
                        world.setBlockState(pos, ModBlocks.ROOTED_DIRT.getDefaultState(), 2);

                        BlockPos rootPos = pos.down();
                        if (world.isAirBlock(rootPos) && random.nextInt(100) < 55 && ModBlocks.HANGING_ROOTS.canPlaceBlockAt(world, rootPos)) {
                            world.setBlockState(rootPos, ModBlocks.HANGING_ROOTS.getDefaultState(), 2);
                        }
                    } else if (world.isAirBlock(pos) && random.nextInt(100) < 35 && ModBlocks.HANGING_ROOTS.canPlaceBlockAt(world, pos)) {
                        world.setBlockState(pos, ModBlocks.HANGING_ROOTS.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private boolean hasNearbySolid(World world, BlockPos pos, int radius) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            for (int distance = 1; distance <= radius; distance++) {
                IBlockState state = world.getBlockState(pos.offset(facing, distance));

                if (isNaturalCaveBlock(state.getBlock())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasNearbySolid(World world, BlockPos pos, int radius, int blockX, int blockZ) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            for (int distance = 1; distance <= radius; distance++) {
                BlockPos check = pos.offset(facing, distance);

                if (!isInsideChunk(check, blockX, blockZ)) {
                    continue;
                }

                IBlockState state = world.getBlockState(check);

                if (isNaturalCaveBlock(state.getBlock()) || isLushReplaceable(state.getBlock())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isDensityCaveAir(CaveDensityContext context, BlockPos pos, int nearbySolidRadius) {
        return context.isInsideChunk(pos)
                && context.world.isAirBlock(pos)
                && !context.world.canSeeSky(pos)
                && context.isDeepEnough(pos, LUSH_MIN_SURFACE_DEPTH)
                && context.isLikelyOpen(pos, DENSITY_COLUMN_OPEN_MARGIN)
                && hasNearbySolid(context.world, pos, nearbySolidRadius, context.blockX, context.blockZ);
    }

    private void generateLushPocket(CaveDensityContext context, Random random, BlockPos azaleaTree, BlockPos center) {
        int radius = 18 + random.nextInt(7);
        int verticalRadius = 10 + random.nextInt(5);

        generateLushPocket(context, random, azaleaTree, center, radius, verticalRadius);
    }

    private void generateLushPocket(CaveDensityContext context, Random random, BlockPos azaleaTree, BlockPos center, int radius, int verticalRadius) {
        World world = context.world;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = (dx * dx) / (double)(radius * radius) + (dy * dy) / (double)(verticalRadius * verticalRadius) + (dz * dz) / (double)(radius * radius);

                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);

                    if (!context.isInsideChunk(pos) || !isLushPatchNoise(context, pos, azaleaTree, distance)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isLushReplaceable(state.getBlock()) || world.canSeeSky(pos)) {
                        continue;
                    }

                    BlockPos above = pos.up();
                    BlockPos below = pos.down();
                    boolean openAbove = isAirOrWater(world, above) && context.isUndergroundOpenSpace(above, LUSH_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);
                    boolean openBelow = world.isAirBlock(below) && context.isUndergroundOpenSpace(below, LUSH_MIN_SURFACE_DEPTH, DENSITY_DECORATION_OPEN_MARGIN);

                    if (openAbove) {
                        decorateLushFloor(world, random, pos, context.blockX, context.blockZ);
                    }

                    if (openBelow) {
                        decorateLushCeiling(world, random, pos);
                    }
                }
            }
        }

        spawnAxolotlNearWater(world, random, center, radius + 1, context.blockX, context.blockZ);
    }

    private void decorateLushFloor(World world, Random random, BlockPos pos, int blockX, int blockZ) {
        BlockPos above = pos.up();

        if (!isAirOrWater(world, above)) {
            return;
        }

        if (world.getBlockState(pos).getBlock() == Blocks.CLAY) {
            return;
        }

        if (random.nextInt(100) < LUSH_CLAY_DRIPLEAF_PATCH_CHANCE && tryPlaceClayPatchWithDripleaves(world, random, pos, blockX, blockZ)) {
            return;
        }

        int groundRoll = random.nextInt(100);

        if (groundRoll < 62) {
            world.setBlockState(pos, ModBlocks.MOSS_BLOCK.getDefaultState(), 2);
        } else if (groundRoll < 84) {
            world.setBlockState(pos, ModBlocks.ROOTED_DIRT.getDefaultState(), 2);
        } else if (groundRoll < 94) {
            world.setBlockState(pos, Blocks.CLAY.getDefaultState(), 2);
        } else {
            world.setBlockState(pos, ModBlocks.MOSS_BLOCK.getDefaultState(), 2);
        }

        if (!isAirOrWater(world, above)) {
            return;
        }

        int roll = random.nextInt(100);

        if (roll < 28 && world.isAirBlock(above)) {
            world.setBlockState(above, ModBlocks.MOSS_CARPET.getDefaultState(), 2);
        } else if (roll < 36 && world.isAirBlock(above) && ModBlocks.Azalea.canPlaceBlockAt(world, above)) {
            world.setBlockState(above, ModBlocks.Azalea.getDefaultState(), 2);
        } else if (roll < 43 && world.isAirBlock(above) && ModBlocks.Flowering_Azalea.canPlaceBlockAt(world, above)) {
            world.setBlockState(above, ModBlocks.Flowering_Azalea.getDefaultState(), 2);
        }
    }

    private boolean tryPlaceClayPatchWithDripleaves(World world, Random random, BlockPos originFloor, int blockX, int blockZ) {
        boolean waterloggedPatch = random.nextBoolean();
        int verticalRange = waterloggedPatch ? LUSH_CLAY_POOL_VERTICAL_RANGE : LUSH_CLAY_PATCH_VERTICAL_RANGE;
        float vegetationChance = waterloggedPatch ? LUSH_CLAY_POOL_VEGETATION_CHANCE : LUSH_CLAY_PATCH_VEGETATION_CHANCE;
        int xRadius = LUSH_DRIPLEAF_PATCH_RADIUS_MIN + random.nextInt(LUSH_DRIPLEAF_PATCH_RADIUS_MAX - LUSH_DRIPLEAF_PATCH_RADIUS_MIN + 1);
        int zRadius = LUSH_DRIPLEAF_PATCH_RADIUS_MIN + random.nextInt(LUSH_DRIPLEAF_PATCH_RADIUS_MAX - LUSH_DRIPLEAF_PATCH_RADIUS_MIN + 1);
        List<BlockPos> surfaceTargets = new ArrayList<BlockPos>();
        boolean placedGround = false;

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            boolean xEdge = dx == -xRadius || dx == xRadius;

            for (int dz = -zRadius; dz <= zRadius; dz++) {
                boolean zEdge = dz == -zRadius || dz == zRadius;
                boolean corner = xEdge && zEdge;
                boolean edge = xEdge || zEdge;

                if (corner || edge && random.nextFloat() > LUSH_DRIPLEAF_PATCH_EDGE_CHANCE) {
                    continue;
                }

                BlockPos surfaceFloor = findLushPatchFloor(world, originFloor.add(dx, 0, dz), verticalRange, blockX, blockZ);

                if (surfaceFloor == null || surfaceTargets.contains(surfaceFloor)) {
                    continue;
                }

                BlockPos openPos = surfaceFloor.up();

                if (!waterloggedPatch && !world.isAirBlock(openPos)) {
                    continue;
                }

                int depth = LUSH_DRIPLEAF_PATCH_BASE_DEPTH + (random.nextFloat() < LUSH_DRIPLEAF_PATCH_EXTRA_BOTTOM_CHANCE ? 1 : 0);

                if (placeClayGround(world, surfaceFloor, depth)) {
                    surfaceTargets.add(surfaceFloor);
                    placedGround = true;
                }
            }
        }

        if (!placedGround) {
            return false;
        }

        if (waterloggedPatch) {
            List<BlockPos> waterTargets = new ArrayList<BlockPos>();

            for (BlockPos surfaceFloor : surfaceTargets) {
                if (isClayPatchSurfaceExposed(world, surfaceFloor, blockX, blockZ) || isNearLava(world, surfaceFloor, 2, blockX, blockZ)) {
                    continue;
                }

                waterTargets.add(surfaceFloor);
            }

            for (BlockPos surfaceFloor : waterTargets) {
                world.setBlockState(surfaceFloor, Blocks.WATER.getDefaultState(), 2);

                if (random.nextFloat() < vegetationChance) {
                    placeDripleafFeature(world, random, surfaceFloor, blockX, blockZ);
                }
            }
        } else {
            for (BlockPos surfaceFloor : surfaceTargets) {
                BlockPos vegetationPos = surfaceFloor.up();

                if (random.nextFloat() < vegetationChance) {
                    placeDripleafFeature(world, random, vegetationPos, blockX, blockZ);
                }
            }
        }

        return true;
    }

    private BlockPos findLushPatchFloor(World world, BlockPos origin, int verticalRange, int blockX, int blockZ) {
        if (!isInsideChunk(origin, blockX, blockZ)) {
            return null;
        }

        BlockPos scan = origin;
        int offset = 0;

        while (offset < verticalRange && isInsideChunk(scan, blockX, blockZ) && isAirOrWater(world, scan)) {
            scan = scan.down();
            offset++;
        }

        offset = 0;

        while (offset < verticalRange && isInsideChunk(scan, blockX, blockZ) && !isAirOrWater(world, scan)) {
            scan = scan.up();
            offset++;
        }

        if (!isInsideChunk(scan, blockX, blockZ) || !isAirOrWater(world, scan) || world.canSeeSky(scan)) {
            return null;
        }

        BlockPos floor = scan.down();

        if (!isInsideChunk(floor, blockX, blockZ) || world.canSeeSky(floor) || !isLushReplaceable(world.getBlockState(floor).getBlock()) || !world.isSideSolid(floor, EnumFacing.UP, true)) {
            return null;
        }

        return floor;
    }

    private boolean placeClayGround(World world, BlockPos floor, int depth) {
        boolean placedAny = false;

        for (int i = 0; i < depth; i++) {
            BlockPos groundPos = floor.down(i);

            if (groundPos.getY() <= 0) {
                break;
            }

            Block block = world.getBlockState(groundPos).getBlock();

            if (block == Blocks.CLAY) {
                placedAny = true;
                continue;
            }

            if (!isLushReplaceable(block)) {
                return placedAny;
            }

            world.setBlockState(groundPos, Blocks.CLAY.getDefaultState(), 2);
            placedAny = true;
        }

        return placedAny;
    }

    private boolean isClayPatchSurfaceExposed(World world, BlockPos surfaceFloor, int blockX, int blockZ) {
        for (EnumFacing facing : DRIPLEAF_FEATURE_FACINGS) {
            if (!isSolidFace(world, surfaceFloor.offset(facing), facing.getOpposite(), blockX, blockZ)) {
                return true;
            }
        }

        return !isSolidFace(world, surfaceFloor.down(), EnumFacing.UP, blockX, blockZ);
    }

    private boolean isSolidFace(World world, BlockPos pos, EnumFacing side, int blockX, int blockZ) {
        return isInsideChunk(pos, blockX, blockZ) && world.getBlockState(pos).isSideSolid(world, pos, side);
    }

    private boolean placeDripleafFeature(World world, Random random, BlockPos basePos, int blockX, int blockZ) {
        int choice = random.nextInt(5);

        if (choice == 0) {
            return placeSmallDripleaf(world, random, basePos, blockX, blockZ);
        }

        return placeBigDripleafColumn(world, random, basePos, DRIPLEAF_FEATURE_FACINGS[choice - 1], blockX, blockZ);
    }

    private boolean placeSmallDripleaf(World world, Random random, BlockPos basePos, int blockX, int blockZ) {
        if (!canSupportSmallDripleaf(world.getBlockState(basePos.down()).getBlock())) {
            return false;
        }

        if (canPlaceDripleafPartAt(world, basePos, blockX, blockZ) && canPlaceDripleafPartAt(world, basePos.up(), blockX, blockZ)) {
            ((SmallDripleaf)ModBlocks.SMALL_DRIPLEAF).placeAt(world, basePos, EnumFacing.byHorizontalIndex(random.nextInt(4)), 2);
            return true;
        }

        return false;
    }

    private boolean placeBigDripleafColumn(World world, Random random, BlockPos basePos, EnumFacing facing, int blockX, int blockZ) {
        if (!canSupportDripleaf(world.getBlockState(basePos.down()).getBlock())) {
            return false;
        }

        int stemHeight = random.nextInt(3) == 0 ? 0 : random.nextInt(5);
        int totalHeight = stemHeight + 1;
        int availableHeight = 0;

        for (int y = 0; y < totalHeight; y++) {
            BlockPos check = basePos.up(y);

            if (!canPlaceDripleafPartAt(world, check, blockX, blockZ)) {
                break;
            }

            availableHeight++;
        }

        if (availableHeight == 0) {
            return false;
        }

        if (availableHeight < totalHeight) {
            stemHeight = Math.max(0, availableHeight - 1);
        }

        for (int y = 0; y < stemHeight; y++) {
            setFluidloggableBlock(world, basePos.up(y), ModBlocks.DRIPLEAF_STEM.getDefaultState().withProperty(DripleafStem.FACING, facing), 2);
        }

        setFluidloggableBlock(world, basePos.up(stemHeight), ModBlocks.BIG_DRIPLEAF.getDefaultState().withProperty(BigDripleaf.FACING, facing), 2);
        return true;
    }

    private boolean canPlaceDripleafPartAt(World world, BlockPos pos, int blockX, int blockZ) {
        return isInsideChunk(pos, blockX, blockZ) && isAirOrWater(world, pos);
    }

    private boolean isAirOrWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER;
    }

    private void setFluidloggableBlock(World world, BlockPos pos, IBlockState newState, int flags) {
        if (hasWaterFluid(world, pos)) {
            world.setBlockState(pos, newState, flags);
            FluidloggedUtils.setFluidState(world, pos, world.getBlockState(pos), FluidState.of(FluidRegistry.WATER), false, flags);
        } else {
            world.setBlockState(pos, newState, flags);
        }
    }

    private boolean hasWaterFluid(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() == Material.WATER || FluidloggedUtils.getFluidState(world, pos, state).getFluid() == FluidRegistry.WATER;
    }

    private boolean canSupportDripleaf(Block block) {
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.ROOTED_DIRT || block == Blocks.GRASS || block == Blocks.DIRT || block == Blocks.MYCELIUM || block == Blocks.FARMLAND;
    }

    private boolean canSupportSmallDripleaf(Block block) {
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK;
    }

    private void decorateLushCeiling(World world, Random random, BlockPos pos) {
        BlockPos below = pos.down();

        if (!world.isAirBlock(below)) {
            return;
        }

        int roll = random.nextInt(100);

        if (roll < 10 && ModBlocks.SPORE_BLOSSOM.canPlaceBlockAt(world, below)) {
            world.setBlockState(below, ModBlocks.SPORE_BLOSSOM.getDefaultState(), 2);
        } else if (roll < 42 && ModBlocks.HANGING_ROOTS.canPlaceBlockAt(world, below)) {
            world.setBlockState(below, ModBlocks.HANGING_ROOTS.getDefaultState(), 2);
        } else if (roll < 72) {
            placeCaveVine(world, random, below);
        }
    }

    private void placeCaveVine(World world, Random random, BlockPos start) {
        int length = 2 + random.nextInt(8);

        for (int i = 0; i < length; i++) {
            BlockPos pos = start.down(i);

            if (!world.isAirBlock(pos)) {
                break;
            }

            boolean bottom = i == length - 1 || !world.isAirBlock(pos.down());
            boolean berries = random.nextFloat() < 0.22F;

            if (bottom) {
                world.setBlockState(pos, ModBlocks.CAVE_VINE.getDefaultState().withProperty(CaveVine.AGE, random.nextInt(2)).withProperty(CaveVinePlant.BERRIES, berries), 2);
                break;
            } else {
                world.setBlockState(pos, ModBlocks.CAVE_VINE_PLANT.getDefaultState().withProperty(CaveVinePlant.BERRIES, berries), 2);
            }
        }
    }

    private void spawnAxolotlNearWater(World world, Random random, BlockPos center, int radius, int blockX, int blockZ) {
        if (random.nextInt(3) != 0) {
            return;
        }

        AxisAlignedBB checkBox = new AxisAlignedBB(blockX, center.getY() - radius, blockZ, blockX + CHUNK_SIZE, center.getY() + radius, blockZ + CHUNK_SIZE);
        List<EntityAxolotl> existing = world.getEntitiesWithinAABB(EntityAxolotl.class, checkBox);

        if (!existing.isEmpty()) {
            return;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos pos = center.add(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);

            if (!isInsideChunk(pos, blockX, blockZ)) {
                continue;
            }

            if (world.getBlockState(pos).getMaterial() == Material.WATER && isLushReplaceable(world.getBlockState(pos.down()).getBlock())) {
                EntityAxolotl axolotl = new EntityAxolotl(world);
                axolotl.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                axolotl.onInitialSpawn(world.getDifficultyForLocation(pos), (IEntityLivingData)null);
                world.spawnEntity(axolotl);
                return;
            }
        }
    }

    private void placePointedDripstone(World world, Random random, BlockPos start, EnumFacing direction) {
        int length = 1 + random.nextInt(random.nextBoolean() ? 3 : 5);
        placePointedDripstone(world, start, direction, length);
    }

    private void placePointedDripstone(World world, BlockPos start, EnumFacing direction, int length) {
        ((PointedDripstoneBlock)ModBlocks.POINTED_DRIPSTONE).placeColumn(world, start, direction, length, 2);
    }

    private boolean isNearWater(World world, BlockPos pos, int radius, int blockX, int blockZ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos check = pos.add(dx, dy, dz);

                    if (isInsideChunk(check, blockX, blockZ) && world.getBlockState(check).getMaterial() == Material.WATER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isNearLava(World world, BlockPos pos, int radius, int blockX, int blockZ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    BlockPos check = pos.add(dx, dy, dz);

                    if (!isInsideChunk(check, blockX, blockZ)) {
                        continue;
                    }

                    if (world.getBlockState(check).getMaterial() == Material.LAVA) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isInsideChunk(BlockPos pos, int blockX, int blockZ) {
        return pos.getX() >= blockX && pos.getX() < blockX + CHUNK_SIZE && pos.getZ() >= blockZ && pos.getZ() < blockZ + CHUNK_SIZE;
    }

    private boolean isLushRegionChunk(CaveDensityContext context) {
        int chunkCenterX = context.blockX + CHUNK_SIZE / 2;
        int chunkCenterZ = context.blockZ + CHUNK_SIZE / 2;

        if (chunkCenterX * chunkCenterX + chunkCenterZ * chunkCenterZ < MIN_LUSH_CAVE_DISTANCE_SQ) {
            return false;
        }

        for (int dx = 4; dx < CHUNK_SIZE; dx += 4) {
            for (int dz = 4; dz < CHUNK_SIZE; dz += 4) {
                if (isLushRegionColumn(context, context.blockX + dx, context.blockZ + dz)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLushRegionColumn(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.lushRegion.getValue(x * LUSH_REGION_SCALE, 0.0D, z * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushDetail.getValue(x * 0.036D, 0.0D, z * 0.036D);
        return region * 0.8D + detail * 0.2D > LUSH_REGION_THRESHOLD && hasBetterCavesDensityOpening(context, x, z, LUSH_CAVE_MIN_Y, LUSH_CAVE_MAX_Y);
    }

    private boolean isDripstoneRegionColumn(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.dripstoneRegion.getValue(x * DRIPSTONE_REGION_SCALE, 0.0D, z * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(x * 0.033D, 0.0D, z * 0.033D);
        return region * 0.72D + detail * 0.28D > DRIPSTONE_REGION_THRESHOLD && hasBetterCavesDensityOpening(context, x, z, DRIPSTONE_CAVE_MIN_Y, DRIPSTONE_CAVE_MAX_Y);
    }

    private boolean hasBetterCavesDensityOpening(CaveDensityContext context, int x, int z, int minY, int maxY) {
        minY = Math.max(minY, context.bottomY);
        maxY = Math.min(maxY, context.topY);

        for (int y = minY; y <= maxY; y += 8) {
            if (context.isLikelyOpen(new BlockPos(x, y, z), DENSITY_COLUMN_OPEN_MARGIN)) {
                return true;
            }
        }

        return false;
    }

    private boolean isLushCaveRegion(CaveDensityContext context, BlockPos pos, BlockPos azaleaTree) {
        double vertical = 1.0D - Math.min(1.0D, Math.abs(pos.getY() - 34.0D) / 42.0D);
        double horizontalFalloff = horizontalFalloff(pos, azaleaTree, 44.0D);
        double region = context.biomeNoise.lushRegion.getValue(pos.getX() * LUSH_REGION_SCALE, 0.0D, pos.getZ() * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushDetail.getValue(pos.getX() * 0.046D, pos.getY() * 0.05D, pos.getZ() * 0.046D);
        double caveAffinity = context.caveAffinity(pos);
        return region * 0.58D + detail * 0.18D + vertical * 0.14D + horizontalFalloff * 0.38D + caveAffinity * 0.32D > 0.12D;
    }

    private boolean isLushPatchNoise(CaveDensityContext context, BlockPos pos, BlockPos azaleaTree, double normalizedDistance) {
        double main = context.biomeNoise.lushPatch.getValue(pos.getX() * 0.055D, pos.getY() * 0.08D, pos.getZ() * 0.055D);
        double detail = context.biomeNoise.lushPatchDetail.getValue(pos.getX() * 0.13D, pos.getY() * 0.18D, pos.getZ() * 0.13D);
        double edgeFalloff = 1.0D - normalizedDistance;
        double horizontalFalloff = horizontalFalloff(pos, azaleaTree, 48.0D);
        double caveAffinity = context.caveAffinity(pos);
        return isLushCaveRegion(context, pos, azaleaTree) && main * 0.48D + detail * 0.2D + edgeFalloff * 0.38D + horizontalFalloff * 0.28D + caveAffinity * 0.28D > -0.08D;
    }

    private double horizontalFalloff(BlockPos pos, BlockPos center, double radius) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double distanceSq = dx * dx + dz * dz;
        return 1.0D - Math.min(1.0D, distanceSq / (radius * radius));
    }

    private Random createFeatureRandom(World world, long salt, int cellX, int cellZ) {
        long seed = world.getSeed() ^ salt;
        seed ^= cellX * 341873128712L;
        seed ^= cellZ * 132897987541L;
        seed ^= seed >> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >> 33;
        return new Random(seed);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isLushReplaceable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.GRAVEL || block == Blocks.CLAY || block == ModBlocks.DeepSlate || block == ModBlocks.ROOTED_DIRT || block == ModBlocks.MOSS_BLOCK;
    }

    private boolean isNaturalCaveBlock(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.COBBLESTONE || block == ModBlocks.DeepSlate || block == ModBlocks.DRIPSTONE_BLOCK;
    }
}
