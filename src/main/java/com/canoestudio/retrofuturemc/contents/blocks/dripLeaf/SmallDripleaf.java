package com.canoestudio.retrofuturemc.contents.blocks.dripLeaf;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.items.ModItems;
import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import git.jbredwards.fluidlogged_api.api.world.IWorldProvider;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import javax.annotation.Nonnull;
import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.tab.CreativeTab.CREATIVE_TABS;

public class SmallDripleaf extends BlockBush implements IGrowable, IShearable, IFluidloggable {
    public static final String name = "Small_Dripleaf";

    public static final PropertyEnum<BlockDoublePlant.EnumBlockHalf> HALF = BlockDoublePlant.HALF;
    public static final PropertyEnum<EnumFacing> FACING = BlockHorizontal.FACING;

    public SmallDripleaf()
    {
        super(Material.VINE);

        setHardness(0.0F);

        setTranslationKey(Tags.MOD_ID + "." + name.toLowerCase());
        setRegistryName(name);
        setCreativeTab(CREATIVE_TABS);
        setSoundType(BigDripleaf.DRIPLEAF);

        this.setDefaultState(this.blockState.getBaseState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER).withProperty(FACING, EnumFacing.SOUTH));

        ModBlocks.BLOCKS.add(this);
        ModItems.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return FULL_BLOCK_AABB; }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return super.canPlaceBlockAt(worldIn, pos) && canPlaceDripleafPartAt(worldIn, pos) && canPlaceDripleafPartAt(worldIn, pos.up());
    }

    @Override
    protected boolean canSustainBush(IBlockState state)
    {
        Block block = state.getBlock();
        return block == Blocks.CLAY || block == ModBlocks.MOSS_BLOCK || block == ModBlocks.ROOTED_DIRT || super.canSustainBush(state);
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

    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        scheduleContainedFluidTick(worldIn, pos, state);
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    }

    protected void checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        if (!this.canBlockStay(worldIn, pos, state))
        {
            boolean flag = state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER;
            BlockPos blockpos = flag ? pos : pos.up();
            BlockPos blockpos1 = flag ? pos.down() : pos;
            Block block = (Block)(flag ? this : worldIn.getBlockState(blockpos).getBlock());
            Block block1 = (Block)(flag ? worldIn.getBlockState(blockpos1).getBlock() : this);

            if (!flag) this.dropBlockAsItem(worldIn, pos, state, 0); //Forge move above the setting to air.

            if (block == this)
            {
                restoreContainedFluidOrAir(worldIn, blockpos, worldIn.getBlockState(blockpos), 2);
            }

            if (block1 == this)
            {
                restoreContainedFluidOrAir(worldIn, blockpos1, worldIn.getBlockState(blockpos1), 3);
            }
        }
    }

    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state)
    {
        if (state.getBlock() != this) return super.canBlockStay(worldIn, pos, state); //Forge: This function is called during world gen and placement, before this block is set, so if we are not 'here' then assume it's the pre-check.
        if (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER)
        {
            return worldIn.getBlockState(pos.down()).getBlock() == this;
        }
        else
        {
            IBlockState iblockstate = worldIn.getBlockState(pos.up());
            return iblockstate.getBlock() == this && super.canBlockStay(worldIn, pos, iblockstate);
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }

    public void placeAt(World worldIn, BlockPos lowerPos, EnumFacing facing, int flags)
    {
        setFluidloggableBlock(worldIn, lowerPos, this.getDefaultState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER).withProperty(FACING, facing), flags);
        setFluidloggableBlock(worldIn, lowerPos.up(), this.getDefaultState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.UPPER).withProperty(FACING, facing), flags);
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        EnumFacing facing = state.getValue(FACING);
        setFluidloggableBlock(worldIn, pos.up(), this.getDefaultState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.UPPER).withProperty(FACING, facing), 2);
    }

    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player)
    {
        if (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER)
        {
            BlockPos lowerPos = pos.down();
            IBlockState lowerState = worldIn.getBlockState(lowerPos);

            if (lowerState.getBlock() == this)
            {
                if (player.capabilities.isCreativeMode)
                {
                    restoreContainedFluidOrAir(worldIn, lowerPos, lowerState, 2);
                }
                else
                {
                    if (!player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() == Items.SHEARS)
                    {
                        this.onHarvest(worldIn, pos, lowerState, player);
                        restoreContainedFluidOrAir(worldIn, lowerPos, lowerState, 2);
                    }
                    else
                    {
                        FluidState lowerFluid = FluidloggedUtils.getFluidState(worldIn, lowerPos, lowerState);
                        worldIn.destroyBlock(lowerPos, true);
                        restoreFluidOrAir(worldIn, lowerPos, lowerFluid, 3);
                    }
                }
            }
        }
        else if (worldIn.getBlockState(pos.up()).getBlock() == this)
        {
            restoreContainedFluidOrAir(worldIn, pos.up(), worldIn.getBlockState(pos.up()), 2);
        }

        super.onBlockHarvested(worldIn, pos, state, player);
    }

    private boolean onHarvest(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player)
    {
        player.addStat(StatList.getBlockStats(this));
        return true;
    }

    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) { return new ItemStack(this); }

    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) { return true; }

    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        return true;
    }

    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        EnumFacing facing = state.getValue(FACING);

        int i = rand.nextInt(4) + 1, heigh = 0;

        for(int j = 0; j <= 5; j++)
        {
            if(canGrowThrough(worldIn, pos.up(j)))
            {
                heigh ++;
            }
        }

        if(i > heigh) i = heigh;

        int dec = state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER ? 1 : 0;

        for(int k = 0; k <= i - 1; k++)
        {
            setFluidloggableBlock(worldIn, pos.up(k - dec), ModBlocks.DRIPLEAF_STEM.getDefaultState().withProperty(FACING, facing), 2);
        }
        setFluidloggableBlock(worldIn, pos.up(i - dec), ModBlocks.BIG_DRIPLEAF.getDefaultState().withProperty(FACING, facing), 3);
    }

    private boolean canPlaceDripleafPartAt(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        FluidState fluidState = FluidloggedUtils.getFluidState(world, pos, state);
        return state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER || fluidState.getFluid() == FluidRegistry.WATER;
    }

    private boolean canGrowThrough(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR || state.getBlock() == ModBlocks.SMALL_DRIPLEAF || state.getBlock() == Blocks.WATER || FluidloggedUtils.getFluidState(world, pos, state).getFluid() == FluidRegistry.WATER;
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
        restoreFluidOrAir(world, pos, fluidState, flags);
    }

    private void restoreFluidOrAir(World world, BlockPos pos, FluidState fluidState, int flags)
    {
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
        if(meta > 3)
        {
            return this.getDefaultState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.UPPER).withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
        }
        else {
            return this.getDefaultState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER).withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
        }
    }

    public int getMetaFromState(IBlockState state)
    {
        int i = state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER ? 0 : 4;
        return state.getValue(FACING).getHorizontalIndex() + i;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {HALF, FACING});
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) { return world.getBlockState(pos).getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER; }

    @Override
    public java.util.List<ItemStack> onSheared(ItemStack item, net.minecraft.world.IBlockAccess world, BlockPos pos, int fortune)
    {
        return NonNullList.withSize(1, new ItemStack(this));
    }

}
