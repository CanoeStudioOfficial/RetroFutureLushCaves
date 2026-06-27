package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVine;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVinePlant;
import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.BigDripleaf;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.DripleafStem;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.SmallDripleaf;
import com.canoestudio.retrofuturemc.contents.mobs.axolotl.EntityAxolotl;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.CHUNK_SIZE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.DRIPLEAF_FEATURE_FACINGS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_AXOLOTL_CLAY_POOL_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_AXOLOTL_GROUP_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_AXOLOTL_MAX_GROUP_SIZE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_AXOLOTL_MIN_GROUP_SIZE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CAVE_MAX_Y;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CAVE_MIN_Y;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CAVE_VINE_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CEILING_MOSS_PATCH_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CEILING_MOSS_PATCH_DEPTH_MAX;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CEILING_MOSS_PATCH_DEPTH_MIN;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CEILING_MOSS_PATCH_VINE_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CLAY_PATCH_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CLAY_PATCH_VEGETATION_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CLAY_PATCH_VERTICAL_RANGE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CLAY_POOL_VEGETATION_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_CLAY_POOL_VERTICAL_RANGE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_DRIPLEAF_PATCH_BASE_DEPTH;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_DRIPLEAF_PATCH_EDGE_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_DRIPLEAF_PATCH_EXTRA_BOTTOM_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_DRIPLEAF_PATCH_RADIUS_MAX;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_DRIPLEAF_PATCH_RADIUS_MIN;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MIN_SURFACE_DEPTH;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_ATTEMPTS;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_DEPTH;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_EDGE_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_RADIUS_MAX;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_RADIUS_MIN;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_VEGETATION_CHANCE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_MOSS_PATCH_VERTICAL_RANGE;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_PATCH_SALT;
import static com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator.LUSH_SPORE_BLOSSOM_ATTEMPTS;

final class RetroFutureLushCaveGenerator {
    void generate(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context) {
        if (manager.getChunkCaveRegionType(context) != RetroFutureCaveWorldGenerator.CaveRegionType.LUSH) {
            return;
        }

        Random featureRandom = manager.createFeatureRandom(context.world, LUSH_PATCH_SALT, Math.floorDiv(context.blockX, CHUNK_SIZE), Math.floorDiv(context.blockZ, CHUNK_SIZE));
        int minY = Math.max(LUSH_CAVE_MIN_Y, context.bottomY);
        int maxY = Math.min(LUSH_CAVE_MAX_Y, context.topY);

        placeLushFloorMossPatches(manager, context, featureRandom, minY, maxY);
        placeLushCeilingMossPatches(manager, context, featureRandom, minY, maxY);
        placeLushClayPatches(manager, context, featureRandom, minY, maxY);
        placeLushCaveVines(manager, context, featureRandom, minY, maxY);
        placeLushSporeBlossoms(manager, context, featureRandom, minY, maxY);
        spawnLushAxolotlGroups(manager, context, featureRandom, minY, maxY);
    }

    private void placeLushFloorMossPatches(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < LUSH_MOSS_PATCH_ATTEMPTS; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos floor = findLushSurface(manager, context, start, EnumFacing.DOWN, LUSH_MOSS_PATCH_VERTICAL_RANGE);

            if (floor != null) {
                placeMossPatch(manager, context, random, floor, false);
            }
        }
    }

    private void placeLushCeilingMossPatches(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < LUSH_CEILING_MOSS_PATCH_ATTEMPTS; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos ceiling = findLushSurface(manager, context, start, EnumFacing.UP, LUSH_MOSS_PATCH_VERTICAL_RANGE);

            if (ceiling != null) {
                placeMossPatch(manager, context, random, ceiling, true);
            }
        }
    }

    private void placeLushClayPatches(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < LUSH_CLAY_PATCH_ATTEMPTS; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos floor = findLushSurface(manager, context, start, EnumFacing.DOWN, LUSH_CLAY_POOL_VERTICAL_RANGE);

            if (floor != null && tryPlaceClayPatchWithDripleaves(manager, context.world, random, floor, context.blockX, context.blockZ)) {
                maybeSpawnAxolotlGroupNearClayPool(manager, context, random, floor);
            }
        }
    }

    private void placeLushCaveVines(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < LUSH_CAVE_VINE_ATTEMPTS; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos ceiling = findLushSurface(manager, context, start, EnumFacing.UP, LUSH_MOSS_PATCH_VERTICAL_RANGE);

            if (ceiling != null) {
                BlockPos below = ceiling.down();

                if (context.world.isAirBlock(below)) {
                    placeCaveVine(context.world, random, below);
                }
            }
        }
    }

    private void placeLushSporeBlossoms(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        for (int attempt = 0; attempt < LUSH_SPORE_BLOSSOM_ATTEMPTS; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos ceiling = findLushSurface(manager, context, start, EnumFacing.UP, LUSH_MOSS_PATCH_VERTICAL_RANGE);

            if (ceiling != null) {
                BlockPos below = ceiling.down();

                if (context.world.isAirBlock(below) && ModBlocks.SPORE_BLOSSOM.canPlaceBlockAt(context.world, below)) {
                    context.world.setBlockState(below, ModBlocks.SPORE_BLOSSOM.getDefaultState(), 2);
                }
            }
        }
    }

    private void spawnLushAxolotlGroups(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        if (random.nextInt(LUSH_AXOLOTL_GROUP_CHANCE) != 0) {
            return;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos start = randomLushFeatureStart(context, random, minY, maxY);
            BlockPos water = findLushWaterSpawnPos(manager, context, random, start, 8);

            if (water != null) {
                int count = LUSH_AXOLOTL_MIN_GROUP_SIZE + random.nextInt(LUSH_AXOLOTL_MAX_GROUP_SIZE - LUSH_AXOLOTL_MIN_GROUP_SIZE + 1);
                spawnAxolotlGroup(context.world, random, water, count, context.blockX, context.blockZ);
                return;
            }
        }
    }

    private BlockPos randomLushFeatureStart(RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, int minY, int maxY) {
        return new BlockPos(context.blockX + random.nextInt(CHUNK_SIZE), minY + random.nextInt(Math.max(1, maxY - minY + 1)), context.blockZ + random.nextInt(CHUNK_SIZE));
    }

    private BlockPos findLushSurface(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, BlockPos start, EnumFacing direction, int maxSteps) {
        if (!context.isInsideChunk(start) || manager != null && !manager.isLushRegionColumn(context, start.getX(), start.getZ())) {
            return null;
        }

        BlockPos scan = start;
        int offset = 0;

        while (offset < maxSteps && context.isInsideChunk(scan) && context.containsY(scan.getY()) && context.world.isAirBlock(scan)) {
            scan = scan.offset(direction);
            offset++;
        }

        offset = 0;
        EnumFacing outward = direction.getOpposite();

        while (offset < maxSteps && context.isInsideChunk(scan) && context.containsY(scan.getY()) && !context.world.isAirBlock(scan)) {
            scan = scan.offset(outward);
            offset++;
        }

        if (!context.isInsideChunk(scan)
                || !context.containsY(scan.getY())
                || !context.world.isAirBlock(scan)
                || !context.isDeepEnough(scan, LUSH_MIN_SURFACE_DEPTH)
                || context.world.canSeeSky(scan)) {
            return null;
        }

        BlockPos surface = scan.offset(direction);

        if (!context.isInsideChunk(surface)
                || !context.containsY(surface.getY())
                || context.world.canSeeSky(surface)
                || !context.world.getBlockState(surface).isSideSolid(context.world, surface, outward)) {
            return null;
        }

        return surface;
    }

    private void placeMossPatch(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, BlockPos originSurface, boolean ceiling) {
        int xRadius = 1 + LUSH_MOSS_PATCH_RADIUS_MIN + random.nextInt(LUSH_MOSS_PATCH_RADIUS_MAX - LUSH_MOSS_PATCH_RADIUS_MIN + 1);
        int zRadius = 1 + LUSH_MOSS_PATCH_RADIUS_MIN + random.nextInt(LUSH_MOSS_PATCH_RADIUS_MAX - LUSH_MOSS_PATCH_RADIUS_MIN + 1);
        int baseDepth = ceiling ? LUSH_CEILING_MOSS_PATCH_DEPTH_MIN + random.nextInt(LUSH_CEILING_MOSS_PATCH_DEPTH_MAX - LUSH_CEILING_MOSS_PATCH_DEPTH_MIN + 1) : LUSH_MOSS_PATCH_DEPTH;

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            boolean xEdge = dx == -xRadius || dx == xRadius;

            for (int dz = -zRadius; dz <= zRadius; dz++) {
                boolean zEdge = dz == -zRadius || dz == zRadius;
                boolean corner = xEdge && zEdge;
                boolean edge = xEdge || zEdge;

                if (corner || edge && random.nextFloat() > LUSH_MOSS_PATCH_EDGE_CHANCE) {
                    continue;
                }

                BlockPos surface = findLushSurface(manager, context, originSurface.add(dx, 0, dz), ceiling ? EnumFacing.UP : EnumFacing.DOWN, LUSH_MOSS_PATCH_VERTICAL_RANGE);

                if (surface == null || !placeMossGround(manager, context.world, surface, baseDepth, ceiling)) {
                    continue;
                }

                if (ceiling) {
                    if (random.nextFloat() < LUSH_CEILING_MOSS_PATCH_VINE_CHANCE) {
                        BlockPos below = surface.down();

                        if (context.world.isAirBlock(below)) {
                            placeCaveVineInMoss(context.world, random, below);
                        }
                    }
                } else if (random.nextFloat() < LUSH_MOSS_PATCH_VEGETATION_CHANCE) {
                    placeMossVegetation(context.world, random, surface.up());
                }
            }
        }
    }

    private boolean placeMossGround(RetroFutureCaveWorldGenerator manager, World world, BlockPos surface, int depth, boolean ceiling) {
        boolean placedAny = false;
        EnumFacing direction = ceiling ? EnumFacing.UP : EnumFacing.DOWN;

        for (int i = 0; i < depth; i++) {
            BlockPos groundPos = surface.offset(direction, i);

            if (groundPos.getY() <= 0 || groundPos.getY() >= world.getActualHeight() - 1) {
                break;
            }

            Block block = world.getBlockState(groundPos).getBlock();

            if (block == ModBlocks.MOSS_BLOCK) {
                placedAny = true;
                continue;
            }

            if (!manager.isMossReplaceable(block)) {
                return placedAny;
            }

            world.setBlockState(groundPos, ModBlocks.MOSS_BLOCK.getDefaultState(), 2);
            placedAny = true;
        }

        return placedAny;
    }

    private void placeMossVegetation(World world, Random random, BlockPos pos) {
        if (!world.isAirBlock(pos)) {
            return;
        }

        int roll = random.nextInt(96);

        if (roll < 50) {
            IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);

            if (Blocks.TALLGRASS.canBlockStay(world, pos, grass)) {
                world.setBlockState(pos, grass, 2);
            }
        } else if (roll < 75) {
            world.setBlockState(pos, ModBlocks.MOSS_CARPET.getDefaultState(), 2);
        } else if (roll < 85) {
            IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);

            if (Blocks.TALLGRASS.canBlockStay(world, pos, grass)) {
                Blocks.DOUBLE_PLANT.placeAt(world, pos, BlockDoublePlant.EnumPlantType.GRASS, 2);
            }
        } else if (roll < 92 && ModBlocks.Azalea.canPlaceBlockAt(world, pos)) {
            world.setBlockState(pos, ModBlocks.Azalea.getDefaultState(), 2);
        } else if (ModBlocks.Flowering_Azalea.canPlaceBlockAt(world, pos)) {
            world.setBlockState(pos, ModBlocks.Flowering_Azalea.getDefaultState(), 2);
        }
    }

    private void placeCaveVineInMoss(World world, Random random, BlockPos start) {
        int length = random.nextInt(6) == 0 ? 1 + random.nextInt(7) : random.nextInt(4);

        if (length > 0) {
            placeCaveVine(world, random, start, length);
        }
    }

    private BlockPos findLushWaterSpawnPos(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, BlockPos center, int radius) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos pos = center.add(random.nextInt(radius * 2 + 1) - radius, random.nextInt(7) - 3, random.nextInt(radius * 2 + 1) - radius);

            if (!context.isInsideChunk(pos) || !context.containsY(pos.getY()) || !context.isDeepEnough(pos, LUSH_MIN_SURFACE_DEPTH) || !manager.isLushRegionColumn(context, pos.getX(), pos.getZ())) {
                continue;
            }

            if (canSpawnAxolotlInLushWater(context.world, pos, context.blockX, context.blockZ)) {
                return pos;
            }
        }

        return null;
    }

    private void maybeSpawnAxolotlGroupNearClayPool(RetroFutureCaveWorldGenerator manager, RetroFutureCaveWorldGenerator.CaveDensityContext context, Random random, BlockPos center) {
        if (random.nextInt(LUSH_AXOLOTL_CLAY_POOL_CHANCE) != 0) {
            return;
        }

        BlockPos water = findLushWaterSpawnPos(manager, context, random, center, LUSH_DRIPLEAF_PATCH_RADIUS_MAX + 2);

        if (water != null) {
            int count = LUSH_AXOLOTL_MIN_GROUP_SIZE + random.nextInt(LUSH_AXOLOTL_MAX_GROUP_SIZE - LUSH_AXOLOTL_MIN_GROUP_SIZE + 1);
            spawnAxolotlGroup(context.world, random, water, count, context.blockX, context.blockZ);
        }
    }

    private void spawnAxolotlGroup(World world, Random random, BlockPos origin, int count, int blockX, int blockZ) {
        AxisAlignedBB checkBox = new AxisAlignedBB(blockX, origin.getY() - 12, blockZ, blockX + CHUNK_SIZE, origin.getY() + 12, blockZ + CHUNK_SIZE);
        List<EntityAxolotl> existing = world.getEntitiesWithinAABB(EntityAxolotl.class, checkBox);

        if (!existing.isEmpty()) {
            return;
        }

        int spawned = 0;

        for (int attempt = 0; attempt < count * 8 && spawned < count; attempt++) {
            BlockPos pos = origin.add(random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);

            if (!canSpawnAxolotlInLushWater(world, pos, blockX, blockZ)) {
                continue;
            }

            EntityAxolotl axolotl = new EntityAxolotl(world);
            axolotl.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            axolotl.onInitialSpawn(world.getDifficultyForLocation(pos), (IEntityLivingData)null);
            world.spawnEntity(axolotl);
            spawned++;
        }
    }

    private boolean canSpawnAxolotlInLushWater(World world, BlockPos pos, int blockX, int blockZ) {
        return pos.getX() >= blockX
                && pos.getX() < blockX + CHUNK_SIZE
                && pos.getZ() >= blockZ
                && pos.getZ() < blockZ + CHUNK_SIZE
                && !world.canSeeSky(pos)
                && world.getBlockState(pos).getMaterial() == Material.WATER
                && world.getBlockState(pos.down()).getBlock() == Blocks.CLAY;
    }

    private boolean tryPlaceClayPatchWithDripleaves(RetroFutureCaveWorldGenerator manager, World world, Random random, BlockPos originFloor, int blockX, int blockZ) {
        boolean waterloggedPatch = random.nextInt(3) != 0;
        int verticalRange = waterloggedPatch ? LUSH_CLAY_POOL_VERTICAL_RANGE : LUSH_CLAY_PATCH_VERTICAL_RANGE;
        float vegetationChance = waterloggedPatch ? LUSH_CLAY_POOL_VEGETATION_CHANCE : LUSH_CLAY_PATCH_VEGETATION_CHANCE;
        int xRadius = 1 + LUSH_DRIPLEAF_PATCH_RADIUS_MIN + random.nextInt(LUSH_DRIPLEAF_PATCH_RADIUS_MAX - LUSH_DRIPLEAF_PATCH_RADIUS_MIN + 1);
        int zRadius = 1 + LUSH_DRIPLEAF_PATCH_RADIUS_MIN + random.nextInt(LUSH_DRIPLEAF_PATCH_RADIUS_MAX - LUSH_DRIPLEAF_PATCH_RADIUS_MIN + 1);
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

                BlockPos surfaceFloor = findLushPatchFloor(manager, world, originFloor.add(dx, 0, dz), verticalRange, blockX, blockZ);

                if (surfaceFloor == null || surfaceTargets.contains(surfaceFloor)) {
                    continue;
                }

                BlockPos openPos = surfaceFloor.up();

                if (!waterloggedPatch && !world.isAirBlock(openPos)) {
                    continue;
                }

                int depth = LUSH_DRIPLEAF_PATCH_BASE_DEPTH + (random.nextFloat() < LUSH_DRIPLEAF_PATCH_EXTRA_BOTTOM_CHANCE ? 1 : 0);

                if (placeClayGround(manager, world, surfaceFloor, depth)) {
                    surfaceTargets.add(surfaceFloor);
                    placedGround = true;
                }
            }
        }

        if (!placedGround) {
            return false;
        }

        for (BlockPos surfaceFloor : surfaceTargets) {
            if (manager.isNearLava(world, surfaceFloor, 2, blockX, blockZ)) {
                continue;
            }

            if (waterloggedPatch) {
                world.setBlockState(surfaceFloor, Blocks.WATER.getDefaultState(), 2);

                if (random.nextFloat() < vegetationChance) {
                    placeDripleafFeature(manager, world, random, surfaceFloor, blockX, blockZ);
                }
            } else if (random.nextFloat() < vegetationChance) {
                placeDripleafFeature(manager, world, random, surfaceFloor.up(), blockX, blockZ);
            }
        }

        return true;
    }

    private BlockPos findLushPatchFloor(RetroFutureCaveWorldGenerator manager, World world, BlockPos origin, int verticalRange, int blockX, int blockZ) {
        if (!manager.isInsideChunk(origin, blockX, blockZ)) {
            return null;
        }

        BlockPos scan = origin;
        int offset = 0;

        while (offset < verticalRange && manager.isInsideChunk(scan, blockX, blockZ) && manager.isAirOrWater(world, scan)) {
            scan = scan.down();
            offset++;
        }

        offset = 0;

        while (offset < verticalRange && manager.isInsideChunk(scan, blockX, blockZ) && !manager.isAirOrWater(world, scan)) {
            scan = scan.up();
            offset++;
        }

        if (!manager.isInsideChunk(scan, blockX, blockZ) || !manager.isAirOrWater(world, scan) || world.canSeeSky(scan)) {
            return null;
        }

        BlockPos floor = scan.down();

        if (!manager.isInsideChunk(floor, blockX, blockZ) || world.canSeeSky(floor) || !manager.isLushReplaceable(world.getBlockState(floor).getBlock()) || !world.isSideSolid(floor, EnumFacing.UP, true)) {
            return null;
        }

        return floor;
    }

    private boolean placeClayGround(RetroFutureCaveWorldGenerator manager, World world, BlockPos floor, int depth) {
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

            if (!manager.isLushReplaceable(block)) {
                return placedAny;
            }

            world.setBlockState(groundPos, Blocks.CLAY.getDefaultState(), 2);
            placedAny = true;
        }

        return placedAny;
    }

    private boolean placeDripleafFeature(RetroFutureCaveWorldGenerator manager, World world, Random random, BlockPos basePos, int blockX, int blockZ) {
        int choice = random.nextInt(5);

        if (choice == 0) {
            return placeSmallDripleaf(world, random, basePos, blockX, blockZ);
        }

        return placeBigDripleafColumn(manager, world, random, basePos, DRIPLEAF_FEATURE_FACINGS[choice - 1], blockX, blockZ);
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

    private boolean placeBigDripleafColumn(RetroFutureCaveWorldGenerator manager, World world, Random random, BlockPos basePos, EnumFacing facing, int blockX, int blockZ) {
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
            manager.setFluidloggableBlock(world, basePos.up(y), ModBlocks.DRIPLEAF_STEM.getDefaultState().withProperty(DripleafStem.FACING, facing), 2);
        }

        manager.setFluidloggableBlock(world, basePos.up(stemHeight), ModBlocks.BIG_DRIPLEAF.getDefaultState().withProperty(BigDripleaf.FACING, facing), 2);
        return true;
    }

    private boolean canPlaceDripleafPartAt(World world, BlockPos pos, int blockX, int blockZ) {
        IBlockState state = world.getBlockState(pos);
        return pos.getX() >= blockX
                && pos.getX() < blockX + CHUNK_SIZE
                && pos.getZ() >= blockZ
                && pos.getZ() < blockZ + CHUNK_SIZE
                && (state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER || FluidloggedUtils.getFluidState(world, pos, state).getFluid() == FluidRegistry.WATER);
    }

    private boolean canSupportDripleaf(Block block) {
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.ROOTED_DIRT || block == Blocks.GRASS || block == Blocks.DIRT || block == Blocks.MYCELIUM || block == Blocks.FARMLAND;
    }

    private boolean canSupportSmallDripleaf(Block block) {
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK;
    }

    private void placeCaveVine(World world, Random random, BlockPos start) {
        int roll = random.nextInt(15);
        int length;

        if (roll < 2) {
            length = random.nextInt(20);
        } else if (roll < 5) {
            length = random.nextInt(3);
        } else {
            length = random.nextInt(7);
        }

        if (length > 0) {
            placeCaveVine(world, random, start, length);
        }
    }

    private void placeCaveVine(World world, Random random, BlockPos start, int length) {
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
}
