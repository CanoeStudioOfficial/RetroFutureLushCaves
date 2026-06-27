package com.canoestudio.retrofuturemc.contents.blocks.dripLeaf;


import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import git.jbredwards.fluidlogged_api.api.world.IWorldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import javax.annotation.Nonnull;
import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.tab.CreativeTab.CREATIVE_TABS;

public class DripleafStem extends BlockBush implements IGrowable, IFluidloggable
{
    public static final String name = "Big_Dripleaf_Stem";
    public static final PropertyEnum<EnumFacing> FACING = BlockHorizontal.FACING;

    public DripleafStem()
    {
        super(Material.VINE);

        setHardness(0.0F);

        setTranslationKey(Tags.MOD_ID + "." + name.toLowerCase());
        setRegistryName(name);
        setCreativeTab(CREATIVE_TABS);
        setSoundType(BigDripleaf.DRIPLEAF);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
        this.setTickRandomly(true);

        ModBlocks.BLOCKS.add(this);
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return FULL_BLOCK_AABB; }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (!worldIn.isRemote)
        {
            if (!this.canBlockStay(worldIn, pos, state))
            {
                this.dropBlockAsItem(worldIn, pos, state, 0);
                restoreContainedFluidOrAir(worldIn, pos, state, 3);
            }
            else if (!hasDripleafAbove(worldIn, pos))
            {
                setFluidloggableBlock(worldIn, pos, ModBlocks.BIG_DRIPLEAF.getDefaultState().withProperty(FACING, state.getValue(FACING)), 3);
            }
        }
    }

    private boolean hasDripleafAbove(World world, BlockPos pos)
    {
        IBlockState upState = world.getBlockState(pos.up());
        return upState.getBlock() == ModBlocks.DRIPLEAF_STEM || upState.getBlock() == ModBlocks.BIG_DRIPLEAF;
    }

    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        scheduleContainedFluidTick(worldIn, pos, state);
        if (!worldIn.isRemote)
        {
            worldIn.scheduleUpdate(pos, this, 1);
        }
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        IBlockState downState = worldIn.getBlockState(pos.down());
        Block downBlock = downState.getBlock();
        
        if (downBlock == ModBlocks.DRIPLEAF_STEM)
        {
            return true;
        }
        
        if (downBlock == ModBlocks.BIG_DRIPLEAF)
        {
            return true;
        }

        if (canSustainDripleaf(downBlock))
        {
            return true;
        }
        
        if (downBlock.canSustainPlant(downState, worldIn, pos.down(), EnumFacing.UP, this))
        {
            return true;
        }
        
        return false;
    }

    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state)
    {
        IBlockState downState = worldIn.getBlockState(pos.down());
        Block downBlock = downState.getBlock();

        if (downBlock == ModBlocks.DRIPLEAF_STEM)
        {
            return true;
        }
        
        if (downBlock == ModBlocks.BIG_DRIPLEAF)
        {
            return true;
        }

        if (canSustainDripleaf(downBlock))
        {
            return true;
        }
        
        return downBlock.canSustainPlant(downState, worldIn, pos.down(), EnumFacing.UP, this);
    }

    private boolean canSustainDripleaf(Block block)
    {
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.ROOTED_DIRT || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM || block == Blocks.FARMLAND;
    }

    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) { return false; }

    @Override
    public boolean isFluidValid(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Fluid fluid) {
        return FluidloggedUtils.isCompatibleFluid(FluidRegistry.WATER, fluid);
    }

    @Override
    public boolean isFluidloggable(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull FluidState fluidState) {
        return fluidState.isEmpty() || fluidState.isFluidloggable() && isFluidValid(state, IWorldProvider.getWorld(world), pos, fluidState.getFluid());
    }

    @Nonnull
    @Override
    public EnumActionResult onFluidFill(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState here, @Nonnull FluidState newFluid, int blockFlags) {
        return EnumActionResult.PASS;
    }

    @Nonnull
    @Override
    public EnumActionResult onFluidDrain(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState here, int blockFlags) {
        return EnumActionResult.PASS;
    }

    @Override
    public boolean canFluidFlow(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState here, @Nonnull EnumFacing side) {
        return true;
    }

    @Override
    public boolean canFluidConnect(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState here, @Nonnull EnumFacing side) {
        return true;
    }

    @Override
    public boolean overrideApplyDefaultsSetting() { return true; }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Item.getItemFromBlock(ModBlocks.BIG_DRIPLEAF); }

    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) { return new ItemStack(ModBlocks.BIG_DRIPLEAF); }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) { return true; }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) { return true; }

    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        BlockPos topPos = findTopPosition(worldIn, pos);
        IBlockState topState = worldIn.getBlockState(topPos);
        EnumFacing facing = topState.getValue(FACING);
        
        BlockPos aboveTop = topPos.up();
        
        if (canGrowInto(worldIn, aboveTop))
        {
            if (topState.getBlock() == ModBlocks.BIG_DRIPLEAF)
            {
                setFluidloggableBlock(worldIn, topPos, this.getDefaultState().withProperty(FACING, facing), 2);
            }
            setFluidloggableBlock(worldIn, aboveTop, ModBlocks.BIG_DRIPLEAF.getDefaultState().withProperty(FACING, facing), 3);
        }
    }

    private boolean canGrowInto(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isReplaceable(world, pos) || state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER || FluidloggedUtils.getFluidState(world, pos, state).getFluid() == FluidRegistry.WATER;
    }

    private void setFluidloggableBlock(World world, BlockPos pos, IBlockState newState, int flags)
    {
        FluidState fluidState = getWaterFluidState(world, pos);

        if (fluidState.getFluid() == FluidRegistry.WATER)
        {
            world.setBlockState(pos, newState, flags);
            FluidloggedUtils.setFluidState(world, pos, world.getBlockState(pos), fluidState, false, flags);
            world.scheduleUpdate(pos, fluidState.getState().getBlock(), fluidState.getState().getBlock().tickRate(world));
        }
        else
        {
            world.setBlockState(pos, newState, flags);
        }
    }

    private boolean hasWaterFluid(World world, BlockPos pos)
    {
        return getWaterFluidState(world, pos).getFluid() == FluidRegistry.WATER;
    }

    private FluidState getWaterFluidState(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() == Material.WATER ? FluidState.of(state) : FluidloggedUtils.getFluidState(world, pos, state);
    }

    private void restoreContainedFluidOrAir(World world, BlockPos pos, IBlockState state, int flags)
    {
        FluidState fluidState = FluidloggedUtils.getFluidState(world, pos, state);
        world.setBlockState(pos, fluidState.getFluid() == FluidRegistry.WATER ? fluidState.getState() : Blocks.AIR.getDefaultState(), flags);
        scheduleFluidTick(world, pos, fluidState);
    }

    private void scheduleContainedFluidTick(World world, BlockPos pos, IBlockState state)
    {
        scheduleFluidTick(world, pos, FluidloggedUtils.getFluidState(world, pos, state));
    }

    private void scheduleFluidTick(World world, BlockPos pos, FluidState fluidState)
    {
        if (fluidState.getFluid() == FluidRegistry.WATER)
        {
            world.scheduleUpdate(pos, fluidState.getState().getBlock(), fluidState.getState().getBlock().tickRate(world));
        }
    }

    private BlockPos findTopPosition(World world, BlockPos pos)
    {
        BlockPos checkPos = pos;
        while (true)
        {
            IBlockState upState = world.getBlockState(checkPos.up());
            if (upState.getBlock() == ModBlocks.DRIPLEAF_STEM || upState.getBlock() == ModBlocks.BIG_DRIPLEAF)
            {
                checkPos = checkPos.up();
            }
            else
            {
                break;
            }
        }
        return checkPos;
    }

    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(FACING).getHorizontalIndex();
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }
}
