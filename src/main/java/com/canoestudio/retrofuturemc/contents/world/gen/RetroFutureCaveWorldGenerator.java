package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.PointedDripstoneBlock;
import com.canoestudio.retrofuturemc.contents.world.gen.noise.RetroFutureCaveNoises;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class RetroFutureCaveWorldGenerator implements IWorldGenerator {
    static final int CHUNK_SIZE = 16;
    static final int CHUNK_MARGIN = 4;
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
    static final int LUSH_MOSS_PATCH_ATTEMPTS = 125;
    static final int LUSH_CEILING_MOSS_PATCH_ATTEMPTS = 125;
    static final int LUSH_CLAY_PATCH_ATTEMPTS = 62;
    static final int LUSH_CAVE_VINE_ATTEMPTS = 188;
    static final int LUSH_SPORE_BLOSSOM_ATTEMPTS = 25;
    static final int LUSH_AXOLOTL_GROUP_CHANCE = 5;
    static final int LUSH_AXOLOTL_CLAY_POOL_CHANCE = 8;
    static final int LUSH_AXOLOTL_MIN_GROUP_SIZE = 4;
    static final int LUSH_AXOLOTL_MAX_GROUP_SIZE = 6;
    static final int LUSH_MOSS_PATCH_RADIUS_MIN = 4;
    static final int LUSH_MOSS_PATCH_RADIUS_MAX = 7;
    static final int LUSH_MOSS_PATCH_DEPTH = 1;
    static final int LUSH_CEILING_MOSS_PATCH_DEPTH_MIN = 1;
    static final int LUSH_CEILING_MOSS_PATCH_DEPTH_MAX = 2;
    static final int LUSH_MOSS_PLACEMENT_SCAN_RANGE = 12;
    static final int LUSH_MOSS_PATCH_VERTICAL_RANGE = 5;
    static final float LUSH_MOSS_PATCH_EDGE_CHANCE = 0.3F;
    static final float LUSH_MOSS_PATCH_VEGETATION_CHANCE = 0.8F;
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
    static final double CAVE_TYPE_SELECTOR_SCALE = 0.7D;
    static final double LUSH_REGION_SCALE = 0.55D;
    static final double LUSH_REGION_THRESHOLD = -0.14D;
    static final double DRIPSTONE_REGION_SCALE = 0.55D;
    static final double DRIPSTONE_REGION_THRESHOLD = -0.1D;
    static final double CAVE_REGION_TYPE_MARGIN = 0.04D;
    static final int CAVE_DECORATION_BOTTOM_Y = 4;
    static final int CAVE_DECORATION_TOP_Y = 72;
    static final long LUSH_PATCH_SALT = 0x4C55534843415645L;
    static final long DRIPSTONE_PATCH_SALT = 0x4452495053544F4EL;
    static final int NO_SURFACE = -1;
    static final EnumFacing[] DRIPLEAF_FEATURE_FACINGS = new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.NORTH};
    private static final int STRUCTURE_PROTECTION_MARGIN = 12;
    private static final String[] PROTECTED_STRUCTURE_DATA_NAMES = new String[] {"Village", "Temple", "Mansion", "Monument", "Stronghold", "Mineshaft"};
    private RetroFutureCaveNoises cachedBiomeNoise;
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

        int blockX = chunkX * CHUNK_SIZE;
        int blockZ = chunkZ * CHUNK_SIZE;

        if (isMostlyProtectedSurfaceBiomeChunk(world, blockX, blockZ) || intersectsProtectedStructure(world, blockX, blockZ)) {
            return;
        }

        CaveDensityContext caveContext = createCaveDensityContext(world, blockX, blockZ);

        lushCaveGenerator.generate(this, caveContext);
        dripstoneCaveGenerator.generate(this, caveContext);
    }

    private CaveDensityContext createCaveDensityContext(World world, int blockX, int blockZ) {
        return new CaveDensityContext(world, blockX, blockZ);
    }

    private RetroFutureCaveNoises getBiomeNoise(World world) {
        if (cachedBiomeNoise == null || cachedBiomeNoiseSeed != world.getSeed()) {
            cachedBiomeNoiseSeed = world.getSeed();
            cachedBiomeNoise = new RetroFutureCaveNoises(world.getSeed());
        }

        return cachedBiomeNoise;
    }

    final class CaveDensityContext {
        final World world;
        final int blockX;
        final int blockZ;
        private final RetroFutureCaveNoises biomeNoise;
        final int bottomY;
        final int topY;
        private final int[] surfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];
        private final int[] terrainSurfaceYCache = new int[CHUNK_SIZE * CHUNK_SIZE];
        private final byte[] protectedSurfaceColumnCache = new byte[CHUNK_SIZE * CHUNK_SIZE];
        private final CaveRegionType[] caveRegionTypeCache = new CaveRegionType[CHUNK_SIZE * CHUNK_SIZE];

        private CaveDensityContext(World world, int blockX, int blockZ) {
            this.world = world;
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.biomeNoise = getBiomeNoise(world);
            this.bottomY = Math.max(1, CAVE_DECORATION_BOTTOM_Y);
            this.topY = Math.min(world.getActualHeight() - 2, CAVE_DECORATION_TOP_Y);
        }

        boolean containsY(int y) {
            return y >= bottomY && y <= topY;
        }

        boolean isLikelyOpen(BlockPos pos, double margin) {
            return containsY(pos.getY()) && caveAffinity(pos) >= 0.35D - Math.min(0.25D, margin);
        }

        boolean isDeepEnough(BlockPos pos, int minSurfaceDepth) {
            return !isProtectedSurfaceColumn(pos.getX(), pos.getZ()) && getTerrainSurfaceY(pos.getX(), pos.getZ()) - pos.getY() >= minSurfaceDepth;
        }

        boolean isUndergroundOpenSpace(BlockPos pos, int minSurfaceDepth, double margin) {
            return isDeepEnough(pos, minSurfaceDepth) && !world.canSeeSky(pos) && isLikelyOpen(pos, margin);
        }

        double caveAffinity(BlockPos pos) {
            if (!containsY(pos.getY()) || world.canSeeSky(pos) || !isDeepEnough(pos, 8)) {
                return 0.0D;
            }

            double score = RetroFutureCaveWorldGenerator.this.isAirOrWater(world, pos) ? 0.35D : 0.0D;
            int openNeighbors = 0;
            int naturalNeighbors = 0;

            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos neighbor = pos.offset(facing);
                IBlockState state = world.getBlockState(neighbor);

                if (RetroFutureCaveWorldGenerator.this.isAirOrWater(world, neighbor)) {
                    openNeighbors++;
                } else if (RetroFutureCaveWorldGenerator.this.isNaturalCaveBlock(state.getBlock())) {
                    naturalNeighbors++;
                }
            }

            score += openNeighbors / 6.0D * 0.45D;

            if (naturalNeighbors > 0) {
                score += 0.2D;
            }

            return clamp(score, 0.0D, 1.0D);
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

        boolean isProtectedSurfaceColumn(int x, int z) {
            if (isInsideChunk(x, z)) {
                int index = (x - blockX) * CHUNK_SIZE + z - blockZ;
                byte cached = protectedSurfaceColumnCache[index];

                if (cached != 0) {
                    return cached == 1;
                }

                boolean protectedColumn = RetroFutureCaveWorldGenerator.this.isProtectedSurfaceBiomeColumn(world, x, z);
                protectedSurfaceColumnCache[index] = protectedColumn ? (byte)1 : (byte)2;
                return protectedColumn;
            }

            return RetroFutureCaveWorldGenerator.this.isProtectedSurfaceBiomeColumn(world, x, z);
        }
    }

    private boolean isMostlyProtectedSurfaceBiomeChunk(World world, int blockX, int blockZ) {
        int protectedColumns = 0;
        int samples = 0;

        for (int dx = 2; dx < CHUNK_SIZE; dx += 4) {
            for (int dz = 2; dz < CHUNK_SIZE; dz += 4) {
                samples++;

                if (isProtectedSurfaceBiomeColumn(world, blockX + dx, blockZ + dz)) {
                    protectedColumns++;
                }
            }
        }

        return protectedColumns >= samples / 2;
    }

    boolean isProtectedSurfaceBiomeColumn(World world, int x, int z) {
        Biome biome = world.getBiome(new BlockPos(x, world.getSeaLevel(), z));
        return biome == Biomes.OCEAN
                || biome == Biomes.DEEP_OCEAN
                || biome == Biomes.FROZEN_OCEAN
                || biome == Biomes.RIVER
                || biome == Biomes.FROZEN_RIVER
                || biome == Biomes.BEACH
                || biome == Biomes.COLD_BEACH
                || biome == Biomes.STONE_BEACH
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH);
    }

    private boolean intersectsProtectedStructure(World world, int blockX, int blockZ) {
        int minX = blockX - STRUCTURE_PROTECTION_MARGIN;
        int minZ = blockZ - STRUCTURE_PROTECTION_MARGIN;
        int maxX = blockX + CHUNK_SIZE - 1 + STRUCTURE_PROTECTION_MARGIN;
        int maxZ = blockZ + CHUNK_SIZE - 1 + STRUCTURE_PROTECTION_MARGIN;

        for (String dataName : PROTECTED_STRUCTURE_DATA_NAMES) {
            MapGenStructureData structureData = (MapGenStructureData)world.loadData(MapGenStructureData.class, dataName);

            if (structureData != null && intersectsStructureData(structureData.getTagCompound(), minX, minZ, maxX, maxZ)) {
                return true;
            }
        }

        return false;
    }

    private boolean intersectsStructureData(NBTTagCompound structures, int minX, int minZ, int maxX, int maxZ) {
        for (String key : structures.getKeySet()) {
            NBTTagCompound structure = structures.getCompoundTag(key);

            if (structure.hasKey("Valid") && !structure.getBoolean("Valid")) {
                continue;
            }

            if (!structure.hasKey("BB", 11)) {
                continue;
            }

            int[] bounds = structure.getIntArray("BB");

            if (bounds.length != 6) {
                continue;
            }

            StructureBoundingBox box = new StructureBoundingBox(bounds);

            if (box.intersectsWith(minX, minZ, maxX, maxZ)) {
                return true;
            }
        }

        return false;
    }

    boolean isLushBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double vertical = 1.0D - Math.min(1.0D, Math.abs(pos.getY() - 34.0D) / 42.0D);
        double region = context.biomeNoise.lushRegion.getValue(pos.getX() * LUSH_REGION_SCALE, 0.0D, pos.getZ() * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushPatch.getValue(pos.getX() * 1.25D, pos.getY() * 1.35D, pos.getZ() * 1.25D);
        double caveAffinity = context.caveAffinity(pos);
        CaveRegionType columnType = getCaveRegionType(context, pos.getX(), pos.getZ());
        double typeBias = columnType == CaveRegionType.LUSH ? 0.18D : columnType == CaveRegionType.DRIPSTONE ? -0.12D : 0.0D;

        return region * 0.36D + detail * 0.2D + vertical * 0.2D + caveAffinity * 0.24D + typeBias > -0.24D;
    }

    boolean isDripstoneBiomePatchNoise(CaveDensityContext context, BlockPos pos) {
        double region = context.biomeNoise.dripstoneRegion.getValue(pos.getX() * DRIPSTONE_REGION_SCALE, 0.0D, pos.getZ() * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(pos.getX() * 1.15D, pos.getY() * 1.45D, pos.getZ() * 1.15D);
        double ridged = 1.0D - Math.abs(context.biomeNoise.dripstoneRidge.getValue(pos.getX() * 1.8D, pos.getY() * 2.0D, pos.getZ() * 1.8D));
        double caveAffinity = context.caveAffinity(pos);
        CaveRegionType columnType = getCaveRegionType(context, pos.getX(), pos.getZ());
        double typeBias = columnType == CaveRegionType.DRIPSTONE ? 0.18D : columnType == CaveRegionType.LUSH ? -0.12D : 0.0D;

        return region * 0.32D + detail * 0.16D + ridged * 0.26D + caveAffinity * 0.22D + typeBias > -0.08D;
    }

    BlockPos findCavePocket(CaveDensityContext context, Random random, int minY, int maxY, int attempts, int margin, int minSurfaceDepth) {
        return findCavePocket(context, random, minY, maxY, attempts, margin, minSurfaceDepth, true);
    }

    BlockPos findRandomCavePocket(CaveDensityContext context, Random random, int minY, int maxY, int attempts, int margin, int minSurfaceDepth) {
        return findCavePocket(context, random, minY, maxY, attempts, margin, minSurfaceDepth, false);
    }

    private BlockPos findCavePocket(CaveDensityContext context, Random random, int minY, int maxY, int attempts, int margin, int minSurfaceDepth, boolean scanFallback) {
        if (maxY < minY) {
            return null;
        }

        int localMin = Math.max(0, Math.min(CHUNK_SIZE - 1, margin));
        int localMax = Math.max(localMin + 1, CHUNK_SIZE - localMin);
        BlockPos fallback = null;

        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos start = new BlockPos(
                    context.blockX + localMin + random.nextInt(localMax - localMin),
                    minY + random.nextInt(maxY - minY + 1),
                    context.blockZ + localMin + random.nextInt(localMax - localMin));
            BlockPos pocket = findNearbyCaveAir(context, start, 8, minSurfaceDepth);

            if (pocket == null) {
                continue;
            }

            if (hasNearbyNaturalSolid(context.world, pocket, 4, context.blockX, context.blockZ)) {
                return pocket;
            }

            if (fallback == null) {
                fallback = pocket;
            }
        }

        if (!scanFallback) {
            return fallback;
        }

        for (int column = 0; column < 10; column++) {
            int x = context.blockX + localMin + random.nextInt(localMax - localMin);
            int z = context.blockZ + localMin + random.nextInt(localMax - localMin);

            for (int y = maxY; y >= minY; y--) {
                BlockPos pocket = new BlockPos(x, y, z);

                if (isUsableCaveAir(context, pocket, minSurfaceDepth) && hasNearbyNaturalSolid(context.world, pocket, 4, context.blockX, context.blockZ)) {
                    return pocket;
                }
            }
        }

        return fallback;
    }

    BlockPos findNearbyCaveAir(CaveDensityContext context, BlockPos start, int range, int minSurfaceDepth) {
        if (isUsableCaveAir(context, start, minSurfaceDepth)) {
            return start;
        }

        for (int dy = 1; dy <= range; dy++) {
            BlockPos down = start.down(dy);

            if (isUsableCaveAir(context, down, minSurfaceDepth)) {
                return down;
            }

            BlockPos up = start.up(dy);

            if (isUsableCaveAir(context, up, minSurfaceDepth)) {
                return up;
            }
        }

        return null;
    }

    boolean isUsableCaveAir(CaveDensityContext context, BlockPos pos, int minSurfaceDepth) {
        return context.isInsideChunk(pos)
                && context.containsY(pos.getY())
                && context.world.isAirBlock(pos)
                && !context.world.canSeeSky(pos)
                && context.isDeepEnough(pos, minSurfaceDepth);
    }

    boolean hasNearbyNaturalSolid(World world, BlockPos pos, int radius, int blockX, int blockZ) {
        for (EnumFacing facing : EnumFacing.values()) {
            for (int distance = 1; distance <= radius; distance++) {
                BlockPos check = pos.offset(facing, distance);

                if (!isInsideChunk(check, blockX, blockZ)) {
                    continue;
                }

                Block block = world.getBlockState(check).getBlock();

                if (isNaturalCaveBlock(block) || isLushReplaceable(block)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean isAirOrWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER;
    }

    void setFluidloggableBlock(World world, BlockPos pos, IBlockState newState, int flags) {
        FluidState fluidState = getWaterFluidState(world, pos);

        if (fluidState.getFluid() == FluidRegistry.WATER) {
            world.setBlockState(pos, newState, flags);
            FluidloggedUtils.setFluidState(world, pos, world.getBlockState(pos), fluidState, false, flags);
            world.scheduleUpdate(pos, fluidState.getState().getBlock(), fluidState.getState().getBlock().tickRate(world));
        } else {
            world.setBlockState(pos, newState, flags);
        }
    }

    private boolean hasWaterFluid(World world, BlockPos pos) {
        return getWaterFluidState(world, pos).getFluid() == FluidRegistry.WATER;
    }

    private FluidState getWaterFluidState(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() == Material.WATER ? FluidState.of(state) : FluidloggedUtils.getFluidState(world, pos, state);
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

        if (lushColumns > 0 && lushColumns >= dripstoneColumns) {
            return CaveRegionType.LUSH;
        }

        if (dripstoneColumns > 0) {
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

        if (canLush && !canDripstone) {
            return CaveRegionType.LUSH;
        }

        if (canDripstone && !canLush) {
            return CaveRegionType.DRIPSTONE;
        }

        double choice = lushScore - dripstoneScore - selector * 0.22D;

        if (choice >= -CAVE_REGION_TYPE_MARGIN) {
            return CaveRegionType.LUSH;
        }

        return CaveRegionType.DRIPSTONE;
    }

    private double getLushRegionScore(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.lushRegion.getValue(x * LUSH_REGION_SCALE, 0.0D, z * LUSH_REGION_SCALE);
        double detail = context.biomeNoise.lushDetail.getValue(x * 1.25D, 0.0D, z * 1.25D);
        return region * 0.7D + detail * 0.3D;
    }

    private double getDripstoneRegionScore(CaveDensityContext context, int x, int z) {
        double region = context.biomeNoise.dripstoneRegion.getValue(x * DRIPSTONE_REGION_SCALE, 0.0D, z * DRIPSTONE_REGION_SCALE);
        double detail = context.biomeNoise.dripstoneDetail.getValue(x * 1.15D, 0.0D, z * 1.15D);
        return region * 0.7D + detail * 0.3D;
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
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL || block == ModBlocks.DeepSlate || block == ModBlocks.DRIPSTONE_BLOCK;
    }
}
