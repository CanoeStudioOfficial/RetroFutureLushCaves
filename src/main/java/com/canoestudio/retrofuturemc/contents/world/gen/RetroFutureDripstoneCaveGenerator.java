package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.CHUNK_SIZE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_CAVE_MAX_Y;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_CAVE_MIN_Y;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_CLUSTER_MAX_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_CLUSTER_MIN_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_CLUSTER_SEARCH_RANGE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_LARGE_FEATURE_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_MAX_BLOCKS_PER_CHUNK;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_MAX_COLUMNS_PER_CHUNK;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_MIN_SURFACE_DEPTH;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPSTONE_PATCH_SALT;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.NO_SURFACE;

final class RetroFutureDripstoneCaveGenerator {
    void generate(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context) {
        Random featureRandom = manager.createFeatureRandom(context.world, DRIPSTONE_PATCH_SALT, Math.floorDiv(context.blockX, CHUNK_SIZE), Math.floorDiv(context.blockZ, CHUNK_SIZE));
        RetroFutureCaveWorldGenerator.CaveRegionType chunkType = manager.getChunkCaveRegionType(context);

        if (!shouldTryDripstoneCave(featureRandom, chunkType)) {
            return;
        }

        int minY = Math.max(DRIPSTONE_CAVE_MIN_Y, context.bottomY);
        int maxY = Math.min(DRIPSTONE_CAVE_MAX_Y, context.topY);
        int[] budget = new int[] {DRIPSTONE_MAX_BLOCKS_PER_CHUNK, DRIPSTONE_MAX_COLUMNS_PER_CHUNK};
        int clusterAttempts = getClusterAttempts(featureRandom, chunkType);

        for (int attempt = 0; attempt < clusterAttempts; attempt++) {
            if (budget[0] <= 0 && budget[1] <= 0) {
                return;
            }

            BlockPos origin = findDripstoneClusterOrigin(manager, context, featureRandom, minY, maxY);

            if (origin == null) {
                continue;
            }

            if (featureRandom.nextInt(100) < DRIPSTONE_LARGE_FEATURE_CHANCE) {
                placeLargeDripstoneFeature(manager, context, featureRandom, origin, budget);
            }

            placeDripstoneCluster(manager, context, featureRandom, origin, budget);
        }
    }

    private boolean shouldTryDripstoneCave(Random random, RetroFutureCaveWorldGenerator.CaveRegionType chunkType) {
        if (chunkType == RetroFutureCaveWorldGenerator.CaveRegionType.DRIPSTONE) {
            return true;
        }

        if (chunkType == RetroFutureCaveWorldGenerator.CaveRegionType.LUSH) {
            return random.nextInt(14) == 0;
        }

        return random.nextInt(6) == 0;
    }

    private int getClusterAttempts(Random random, RetroFutureCaveWorldGenerator.CaveRegionType chunkType) {
        if (chunkType == RetroFutureCaveWorldGenerator.CaveRegionType.DRIPSTONE) {
            return DRIPSTONE_CLUSTER_MIN_ATTEMPTS + random.nextInt(DRIPSTONE_CLUSTER_MAX_ATTEMPTS - DRIPSTONE_CLUSTER_MIN_ATTEMPTS + 1);
        }

        return 1 + random.nextInt(2);
    }

    private BlockPos findDripstoneClusterOrigin(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        BlockPos fallback = null;

        for (int attempt = 0; attempt < 36; attempt++) {
            BlockPos pos = manager.findRandomCavePocket(context, random, minY, maxY, 1, RetroFutureCaveWorldGenerator.CHUNK_MARGIN, DRIPSTONE_MIN_SURFACE_DEPTH);

            if (pos == null) {
                continue;
            }

            if (findDripstoneFloorY(manager, context, pos, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE && findDripstoneCeilingY(manager, context, pos, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE) {
                if (manager.isDripstoneBiomePatchNoise(context, pos)) {
                    return pos;
                }

                if (fallback == null) {
                    fallback = pos;
                }
            }
        }

        if (fallback != null) {
            return fallback;
        }

        BlockPos pocket = manager.findCavePocket(context, random, minY, maxY, 24, RetroFutureCaveWorldGenerator.CHUNK_MARGIN, DRIPSTONE_MIN_SURFACE_DEPTH);

        if (pocket != null
                && findDripstoneFloorY(manager, context, pocket, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE
                && findDripstoneCeilingY(manager, context, pocket, DRIPSTONE_CLUSTER_SEARCH_RANGE) != NO_SURFACE) {
            return pocket;
        }

        return null;
    }

    private void placeDripstoneCluster(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, BlockPos origin, int[] budget) {
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

                if (!context.isInsideChunk(columnOrigin) || !context.isDeepEnough(columnOrigin, DRIPSTONE_MIN_SURFACE_DEPTH)) {
                    continue;
                }

                double chance = getClusterColumnChance(xRadius, zRadius, dx, dz) * density;

                if (random.nextDouble() > chance) {
                    continue;
                }

                int floorY = findDripstoneFloorY(manager, context, columnOrigin, DRIPSTONE_CLUSTER_SEARCH_RANGE);
                int ceilingY = findDripstoneCeilingY(manager, context, columnOrigin, DRIPSTONE_CLUSTER_SEARCH_RANGE);

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
                    placeDripstoneBaseLayer(manager, context, new BlockPos(columnOrigin.getX(), ceilingY, columnOrigin.getZ()), EnumFacing.UP, baseLayerThickness, budget);
                    placePointedDripstoneWithBudget(manager, context.world, new BlockPos(columnOrigin.getX(), ceilingY - 1, columnOrigin.getZ()), EnumFacing.DOWN, stalactiteHeight, budget);
                }

                if (floorY != NO_SURFACE) {
                    placeDripstoneBaseLayer(manager, context, new BlockPos(columnOrigin.getX(), floorY, columnOrigin.getZ()), EnumFacing.DOWN, baseLayerThickness, budget);
                    placePointedDripstoneWithBudget(manager, context.world, new BlockPos(columnOrigin.getX(), floorY + 1, columnOrigin.getZ()), EnumFacing.UP, stalagmiteHeight, budget);
                }
            }
        }
    }

    private void placeLargeDripstoneFeature(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, BlockPos origin, int[] budget) {
        int floorY = findDripstoneFloorY(manager, context, origin, 16);
        int ceilingY = findDripstoneCeilingY(manager, context, origin, 16);

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
                    placeLargeDripstoneBlock(manager, context, origin.add(dx, ceilingY - origin.getY() - 1 - y, dz), budget);
                }

                for (int y = 0; y < stalagmiteHeight && budget[0] > 0; y++) {
                    placeLargeDripstoneBlock(manager, context, origin.add(dx, floorY - origin.getY() + 1 + y, dz), budget);
                }
            }
        }
    }

    private void placeLargeDripstoneBlock(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, BlockPos pos, int[] budget) {
        if (budget[0] <= 0 || !context.isInsideChunk(pos) || !manager.isAirOrWater(context.world, pos)) {
            return;
        }

        context.world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
        budget[0]--;
    }

    private int findDripstoneFloorY(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, BlockPos pos, int range) {
        for (int dy = 0; dy <= range && pos.getY() - dy > 0; dy++) {
            BlockPos check = pos.down(dy);
            IBlockState state = context.world.getBlockState(check);

            if (manager.isAirOrWater(context.world, check)) {
                continue;
            }

            return manager.isNaturalCaveBlock(state.getBlock()) ? check.getY() : NO_SURFACE;
        }

        return NO_SURFACE;
    }

    private int findDripstoneCeilingY(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, BlockPos pos, int range) {
        for (int dy = 0; dy <= range && pos.getY() + dy < context.world.getActualHeight() - 1; dy++) {
            BlockPos check = pos.up(dy);
            IBlockState state = context.world.getBlockState(check);

            if (manager.isAirOrWater(context.world, check)) {
                continue;
            }

            return manager.isNaturalCaveBlock(state.getBlock()) ? check.getY() : NO_SURFACE;
        }

        return NO_SURFACE;
    }

    private void placeDripstoneBaseLayer(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, BlockPos firstPos, EnumFacing intoStone, int maxCount, int[] budget) {
        for (int i = 0; i < maxCount && budget[0] > 0; i++) {
            BlockPos pos = firstPos.offset(intoStone, i);
            IBlockState state = context.world.getBlockState(pos);

            if (!context.isInsideChunk(pos) || !manager.isNaturalCaveBlock(state.getBlock())) {
                return;
            }

            context.world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
            budget[0]--;
        }
    }

    private void placePointedDripstoneWithBudget(RetroFutureCaveWorldGenerator manager, World world, BlockPos start, EnumFacing direction, int height, int[] budget) {
        if (height <= 0 || budget[1] <= 0 || !manager.isAirOrWater(world, start)) {
            return;
        }

        manager.placePointedDripstone(world, start, direction, height);
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
}
