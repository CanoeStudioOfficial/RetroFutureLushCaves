package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.PointedDripstoneBlock;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesAPI;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesConfig;
import com.yungnickyoung.minecraft.bettercaves.noise.MojangNormalNoise;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.mojang.Mojang118CaveDensitySampler;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class RetroFutureCaveWorldGenerator implements IWorldGenerator {
    static final int CHUNK_SIZE = 16;
    static final int LUSH_CAVE_MIN_Y = 8;
    static final int LUSH_CAVE_MAX_Y = 62;
    static final int DRIPSTONE_CAVE_MIN_Y = 8;
    static final int DRIPSTONE_CAVE_MAX_Y = 58;
    static final int LUSH_MIN_SURFACE_DEPTH = 12;
    static final int DRIPSTONE_MIN_SURFACE_DEPTH = 12;
    static final int DRIPSTONE_CLUSTER_SEARCH_RANGE = 12;
    static final int DRIPSTONE_CLUSTER_MIN_ATTEMPTS = 4;
    static final int DRIPSTONE_CLUSTER_MAX_ATTEMPTS = 6;
    static final int DRIPSTONE_LARGE_FEATURE_CHANCE = 24;
    static final int DRIPSTONE_MAX_BLOCKS_PER_CHUNK = 180;
    static final int DRIPSTONE_MAX_COLUMNS_PER_CHUNK = 72;
    static final int LUSH_MOSS_PATCH_ATTEMPTS = 54;
    static final int LUSH_CEILING_MOSS_PATCH_ATTEMPTS = 42;
    static final int LUSH_CLAY_PATCH_ATTEMPTS = 76;
    static final int LUSH_CAVE_VINE_ATTEMPTS = 72;
    static final int LUSH_SPORE_BLOSSOM_ATTEMPTS = 26;
    static final int LUSH_AXOLOTL_GROUP_CHANCE = 5;
    static final int LUSH_AXOLOTL_CLAY_POOL_CHANCE = 8;
    static final int LUSH_AXOLOTL_MIN_GROUP_SIZE = 4;
    static final int LUSH_AXOLOTL_MAX_GROUP_SIZE = 6;
    static final int LUSH_MOSS_PATCH_RADIUS_MIN = 4;
    static final int LUSH_MOSS_PATCH_RADIUS_MAX = 7;
    static final int LUSH_MOSS_PATCH_DEPTH = 1;
    static final int LUSH_CEILING_MOSS_PATCH_DEPTH_MIN = 1;
    static final int LUSH_CEILING_MOSS_PATCH_DEPTH_MAX = 2;
    static final int LUSH_MOSS_PATCH_VERTICAL_RANGE = 20;
    static final float LUSH_MOSS_PATCH_EDGE_CHANCE = 0.3F;
    static final float LUSH_MOSS_PATCH_VEGETATION_CHANCE = 0.72F;
    static final float LUSH_CEILING_MOSS_PATCH_VINE_CHANCE = 0.08F;
    static final int LUSH_DRIPLEAF_PATCH_RADIUS_MIN = 4;
    static final int LUSH_DRIPLEAF_PATCH_RADIUS_MAX = 7;
    static final int LUSH_DRIPLEAF_PATCH_BASE_DEPTH = 3;
    static final float LUSH_DRIPLEAF_PATCH_EXTRA_BOTTOM_CHANCE = 0.8F;
    static final float LUSH_DRIPLEAF_PATCH_EDGE_CHANCE = 0.7F;
    static final int LUSH_CLAY_PATCH_VERTICAL_RANGE = 2;
    static final int LUSH_CLAY_POOL_VERTICAL_RANGE = 5;
    static final float LUSH_CLAY_PATCH_VEGETATION_CHANCE = 0.05F;
    static final float LUSH_CLAY_POOL_VEGETATION_CHANCE = 0.1F;
    static final double CAVE_TYPE_SELECTOR_SCALE = 0.006D;
    static final double LUSH_TYPE_SELECTOR_MAX = -0.03D;
    static final double DRIPSTONE_TYPE_SELECTOR_MIN = 0.03D;
    static final double LUSH_REGION_SCALE = 0.01D;
    static final double LUSH_REGION_THRESHOLD = -0.12D;
    static final double DRIPSTONE_REGION_SCALE = 0.01D;
    static final double DRIPSTONE_REGION_THRESHOLD = -0.1D;
    static final double CAVE_REGION_TYPE_MARGIN = 0.12D;
    static final double DENSITY_COLUMN_OPEN_MARGIN = 0.24D;
    static final double DENSITY_DECORATION_OPEN_MARGIN = 0.34D;
    static final long LUSH_PATCH_SALT = 0x4C55534843415645L;
    static final long DRIPSTONE_PATCH_SALT = 0x4452495053544F4EL;
    static final int NO_SURFACE = -1;
    static final EnumFacing[] DRIPLEAF_FEATURE_FACINGS = new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.NORTH};
    private BetterCavesConfig cachedBetterCavesConfig;
    private int cachedBetterCavesConfigDimension = Integer.MIN_VALUE;
    private Mojang118CaveDensitySampler cachedDensitySampler;
    private long cachedDensitySeed = Long.MIN_VALUE;
    private float cachedDensityHorizontalScale;
    private float cachedDensityVerticalScale;
    private CaveBiomeNoise cachedBiomeNoise;
    private long cachedBiomeNoiseSeed = Long.MIN_VALUE;
    private final RetroFutureLushCaveGenerator lushCaveGenerator = new RetroFutureLushCaveGenerator();
    private final RetroFutureDripstoneCaveGenerator dripstoneCaveGenerator = new RetroFutureDripstoneCaveGenerator();

    enum CaveRegionType {
        NONE,
        LUSH,
        DRIPSTONE
    }

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

        lushCaveGenerator.generate(this, caveContext);
        dripstoneCaveGenerator.generate(this, caveContext);
    }

    private CaveDensityContext createCaveDensityContext(World world, int blockX, int blockZ) {
        BetterCavesConfig betterCavesConfig = getBetterCavesConfig(world);

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

    static final class CaveBiomeNoise {
        private final MojangNormalNoise caveTypeSelector;
        private final MojangNormalNoise lushRegion;
        private final MojangNormalNoise lushDetail;
        private final MojangNormalNoise lushPatch;
        private final MojangNormalNoise lushPatchDetail;
        private final MojangNormalNoise dripstoneRegion;
        private final MojangNormalNoise dripstoneDetail;
        private final MojangNormalNoise dripstoneRidge;

        private CaveBiomeNoise(long seed) {
            this.caveTypeSelector = MojangNormalNoise.create(seed, "retro_cave_type_selector", -8, 1.0D);
            this.lushRegion = MojangNormalNoise.create(seed, "retro_lush_caves_region", -8, 1.0D);
            this.lushDetail = MojangNormalNoise.create(seed, "retro_lush_caves_detail", -7, 1.0D);
            this.lushPatch = MojangNormalNoise.create(seed, "retro_lush_caves_patch", -7, 1.0D);
            this.lushPatchDetail = MojangNormalNoise.create(seed, "retro_lush_caves_patch_detail", -6, 1.0D);
            this.dripstoneRegion = MojangNormalNoise.create(seed, "retro_dripstone_caves_region", -8, 1.0D);
            this.dripstoneDetail = MojangNormalNoise.create(seed, "retro_dripstone_caves_detail", -7, 1.0D);
            this.dripstoneRidge = MojangNormalNoise.create(seed, "retro_dripstone_caves_ridge", -6, 1.0D);
        }
    }

    final class CaveDensityContext {
        final World world;
        final int blockX;
        final int blockZ;
        private final Mojang118CaveDensitySampler densitySampler;
        private final CaveBiomeNoise biomeNoise;
        final int bottomY;
        final int topY;
        private final int surfaceCutoff;
        private final double densityThreshold;
        private final int[] surfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];
        private final int[] terrainSurfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];
        private final CaveRegionType[] caveRegionTypeCache = new CaveRegionType[CHUNK_SIZE * CHUNK_SIZE];

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

        boolean containsY(int y) {
            return y >= bottomY && y <= topY;
        }

        boolean isLikelyOpen(BlockPos pos, double margin) {
            return containsY(pos.getY()) && sampleCarverDensity(pos) <= densityThreshold + margin;
        }

        boolean isDeepEnough(BlockPos pos, int minSurfaceDepth) {
            return getTerrainSurfaceY(pos.getX(), pos.getZ()) - pos.getY() >= minSurfaceDepth;
        }

        boolean isUndergroundOpenSpace(BlockPos pos, int minSurfaceDepth, double margin) {
            return isDeepEnough(pos, minSurfaceDepth) && !world.canSeeSky(pos) && isLikelyOpen(pos, margin);
        }

        double caveAffinity(BlockPos pos) {
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

        boolean isInsideChunk(BlockPos pos) {
            return RetroFutureCaveWorldGenerator.this.isInsideChunk(pos, blockX, blockZ);
        }

        boolean isInsideChunk(int x, int z) {
            return x >= blockX && x < blockX + CHUNK_SIZE && z >= blockZ && z < blockZ + CHUNK_SIZE;
        }
    }

    boolean isLushBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double vertical = 1.0D - Math.min(1.0D, Math.abs(pos.getY() - 34.0D) / 42.0D);
        double region = context.biomeNoise.lushRegion.getValue(pos.getX() * LUSH_REGION_SCALE, 0.0D, pos.getZ() * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushPatch.getValue(pos.getX() * 0.065D, pos.getY() * 0.085D, pos.getZ() * 0.065D);
        double caveAffinity = context.caveAffinity(pos);

        return getCaveRegionType(context, pos.getX(), pos.getZ()) == CaveRegionType.LUSH
                && region * 0.36D + detail * 0.2D + vertical * 0.2D + caveAffinity * 0.24D > -0.28D;
    }

    boolean isDripstoneBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double region = context.biomeNoise.dripstoneRegion.getValue(pos.getX() * DRIPSTONE_REGION_SCALE, 0.0D, pos.getZ() * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(pos.getX() * 0.07D, pos.getY() * 0.1D, pos.getZ() * 0.07D);
        double ridged = 1.0D - Math.abs(context.biomeNoise.dripstoneRidge.getValue(pos.getX() * 0.13D, pos.getY() * 0.16D, pos.getZ() * 0.13D));
        double caveAffinity = context.caveAffinity(pos);

        return getCaveRegionType(context, pos.getX(), pos.getZ()) == CaveRegionType.DRIPSTONE
                && region * 0.32D + detail * 0.16D + ridged * 0.26D + caveAffinity * 0.22D > -0.08D;
    }

    boolean isAirOrWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER;
    }

    void setFluidloggableBlock(World world, BlockPos pos, IBlockState newState, int flags) {
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

    void placePointedDripstone(World world, Random random, BlockPos start, EnumFacing direction) {
        int length = 1 + random.nextInt(random.nextBoolean() ? 3 : 5);
        placePointedDripstone(world, start, direction, length);
    }

    void placePointedDripstone(World world, BlockPos start, EnumFacing direction, int length) {
        ((PointedDripstoneBlock)ModBlocks.POINTED_DRIPSTONE).placeColumn(world, start, direction, length, 2);
    }

    boolean isNearLava(World world, BlockPos pos, int radius, int blockX, int blockZ) {
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

    boolean isInsideChunk(BlockPos pos, int blockX, int blockZ) {
        return pos.getX() >= blockX && pos.getX() < blockX + CHUNK_SIZE && pos.getZ() >= blockZ && pos.getZ() < blockZ + CHUNK_SIZE;
    }

    boolean isLushRegionColumn(CaveDensityContext context, int x, int z) {
        return getCaveRegionType(context, x, z) == CaveRegionType.LUSH;
    }

    CaveRegionType getChunkCaveRegionType(CaveDensityContext context) {
        int lushColumns = 0;
        int dripstoneColumns = 0;

        for (int dx = 2; dx < CHUNK_SIZE; dx += 4) {
            for (int dz = 2; dz < CHUNK_SIZE; dz += 4) {
                CaveRegionType type = getCaveRegionType(context, context.blockX + dx, context.blockZ + dz);

                if (type == CaveRegionType.LUSH) {
                    lushColumns++;
                } else if (type == CaveRegionType.DRIPSTONE) {
                    dripstoneColumns++;
                }
            }
        }

        if (lushColumns >= 3 && lushColumns > dripstoneColumns) {
            return CaveRegionType.LUSH;
        }

        if (dripstoneColumns >= 3 && dripstoneColumns > lushColumns) {
            return CaveRegionType.DRIPSTONE;
        }

        if (lushColumns > 0 && dripstoneColumns == 0) {
            return CaveRegionType.LUSH;
        }

        if (dripstoneColumns > 0 && lushColumns == 0) {
            return CaveRegionType.DRIPSTONE;
        }

        return CaveRegionType.NONE;
    }

    private CaveRegionType getCaveRegionType(CaveDensityContext context, int x, int z) {
        if (context.isInsideChunk(x, z)) {
            int index = (x - context.blockX) * CHUNK_SIZE + z - context.blockZ;
            CaveRegionType cached = context.caveRegionTypeCache[index];

            if (cached != null) {
                return cached;
            }

            cached = computeCaveRegionType(context, x, z);
            context.caveRegionTypeCache[index] = cached;
            return cached;
        }

        return computeCaveRegionType(context, x, z);
    }

    private CaveRegionType computeCaveRegionType(CaveDensityContext context, int x, int z) {
        double lushScore = getLushRegionScore(context, x, z);
        double dripstoneScore = getDripstoneRegionScore(context, x, z);
        boolean canLush = lushScore >= LUSH_REGION_THRESHOLD;
        boolean canDripstone = dripstoneScore >= DRIPSTONE_REGION_THRESHOLD;

        if (!canLush && !canDripstone) {
            return CaveRegionType.NONE;
        }

        double selector = context.biomeNoise.caveTypeSelector.getValue(x * CAVE_TYPE_SELECTOR_SCALE, 0.0D, z * CAVE_TYPE_SELECTOR_SCALE);

        if (canLush && selector <= LUSH_TYPE_SELECTOR_MAX && (!canDripstone || lushScore >= dripstoneScore - CAVE_REGION_TYPE_MARGIN)) {
            return CaveRegionType.LUSH;
        }

        if (canDripstone && selector >= DRIPSTONE_TYPE_SELECTOR_MIN) {
            return CaveRegionType.DRIPSTONE;
        }

        if (canLush && lushScore >= dripstoneScore + CAVE_REGION_TYPE_MARGIN) {
            return CaveRegionType.LUSH;
        }

        if (canDripstone && dripstoneScore >= lushScore + CAVE_REGION_TYPE_MARGIN) {
            return CaveRegionType.DRIPSTONE;
        }

        return CaveRegionType.NONE;
    }

    private double getLushRegionScore(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.lushRegion.getValue(x * LUSH_REGION_SCALE, 0.0D, z * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushDetail.getValue(x * 0.036D, 0.0D, z * 0.036D);
        return region * 0.74D + detail * 0.26D;
    }

    private double getDripstoneRegionScore(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.dripstoneRegion.getValue(x * DRIPSTONE_REGION_SCALE, 0.0D, z * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(x * 0.033D, 0.0D, z * 0.033D);
        return region * 0.72D + detail * 0.28D;
    }

    Random createFeatureRandom(World world, long salt, int cellX, int cellZ) {
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

    boolean isLushReplaceable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.GRAVEL || block == Blocks.CLAY || block == ModBlocks.DeepSlate || block == ModBlocks.ROOTED_DIRT || block == ModBlocks.MOSS_BLOCK;
    }

    boolean isMossReplaceable(Block block) {
        return block == Blocks.STONE
                || block == Blocks.DIRT
                || block == Blocks.GRASS
                || block == Blocks.MYCELIUM
                || block == ModBlocks.DeepSlate
                || block == ModBlocks.ROOTED_DIRT
                || block == ModBlocks.MOSS_BLOCK
                || block == ModBlocks.CAVE_VINE
                || block == ModBlocks.CAVE_VINE_PLANT;
    }

    boolean isNaturalCaveBlock(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.COBBLESTONE || block == ModBlocks.DeepSlate || block == ModBlocks.DRIPSTONE_BLOCK;
    }
}
