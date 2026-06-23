package com.canoestudio.retrofuturemc.contents.blocks;

import com.canoestudio.retrofuturemc.contents.SimpleBlockCreator;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

public class RootedDirtBlock extends SimpleBlockCreator {
    public RootedDirtBlock() {
        super("Rooted_Dirt", Material.GROUND, SoundType.GROUND, 0.5F, 0.5F, "shovel", 0);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        EnumPlantType plantType = plantable.getPlantType(world, pos.offset(direction));
        return plantType == EnumPlantType.Plains || plantType == EnumPlantType.Cave || plantable == Blocks.SAPLING || super.canSustainPlant(state, world, pos, direction, plantable);
    }
}
