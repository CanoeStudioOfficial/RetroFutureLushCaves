package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.PointedDripstoneBlock;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVine;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVinePlant;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.BigDripleaf;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.DripleafStem;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.SmallDripleaf;
import com.canoestudio.retrofuturemc.contents.mobs.axolotl.EntityAxolotl;
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
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RetroFutureCaveWorldGenerator implements IWorldGenerator {
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_MARGIN = 5;
    private static final int AZALEA_TREE_CHANCE = 18;
    private static final int AZALEA_TREE_MIN_SPACING = 24;
    private static final int LUSH_CAVE_MIN_Y = 8;
    private static final int LUSH_CAVE_MAX_Y = 62;
    private static final double LUSH_REGION_SCALE = 0.012D;
    private static final double LUSH_REGION_THRESHOLD = 0.18D;
    private static final long LUSH_REGION_SALT = 0x4C5553485245474EL;
    private static final long LUSH_PATCH_SALT = 0x4C55534843415645L;
    private static final long DRIPSTONE_PATCH_SALT = 0x4452495053544F4EL;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) {
            return;
        }

        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;

        if (isLushRegionChunk(world, blockX, blockZ) && random.nextInt(AZALEA_TREE_CHANCE) == 0) {
            BlockPos azaleaTree = findSurfaceAzaleaTreePos(world, random, blockX, blockZ);
            BlockPos lushCenter = azaleaTree == null || hasNearbyAzaleaTree(world, azaleaTree, AZALEA_TREE_MIN_SPACING) ? null : findCavePocketBelowAzalea(world, random, azaleaTree, blockX, blockZ);

            if (lushCenter != null && generateSurfaceAzaleaTree(world, random, azaleaTree)) {
                placeRootTrail(world, random, azaleaTree, lushCenter);
                generateLushPocket(world, random, azaleaTree, lushCenter, blockX, blockZ);
            }
        }

        if (random.nextInt(7) == 0) {
            BlockPos dripstoneCenter = findCavePocket(world, random, blockX, blockZ, 8, 58);

            if (dripstoneCenter != null) {
                generateDripstonePocket(world, random, dripstoneCenter, blockX, blockZ);
            }
        }
    }

    private BlockPos findSurfaceAzaleaTreePos(World world, Random random, int blockX, int blockZ) {
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos start = world.getHeight(new BlockPos(blockX + random.nextInt(CHUNK_SIZE), 0, blockZ + random.nextInt(CHUNK_SIZE)));

            if (start.getY() >= 50 && start.getY() <= world.getActualHeight() - 16 && isLushRegionColumn(world, start.getX(), start.getZ())) {
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

    private BlockPos findCavePocket(World world, Random random, int blockX, int blockZ, int minY, int maxY) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos pos = new BlockPos(
                    blockX + CHUNK_MARGIN + random.nextInt(CHUNK_SIZE - CHUNK_MARGIN * 2),
                    minY + random.nextInt(maxY - minY),
                    blockZ + CHUNK_MARGIN + random.nextInt(CHUNK_SIZE - CHUNK_MARGIN * 2));

            if (world.isAirBlock(pos) && !world.canSeeSky(pos) && hasNearbySolid(world, pos, 4)) {
                return pos;
            }
        }

        return null;
    }

    private BlockPos findCavePocketBelowAzalea(World world, Random random, BlockPos azaleaTree, int blockX, int blockZ) {
        int minY = LUSH_CAVE_MIN_Y;
        int maxY = Math.min(LUSH_CAVE_MAX_Y, azaleaTree.getY() - 8);

        if (maxY <= minY) {
            return null;
        }

        for (int attempt = 0; attempt < 144; attempt++) {
            BlockPos pos = new BlockPos(
                    azaleaTree.getX() + random.nextInt(21) - 10,
                    minY + random.nextInt(maxY - minY),
                    azaleaTree.getZ() + random.nextInt(21) - 10);

            if (!isInsideChunk(pos, blockX, blockZ)) {
                continue;
            }

            if (world.isAirBlock(pos) && !world.canSeeSky(pos) && hasNearbySolid(world, pos, 3, blockX, blockZ) && isLushCaveRegion(world, pos, azaleaTree)) {
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

                    if (isInsideChunk(pos, blockX, blockZ) && world.isAirBlock(pos) && !world.canSeeSky(pos) && hasNearbySolid(world, pos, 3, blockX, blockZ) && isLushCaveRegion(world, pos, azaleaTree)) {
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

    private void generateLushPocket(World world, Random random, BlockPos azaleaTree, BlockPos center, int blockX, int blockZ) {
        int radius = 18 + random.nextInt(7);
        int verticalRadius = 10 + random.nextInt(5);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = (dx * dx) / (double)(radius * radius) + (dy * dy) / (double)(verticalRadius * verticalRadius) + (dz * dz) / (double)(radius * radius);

                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);

                    if (!isInsideChunk(pos, blockX, blockZ) || !isLushPatchNoise(world, pos, azaleaTree, distance)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isLushReplaceable(state.getBlock()) || world.canSeeSky(pos)) {
                        continue;
                    }

                    if (isAirOrWater(world, pos.up())) {
                        decorateLushFloor(world, random, pos, blockX, blockZ);
                    }

                    if (world.isAirBlock(pos.down())) {
                        decorateLushCeiling(world, random, pos);
                    }
                }
            }
        }

        spawnAxolotlNearWater(world, random, center, radius + 1, blockX, blockZ);
    }

    private void decorateLushFloor(World world, Random random, BlockPos pos, int blockX, int blockZ) {
        BlockPos above = pos.up();

        if (!isAirOrWater(world, above)) {
            return;
        }

        if (random.nextInt(100) < 16 && tryPlaceClayPoolWithDripleaf(world, random, pos, above, blockX, blockZ)) {
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
        } else if (roll < 58 && isNearWater(world, above, 4, blockX, blockZ)) {
            placeRandomDripleaf(world, random, above, blockX, blockZ);
        }
    }

    private boolean tryPlaceClayPoolWithDripleaf(World world, Random random, BlockPos floor, BlockPos above, int blockX, int blockZ) {
        if (!isAirOrWater(world, above)) {
            return false;
        }

        world.setBlockState(floor, Blocks.CLAY.getDefaultState(), 2);
        boolean placedWater = false;
        List<BlockPos> waterTargets = new ArrayList<BlockPos>();

        for (int i = 0; i < 4; i++) {
            EnumFacing facing = EnumFacing.byHorizontalIndex((i + random.nextInt(4)) & 3);
            BlockPos sideFloor = floor.offset(facing);

            if (!isInsideChunk(sideFloor, blockX, blockZ) || !isLushReplaceable(world.getBlockState(sideFloor).getBlock())) {
                continue;
            }

            world.setBlockState(sideFloor, Blocks.CLAY.getDefaultState(), 2);
            BlockPos waterPos = sideFloor.up();

            if (world.isAirBlock(waterPos) && random.nextInt(100) < 55) {
                world.setBlockState(waterPos, Blocks.WATER.getDefaultState(), 2);
                waterTargets.add(waterPos);
                placedWater = true;
            }
        }

        while (!waterTargets.isEmpty()) {
            BlockPos target = waterTargets.remove(random.nextInt(waterTargets.size()));

            if (placeRandomDripleaf(world, random, target, blockX, blockZ)) {
                return true;
            }
        }

        if (placedWater || isNearWater(world, above, 4, blockX, blockZ)) {
            placeRandomDripleaf(world, random, above, blockX, blockZ);
            return true;
        }

        return false;
    }

    private boolean placeRandomDripleaf(World world, Random random, BlockPos basePos, int blockX, int blockZ) {
        if (random.nextInt(100) < 55 && placeBigDripleafColumn(world, random, basePos, blockX, blockZ)) {
            return true;
        }

        if (canPlaceDripleafPartAt(world, basePos, blockX, blockZ) && canPlaceDripleafPartAt(world, basePos.up(), blockX, blockZ)) {
            ((SmallDripleaf)ModBlocks.SMALL_DRIPLEAF).placeAt(world, basePos, EnumFacing.byHorizontalIndex(random.nextInt(4)), 2);
            return true;
        }

        return false;
    }

    private boolean placeBigDripleafColumn(World world, Random random, BlockPos basePos, int blockX, int blockZ) {
        if (!canSupportDripleaf(world.getBlockState(basePos.down()).getBlock())) {
            return false;
        }

        int stemHeight = random.nextInt(3) == 0 ? 0 : random.nextInt(5);
        EnumFacing facing = EnumFacing.byHorizontalIndex(random.nextInt(4));

        for (int y = 0; y <= stemHeight; y++) {
            BlockPos check = basePos.up(y);

            if (!canPlaceDripleafPartAt(world, check, blockX, blockZ)) {
                return false;
            }
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
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.ROOTED_DIRT || block == Blocks.GRASS || block == Blocks.DIRT;
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

    private void generateDripstonePocket(World world, Random random, BlockPos center, int blockX, int blockZ) {
        int radius = 10 + random.nextInt(7);
        int verticalRadius = 7 + random.nextInt(4);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = (dx * dx) / (double)(radius * radius) + (dy * dy) / (double)(verticalRadius * verticalRadius) + (dz * dz) / (double)(radius * radius);

                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);

                    if (!isInsideChunk(pos, blockX, blockZ) || !isDripstonePatchNoise(world, pos, distance)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isNaturalCaveBlock(state.getBlock()) || world.canSeeSky(pos)) {
                        continue;
                    }

                    if ((world.isAirBlock(pos.down()) || world.isAirBlock(pos.up())) && random.nextInt(100) < 46) {
                        world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
                    }

                    if (world.isAirBlock(pos.down()) && random.nextInt(100) < 42) {
                        placePointedDripstone(world, random, pos.down(), EnumFacing.DOWN);
                    }

                    if (world.isAirBlock(pos.up()) && random.nextInt(100) < 30) {
                        placePointedDripstone(world, random, pos.up(), EnumFacing.UP);
                    }
                }
            }
        }
    }

    private void placePointedDripstone(World world, Random random, BlockPos start, EnumFacing direction) {
        int length = 1 + random.nextInt(random.nextBoolean() ? 3 : 5);
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

    private boolean isInsideChunk(BlockPos pos, int blockX, int blockZ) {
        return pos.getX() >= blockX && pos.getX() < blockX + CHUNK_SIZE && pos.getZ() >= blockZ && pos.getZ() < blockZ + CHUNK_SIZE;
    }

    private boolean isLushRegionChunk(World world, int blockX, int blockZ) {
        for (int dx = 4; dx < CHUNK_SIZE; dx += 4) {
            for (int dz = 4; dz < CHUNK_SIZE; dz += 4) {
                if (isLushRegionColumn(world, blockX + dx, blockZ + dz)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLushRegionColumn(World world, int x, int z) {
        double region = smoothNoise(world.getSeed() ^ LUSH_REGION_SALT, x * LUSH_REGION_SCALE, 0.0D, z * LUSH_REGION_SCALE);
        double detail = smoothNoise(world.getSeed() ^ (LUSH_REGION_SALT << 1), x * 0.036D, 0.0D, z * 0.036D);
        return region * 0.8D + detail * 0.2D > LUSH_REGION_THRESHOLD;
    }

    private boolean isLushCaveRegion(World world, BlockPos pos, BlockPos azaleaTree) {
        double vertical = 1.0D - Math.min(1.0D, Math.abs(pos.getY() - 34.0D) / 42.0D);
        double horizontalFalloff = horizontalFalloff(pos, azaleaTree, 44.0D);
        double region = smoothNoise(world.getSeed() ^ LUSH_REGION_SALT, pos.getX() * LUSH_REGION_SCALE, 0.0D, pos.getZ() * LUSH_REGION_SCALE);
        double detail = smoothNoise(world.getSeed() ^ (LUSH_PATCH_SALT << 2), pos.getX() * 0.046D, pos.getY() * 0.05D, pos.getZ() * 0.046D);
        return region * 0.65D + detail * 0.2D + vertical * 0.18D + horizontalFalloff * 0.42D > 0.12D;
    }

    private boolean isLushPatchNoise(World world, BlockPos pos, BlockPos azaleaTree, double normalizedDistance) {
        double main = smoothNoise(world.getSeed() ^ LUSH_PATCH_SALT, pos.getX() * 0.055D, pos.getY() * 0.08D, pos.getZ() * 0.055D);
        double detail = smoothNoise(world.getSeed() ^ (LUSH_PATCH_SALT << 1), pos.getX() * 0.13D, pos.getY() * 0.18D, pos.getZ() * 0.13D);
        double edgeFalloff = 1.0D - normalizedDistance;
        double horizontalFalloff = horizontalFalloff(pos, azaleaTree, 48.0D);
        return isLushCaveRegion(world, pos, azaleaTree) && main * 0.55D + detail * 0.24D + edgeFalloff * 0.42D + horizontalFalloff * 0.35D > -0.08D;
    }

    private double horizontalFalloff(BlockPos pos, BlockPos center, double radius) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double distanceSq = dx * dx + dz * dz;
        return 1.0D - Math.min(1.0D, distanceSq / (radius * radius));
    }

    private boolean isDripstonePatchNoise(World world, BlockPos pos, double normalizedDistance) {
        double main = smoothNoise(world.getSeed() ^ DRIPSTONE_PATCH_SALT, pos.getX() * 0.06D, pos.getY() * 0.09D, pos.getZ() * 0.06D);
        double ridged = 1.0D - Math.abs(smoothNoise(world.getSeed() ^ (DRIPSTONE_PATCH_SALT << 1), pos.getX() * 0.11D, pos.getY() * 0.15D, pos.getZ() * 0.11D));
        double edgeFalloff = 1.0D - normalizedDistance;
        return main * 0.55D + ridged * 0.35D + edgeFalloff * 0.45D > -0.05D;
    }

    private double smoothNoise(long seed, double x, double y, double z) {
        int x0 = floor(x);
        int y0 = floor(y);
        int z0 = floor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;
        double tx = smoothstep(x - x0);
        double ty = smoothstep(y - y0);
        double tz = smoothstep(z - z0);

        double x00 = lerp(tx, valueNoise(seed, x0, y0, z0), valueNoise(seed, x1, y0, z0));
        double x10 = lerp(tx, valueNoise(seed, x0, y1, z0), valueNoise(seed, x1, y1, z0));
        double x01 = lerp(tx, valueNoise(seed, x0, y0, z1), valueNoise(seed, x1, y0, z1));
        double x11 = lerp(tx, valueNoise(seed, x0, y1, z1), valueNoise(seed, x1, y1, z1));
        double y0Value = lerp(ty, x00, x10);
        double y1Value = lerp(ty, x01, x11);

        return lerp(tz, y0Value, y1Value);
    }

    private double valueNoise(long seed, int x, int y, int z) {
        long hash = seed;
        hash ^= x * 341873128712L;
        hash ^= y * 132897987541L;
        hash ^= z * 42317861L;
        hash ^= hash >> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >> 33;
        return ((hash >>> 11) * 0x1.0p-53D) * 2.0D - 1.0D;
    }

    private int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    private double smoothstep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }

    private boolean isLushReplaceable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.GRAVEL || block == Blocks.CLAY || block == ModBlocks.DeepSlate || block == ModBlocks.ROOTED_DIRT || block == ModBlocks.MOSS_BLOCK;
    }

    private boolean isNaturalCaveBlock(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.COBBLESTONE || block == ModBlocks.DeepSlate || block == ModBlocks.DRIPSTONE_BLOCK;
    }
}
