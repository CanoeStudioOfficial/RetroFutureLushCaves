package com.canoestudio.retrofuturemc.contents.blocks.dripLeaf;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.items.ModItems;
import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import com.canoestudio.retrofuturemc.sounds.ModSoundHandler;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.tab.CreativeTab.CREATIVE_TABS;

public class BigDripleaf extends BlockBush implements IGrowable, IFluidloggable {
    public static final String name = "Big_Dripleaf";
    public static final SoundType DRIPLEAF = new SoundType(1.0F, 1.0F, ModSoundHandler.BLOCK_BIG_DRIPLEAF_BREAK, ModSoundHandler.BLOCK_BIG_DRIPLEAF_STEP, ModSoundHandler.BLOCK_BIG_DRIPLEAF_PLACE, ModSoundHandler.BLOCK_BIG_DRIPLEAF_HIT, ModSoundHandler.BLOCK_BIG_DRIPLEAF_FALL);

    protected static final AxisAlignedBB HALF_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    protected static final AxisAlignedBB DRIP_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.9375D, 1.0D);

    public static final PropertyEnum<EnumFacing> FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<BigDripleaf.EnumTilt> TILT = PropertyEnum.<BigDripleaf.EnumTilt>create("tilt", BigDripleaf.EnumTilt.class);

    public BigDripleaf() {
        super(Material.VINE);

        setHardness(0.0F);

        setTranslationKey(Tags.MOD_ID + "." + name.toLowerCase());
        setRegistryName(name);
        setCreativeTab(CREATIVE_TABS);
        setHardness(0.1F);
        setResistance(0.1F);
        setHarvestLevel("axe", 0);

        setSoundType(BigDripleaf.DRIPLEAF);

        this.setTickRandomly(true);

        ModBlocks.BLOCKS.add(this);
        ModItems.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));

        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH).withProperty(TILT, EnumTilt.NONE));
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return FULL_BLOCK_AABB; }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        EnumTilt tilt = blockState.getValue(TILT);

        if(tilt == EnumTilt.FULL)
            return NULL_AABB;
        else if(tilt == EnumTilt.PARTIAL)
            return HALF_AABB;

        return DRIP_AABB;
    }

    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        EnumTilt tilt = state.getValue(TILT);
        
        if (tilt == EnumTilt.UNSTABLE)
        {
            if (!worldIn.isBlockPowered(pos))
            {
                worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.NONE), 2);
            }
        }
        else if (tilt == EnumTilt.PARTIAL)
        {
            AxisAlignedBB checkAABB = new AxisAlignedBB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
            if (worldIn.getEntitiesWithinAABBExcludingEntity(null, checkAABB).isEmpty())
            {
                worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.NONE), 2);
                worldIn.playSound(null, pos, ModSoundHandler.BLOCK_BIG_DRIPLEAF_BREAK, SoundCategory.BLOCKS, 0.8F, 0.8F);
            }
            else
            {
                worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.FULL), 2);
                worldIn.playSound(null, pos, ModSoundHandler.BLOCK_BIG_DRIPLEAF_BREAK, SoundCategory.BLOCKS, 0.8F, 0.8F);
                worldIn.scheduleUpdate(pos, this, 100);
            }
        }
        else if (tilt == EnumTilt.FULL)
        {
            AxisAlignedBB checkAABB = new AxisAlignedBB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
            if (worldIn.getEntitiesWithinAABBExcludingEntity(null, checkAABB).isEmpty())
            {
                worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.NONE), 2);
                worldIn.playSound(null, pos, ModSoundHandler.BLOCK_BIG_DRIPLEAF_BREAK, SoundCategory.BLOCKS, 0.8F, 0.8F);
            }
        }
    }

    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        if (worldIn.isRemote) return;
        
        EnumTilt tilt = state.getValue(TILT);

        if (tilt == EnumTilt.NONE)
        {
            worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.PARTIAL), 2);
            worldIn.playSound(null, pos, ModSoundHandler.BLOCK_BIG_DRIPLEAF_BREAK, SoundCategory.BLOCKS, 0.8F, 0.8F);
            worldIn.scheduleUpdate(pos, this, 10);
        }
    }

    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        if (!canBlockStay(worldIn, pos, state))
        {
            if (!worldIn.isRemote)
            {
                dropBlockAsItem(worldIn, pos, state, 0);
                restoreContainedFluidOrAir(worldIn, pos, state, 3);
            }
            return;
        }

        if (!worldIn.isRemote)
        {
            boolean isPowered = worldIn.isBlockPowered(pos);
            EnumTilt tilt = state.getValue(TILT);

            if (isPowered)
            {
                if (tilt != EnumTilt.UNSTABLE)
                {
                    worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.UNSTABLE), 2);
                }
            }
            else
            {
                if (tilt == EnumTilt.UNSTABLE)
                {
                    worldIn.setBlockState(pos, state.withProperty(TILT, EnumTilt.NONE), 2);
                }
            }
        }
        scheduleContainedFluidTick(worldIn, pos, state);
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        IBlockState downState = worldIn.getBlockState(pos.down());
        Block downBlock = downState.getBlock();
        
        if (downBlock == ModBlocks.DRIPLEAF_STEM)
        {
            return true;
        }
        
        if (downBlock == ModBlocks.BIG_DRIPLEAF && isTopDripleaf(worldIn, pos.down()))
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

    private boolean isTopDripleaf(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.BIG_DRIPLEAF)
        {
            return false;
        }
        
        IBlockState upState = world.getBlockState(pos.up());
        return upState.getBlock() != ModBlocks.BIG_DRIPLEAF && upState.getBlock() != ModBlocks.DRIPLEAF_STEM;
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

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(worldIn, pos, state);
        
        IBlockState downState = worldIn.getBlockState(pos.down());
        if (downState.getBlock() == ModBlocks.BIG_DRIPLEAF)
        {
            EnumFacing facing = state.getValue(FACING);
            setFluidloggableBlock(worldIn, pos.down(), ModBlocks.DRIPLEAF_STEM.getDefaultState().withProperty(FACING, facing), 2);
        }
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

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        ItemStack heldItem = playerIn.getHeldItem(hand);
        
        if (heldItem.getItem() == Items.DYE && heldItem.getMetadata() == 15)
        {
            if (canGrowWithBonemeal(worldIn, pos, state))
            {
                if (!worldIn.isRemote)
                {
                    growWithBonemeal(worldIn, pos, state);
                    if (!playerIn.capabilities.isCreativeMode)
                    {
                        heldItem.shrink(1);
                    }
                }
                return true;
            }
        }
        
        return false;
    }

    private boolean canGrowWithBonemeal(World world, BlockPos pos, IBlockState state)
    {
        BlockPos topPos = findTopPosition(world, pos);
        IBlockState topState = world.getBlockState(topPos);
        
        if (topState.getBlock() != ModBlocks.BIG_DRIPLEAF)
        {
            return false;
        }
        
        BlockPos aboveTop = topPos.up();
        return canGrowInto(world, aboveTop);
    }

    private BlockPos findTopPosition(World world, BlockPos pos)
    {
        BlockPos checkPos = pos;
        while (true)
        {
            IBlockState upState = world.getBlockState(checkPos.up());
            if (upState.getBlock() == ModBlocks.BIG_DRIPLEAF || upState.getBlock() == ModBlocks.DRIPLEAF_STEM)
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

    private void growWithBonemeal(World world, BlockPos pos, IBlockState state)
    {
        BlockPos topPos = findTopPosition(world, pos);
        IBlockState topState = world.getBlockState(topPos);
        EnumFacing facing = topState.getValue(FACING);
        
        setFluidloggableBlock(world, topPos, ModBlocks.DRIPLEAF_STEM.getDefaultState().withProperty(FACING, facing), 2);
        setFluidloggableBlock(world, topPos.up(), this.getDefaultState().withProperty(FACING, facing), 3);
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

    private void scheduleContainedFluidTick(World world, BlockPos pos, IBlockState state)
    {
        FluidState fluidState = FluidloggedUtils.getFluidState(world, pos, state);

        if (fluidState.getFluid() == FluidRegistry.WATER)
        {
            world.scheduleUpdate(pos, fluidState.getState().getBlock(), fluidState.getState().getBlock().tickRate(world));
        }
    }

    private void restoreContainedFluidOrAir(World world, BlockPos pos, IBlockState state, int flags)
    {
        FluidState fluidState = FluidloggedUtils.getFluidState(world, pos, state);
        world.setBlockState(pos, fluidState.getFluid() == FluidRegistry.WATER ? fluidState.getState() : Blocks.AIR.getDefaultState(), flags);
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
        int i = meta / 4;
        IBlockState state = this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(i));

        int j = meta % 4;

        return state.withProperty(TILT, EnumTilt.byMetadata(j));
    }

    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TILT).getMetadata() + state.getValue(FACING).getHorizontalIndex() * 4;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING, TILT});
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) { 
        return canGrowWithBonemeal(worldIn, pos, state); 
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) { 
        return true; 
    }

    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        if (canGrowWithBonemeal(worldIn, pos, state))
        {
            growWithBonemeal(worldIn, pos, state);
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) 
    { 
        return Item.getItemFromBlock(this); 
    }

    public static enum EnumTilt implements IStringSerializable
    {
        NONE(0, "none"),
        PARTIAL(1, "partial"),
        FULL(2, "full"),
        UNSTABLE(3, "unstable");

        private static final BigDripleaf.EnumTilt[] META_LOOKUP = new BigDripleaf.EnumTilt[values().length];
        private final int meta;
        private final String name;

        private EnumTilt(int metaIn, String nameIn)
        {
            this.meta = metaIn;
            this.name = nameIn;
        }

        public int getMetadata()
        {
            return this.meta;
        }

        public String toString()
        {
            return this.name;
        }

        public static BigDripleaf.EnumTilt byMetadata(int meta)
        {
            if (meta < 0 || meta >= META_LOOKUP.length)
            {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public String getName()
        {
            return this.name;
        }

        static
        {
            for (BigDripleaf.EnumTilt bigdripleaf$enumtilt : values())
            {
                META_LOOKUP[bigdripleaf$enumtilt.getMetadata()] = bigdripleaf$enumtilt;
            }
        }
    }

}
