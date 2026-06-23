package com.canoestudio.retrofuturemc.contents.world.gen;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.blocks.PointedDripstoneBlock;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVine;
import com.canoestudio.retrofuturemc.contents.blocks.CaveVine.CaveVinePlant;
import com.canoestudio.retrofuturemc.contents.blocks.dripLeaf.SmallDripleaf;
import com.canoestudio.retrofuturemc.contents.mobs.axolotl.EntityAxolotl;
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
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.List;
import java.util.Random;

public class RetroFutureCaveWorldGenerator implements IWorldGenerator {
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_MARGIN = 5;
    private static final int AZALEA_TREE_CHANCE = 9;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) {
            return;
        }

        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;

        if (random.nextInt(AZALEA_TREE_CHANCE) == 0) {
            BlockPos azaleaTree = findSurfaceAzaleaTreePos(world, random, blockX, blockZ);
            BlockPos lushCenter = azaleaTree == null ? null : findCavePocketBelowAzalea(world, random, azaleaTree, blockX, blockZ);

            if (lushCenter != null && generateSurfaceAzaleaTree(world, random, azaleaTree)) {
                placeRootTrail(world, random, azaleaTree, lushCenter);
                generateLushPocket(world, random, lushCenter, blockX, blockZ);
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
        BlockPos start = world.getHeight(new BlockPos(blockX + 7 + random.nextInt(2), 0, blockZ + 7 + random.nextInt(2)));

        if (start.getY() < 50 || start.getY() > world.getActualHeight() - 16) {
            return null;
        }

        return start;
    }

    private boolean generateSurfaceAzaleaTree(World world, Random random, BlockPos start) {
        return new WorldGenBigAzaleaTree(true).generate(world, random, start);
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
        int minY = 12;
        int maxY = Math.min(56, azaleaTree.getY() - 8);

        if (maxY <= minY) {
            return null;
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos pos = new BlockPos(
                    azaleaTree.getX() + random.nextInt(7) - 3,
                    minY + random.nextInt(maxY - minY),
                    azaleaTree.getZ() + random.nextInt(7) - 3);

            if (!isInsideChunk(pos, blockX, blockZ)) {
                continue;
            }

            if (world.isAirBlock(pos) && !world.canSeeSky(pos) && hasNearbySolid(world, pos, 4)) {
                return pos;
            }
        }

        return null;
    }

    private void placeRootTrail(World world, Random random, BlockPos azaleaTree, BlockPos lushCenter) {
        int bottomY = Math.max(lushCenter.getY() + 2, 4);

        for (int y = azaleaTree.getY() - 2; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(azaleaTree.getX(), y, azaleaTree.getZ());
            Block block = world.getBlockState(pos).getBlock();

            if (block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.STONE || block == Blocks.GRAVEL || block == ModBlocks.MOSS_BLOCK) {
                if (random.nextInt(4) != 0) {
                    world.setBlockState(pos, ModBlocks.ROOTED_DIRT.getDefaultState(), 2);
                }
            } else if (world.isAirBlock(pos) && random.nextInt(3) == 0 && ModBlocks.HANGING_ROOTS.canPlaceBlockAt(world, pos)) {
                world.setBlockState(pos, ModBlocks.HANGING_ROOTS.getDefaultState(), 2);
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

    private void generateLushPocket(World world, Random random, BlockPos center, int blockX, int blockZ) {
        int radius = 3 + random.nextInt(3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = (dx * dx) / (double)(radius * radius) + (dy * dy) / 12.0D + (dz * dz) / (double)(radius * radius);

                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);

                    if (!isInsideChunk(pos, blockX, blockZ)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isLushReplaceable(state.getBlock())) {
                        continue;
                    }

                    decorateLushFloor(world, random, pos, blockX, blockZ);
                    decorateLushCeiling(world, random, pos);
                }
            }
        }

        spawnAxolotlNearWater(world, random, center, radius + 1, blockX, blockZ);
    }

    private void decorateLushFloor(World world, Random random, BlockPos pos, int blockX, int blockZ) {
        BlockPos above = pos.up();

        if (!world.isAirBlock(above)) {
            return;
        }

        if (random.nextInt(100) < 82) {
            world.setBlockState(pos, ModBlocks.MOSS_BLOCK.getDefaultState(), 2);
        } else if (random.nextInt(100) < 18) {
            world.setBlockState(pos, ModBlocks.ROOTED_DIRT.getDefaultState(), 2);
        }

        if (!world.isAirBlock(above)) {
            return;
        }

        int roll = random.nextInt(100);

        if (roll < 15) {
            world.setBlockState(above, ModBlocks.MOSS_CARPET.getDefaultState(), 2);
        } else if (roll < 19 && ModBlocks.Azalea.canPlaceBlockAt(world, above)) {
            world.setBlockState(above, ModBlocks.Azalea.getDefaultState(), 2);
        } else if (roll < 22 && ModBlocks.Flowering_Azalea.canPlaceBlockAt(world, above)) {
            world.setBlockState(above, ModBlocks.Flowering_Azalea.getDefaultState(), 2);
        } else if (roll < 27 && isNearWater(world, above, 3, blockX, blockZ) && world.isAirBlock(above.up())) {
            ((SmallDripleaf)ModBlocks.SMALL_DRIPLEAF).placeAt(world, above, EnumFacing.byHorizontalIndex(random.nextInt(4)), 2);
        }
    }

    private void decorateLushCeiling(World world, Random random, BlockPos pos) {
        BlockPos below = pos.down();

        if (!world.isAirBlock(below)) {
            return;
        }

        if (random.nextInt(100) < 8 && ModBlocks.SPORE_BLOSSOM.canPlaceBlockAt(world, below)) {
            world.setBlockState(below, ModBlocks.SPORE_BLOSSOM.getDefaultState(), 2);
        } else if (random.nextInt(100) < 14 && ModBlocks.HANGING_ROOTS.canPlaceBlockAt(world, below)) {
            world.setBlockState(below, ModBlocks.HANGING_ROOTS.getDefaultState(), 2);
        } else if (random.nextInt(100) < 20) {
            placeCaveVine(world, random, below);
        }
    }

    private void placeCaveVine(World world, Random random, BlockPos start) {
        int length = 2 + random.nextInt(5);

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
        int radius = 3 + random.nextInt(3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = (dx * dx) / (double)(radius * radius) + (dy * dy) / 16.0D + (dz * dz) / (double)(radius * radius);

                    if (distance > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);

                    if (!isInsideChunk(pos, blockX, blockZ)) {
                        continue;
                    }

                    IBlockState state = world.getBlockState(pos);

                    if (!isNaturalCaveBlock(state.getBlock())) {
                        continue;
                    }

                    if (random.nextInt(100) < 28) {
                        world.setBlockState(pos, ModBlocks.DRIPSTONE_BLOCK.getDefaultState(), 2);
                    }

                    if (world.isAirBlock(pos.down()) && random.nextInt(100) < 26) {
                        placePointedDripstone(world, random, pos.down(), EnumFacing.DOWN);
                    }

                    if (world.isAirBlock(pos.up()) && random.nextInt(100) < 18) {
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

    private boolean isLushReplaceable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.GRAVEL || block == Blocks.CLAY || block == ModBlocks.DeepSlate || block == ModBlocks.ROOTED_DIRT || block == ModBlocks.MOSS_BLOCK;
    }

    private boolean isNaturalCaveBlock(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.COBBLESTONE || block == ModBlocks.DeepSlate || block == ModBlocks.DRIPSTONE_BLOCK;
    }
}
