package com.canoestudio.retrofuturemc.contents.blocks;

import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import com.google.common.base.Predicate;
import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import git.jbredwards.fluidlogged_api.api.world.IWorldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Random;

import static com.canoestudio.retrofuturemc.contents.tab.CreativeTab.CREATIVE_TABS;

public class PointedDripstoneBlock extends Block implements IFluidloggable {
    public static final PropertyDirection VERTICAL_DIRECTION = PropertyDirection.create("vertical_direction", new Predicate<EnumFacing>() {
        @Override
        public boolean apply(EnumFacing input) {
            return input != null && input.getAxis() == EnumFacing.Axis.Y;
        }
    });
    public static final PropertyEnum<Thickness> THICKNESS = PropertyEnum.create("thickness", Thickness.class);

    private static final AxisAlignedBB TIP_UP_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 0.8125D, 0.625D);
    private static final AxisAlignedBB TIP_DOWN_AABB = new AxisAlignedBB(0.375D, 0.1875D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final AxisAlignedBB FRUSTUM_UP_AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final AxisAlignedBB FRUSTUM_DOWN_AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final AxisAlignedBB MIDDLE_AABB = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
    private static final AxisAlignedBB BASE_AABB = new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 1.0D, 0.8125D);
    private static final int STALACTITE_FALL_DELAY = 2;
    private static final int STALAGMITE_BREAK_DELAY = 1;
    private static final int MAX_FALLING_STALACTITE_BLOCKS = 32;
    private static final int STALACTITE_MAX_DAMAGE = 40;
    private static final Field FALL_HURT_AMOUNT_FIELD = findFallingBlockField("fallHurtAmount", "field_145816_i");
    private static final Field FALL_HURT_MAX_FIELD = findFallingBlockField("fallHurtMax", "field_145815_h");

    public PointedDripstoneBlock() {
        super(Material.ROCK);
        setTranslationKey(Tags.MOD_ID + ".pointed_dripstone");
        setRegistryName("pointed_dripstone");
        setHardness(1.5F);
        setResistance(3.0F);
        setHarvestLevel("pickaxe", 0);
        setSoundType(SoundType.STONE);
        setCreativeTab(CREATIVE_TABS);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(VERTICAL_DIRECTION, EnumFacing.DOWN).withProperty(THICKNESS, Thickness.TIP));

        ModBlocks.BLOCKS.add(this);
        ModBlocks.BLOCKITEMS.add(new ItemBlock(this).setRegistryName("pointed_dripstone"));
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return canAttach(worldIn, pos, EnumFacing.UP) || canAttach(worldIn, pos, EnumFacing.DOWN);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
        EnumFacing direction = facing == EnumFacing.UP ? EnumFacing.UP : EnumFacing.DOWN;

        if (facing.getAxis() != EnumFacing.Axis.Y) {
            direction = canAttach(worldIn, pos, EnumFacing.DOWN) && !canAttach(worldIn, pos, EnumFacing.UP) ? EnumFacing.DOWN : EnumFacing.UP;
        }

        return getDefaultState().withProperty(VERTICAL_DIRECTION, direction);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote && !canBlockStay(worldIn, pos, state)) {
            breakUnsupportedBlock(worldIn, pos, state);
            return;
        }

        updateColumn(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!canBlockStay(worldIn, pos, state)) {
            if (!worldIn.isRemote) {
                int delay = state.getValue(VERTICAL_DIRECTION) == EnumFacing.DOWN ? STALACTITE_FALL_DELAY : STALAGMITE_BREAK_DELAY;
                worldIn.scheduleUpdate(pos, this, delay);
            }

            return;
        }

        updateColumn(worldIn, pos);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (state.getBlock() != this) {
            return;
        }

        if (!canBlockStay(worldIn, pos, state)) {
            if (state.getValue(VERTICAL_DIRECTION) == EnumFacing.DOWN) {
                spawnFallingStalactiteColumn(worldIn, pos);
            } else {
                breakUnsupportedBlock(worldIn, pos, state);
            }

            return;
        }

        updateColumn(worldIn, pos);
    }

    private boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        EnumFacing direction = state.getValue(VERTICAL_DIRECTION);
        return canAttach(world, pos, direction);
    }

    private boolean canAttach(World world, BlockPos pos, EnumFacing direction) {
        BlockPos supportPos = pos.offset(direction.getOpposite());
        IBlockState support = world.getBlockState(supportPos);
        return support.getBlock() == this && support.getValue(VERTICAL_DIRECTION) == direction || support.isSideSolid(world, supportPos, direction);
    }

    public void placeColumn(World world, BlockPos start, EnumFacing direction, int length, int flags) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(start);

        for (int i = 0; i < length; i++) {
            BlockPos placePos = mutable.toImmutable();

            if (!isAirOrWater(world, placePos)) {
                break;
            }

            setFluidloggableBlock(world, placePos, getDefaultState().withProperty(VERTICAL_DIRECTION, direction), flags);
            mutable.move(direction);
        }

        updateColumn(world, start);
    }

    public void updateColumn(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);

        if (state.getBlock() != this) {
            return;
        }

        EnumFacing direction = state.getValue(VERTICAL_DIRECTION);
        BlockPos check = findColumnBase(world, pos, direction);

        for (int i = 0; i < 16; i++) {
            IBlockState here = world.getBlockState(check);

            if (here.getBlock() != this || here.getValue(VERTICAL_DIRECTION) != direction) {
                break;
            }

            IBlockState updated = here.withProperty(THICKNESS, getThickness(world, check, direction));

            if (updated != here) {
                setFluidloggableBlock(world, check, updated, 2);
            }

            check = check.offset(direction);
        }
    }

    private BlockPos findColumnBase(World world, BlockPos pos, EnumFacing direction) {
        BlockPos check = pos;

        for (int i = 0; i < 16; i++) {
            BlockPos behind = check.offset(direction.getOpposite());
            IBlockState state = world.getBlockState(behind);

            if (state.getBlock() == this && state.getValue(VERTICAL_DIRECTION) == direction) {
                check = behind;
            } else {
                break;
            }
        }

        return check;
    }

    private Thickness getThickness(World world, BlockPos pos, EnumFacing direction) {
        boolean sameBehind = isPointed(world, pos.offset(direction.getOpposite()), direction);
        boolean sameAhead = isPointed(world, pos.offset(direction), direction);
        boolean oppositeAhead = isPointed(world, pos.offset(direction), direction.getOpposite());

        if (!sameAhead) {
            return oppositeAhead ? Thickness.TIP_MERGE : Thickness.TIP;
        }

        if (!sameBehind) {
            return Thickness.BASE;
        }

        return isPointed(world, pos.offset(direction).offset(direction), direction) ? Thickness.MIDDLE : Thickness.FRUSTUM;
    }

    private boolean isPointed(World world, BlockPos pos, EnumFacing direction) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == this && state.getValue(VERTICAL_DIRECTION) == direction;
    }

    private void spawnFallingStalactiteColumn(World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }

        BlockPos fallPos = pos;

        for (int i = 0; i < MAX_FALLING_STALACTITE_BLOCKS; i++) {
            IBlockState fallState = world.getBlockState(fallPos);

            if (!isStalactite(fallState)) {
                break;
            }

            EntityFallingBlock entity = spawnFallingDripstoneBlock(world, fallPos, fallState);

            if (isTip(fallState, true)) {
                int size = Math.max(1 + pos.getY() - fallPos.getY(), 6);
                configureStalactiteDamage(entity, size, STALACTITE_MAX_DAMAGE);
                break;
            }

            fallPos = fallPos.down();
        }
    }

    private EntityFallingBlock spawnFallingDripstoneBlock(World world, BlockPos pos, IBlockState state) {
        EntityFallingBlock entity = new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
        entity.fallTime = 1;
        setBlockStateKeepingFluid(world, pos, 3);
        world.spawnEntity(entity);
        return entity;
    }

    private void configureStalactiteDamage(EntityFallingBlock entity, float damagePerFallDistance, int maxDamage) {
        entity.setHurtEntities(true);
        setFallingBlockFloat(entity, FALL_HURT_AMOUNT_FIELD, damagePerFallDistance);
        setFallingBlockInt(entity, FALL_HURT_MAX_FIELD, maxDamage);
    }

    private static Field findFallingBlockField(String deobfuscatedName, String srgName) {
        try {
            return ReflectionHelper.findField(EntityFallingBlock.class, deobfuscatedName, srgName);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void setFallingBlockFloat(EntityFallingBlock entity, Field field, float value) {
        if (field == null) {
            return;
        }

        try {
            field.setFloat(entity, value);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
        }
    }

    private static void setFallingBlockInt(EntityFallingBlock entity, Field field, int value) {
        if (field == null) {
            return;
        }

        try {
            field.setInt(entity, value);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
        }
    }

    private boolean isStalactite(IBlockState state) {
        return state.getBlock() == this && state.getValue(VERTICAL_DIRECTION) == EnumFacing.DOWN;
    }

    private boolean isTip(IBlockState state, boolean includeMergedTip) {
        if (state.getBlock() != this) {
            return false;
        }

        Thickness thickness = state.getValue(THICKNESS);
        return thickness == Thickness.TIP || includeMergedTip && thickness == Thickness.TIP_MERGE;
    }

    private void breakUnsupportedBlock(World world, BlockPos pos, IBlockState state) {
        dropBlockAsItem(world, pos, state, 0);
        setBlockStateKeepingFluid(world, pos, 3);
    }

    @Override
    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance) {
        IBlockState state = worldIn.getBlockState(pos);

        if (state.getBlock() == this && state.getValue(VERTICAL_DIRECTION) == EnumFacing.UP && state.getValue(THICKNESS) == Thickness.TIP) {
            entityIn.fall(fallDistance + 2.5F, 2.0F);
        } else {
            super.onFallenUpon(worldIn, pos, entityIn, fallDistance);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        Thickness thickness = state.getValue(THICKNESS);

        if (thickness == Thickness.BASE) {
            return BASE_AABB;
        }

        if (thickness == Thickness.MIDDLE) {
            return MIDDLE_AABB;
        }

        if (thickness == Thickness.FRUSTUM) {
            return state.getValue(VERTICAL_DIRECTION) == EnumFacing.UP ? FRUSTUM_UP_AABB : FRUSTUM_DOWN_AABB;
        }

        return state.getValue(VERTICAL_DIRECTION) == EnumFacing.UP ? TIP_UP_AABB : TIP_DOWN_AABB;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return getBoundingBox(blockState, worldIn, pos);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isFluidValid(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Fluid fluid) {
        return FluidloggedUtils.isCompatibleFluid(FluidRegistry.WATER, fluid);
    }

    @Override
    public boolean isFluidloggable(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull FluidState fluidState) {
        if (fluidState.isEmpty()) return true;
        return isFluidValid(state, IWorldProvider.getWorld(world), pos, fluidState.getFluid())
                && (fluidState.isSource() || fluidState.getActualHeight(world, pos) >= 1 && FluidloggedUtils.canCreateSource(fluidState.getState(), IWorldProvider.getWorld(world), pos));
    }

    @Override
    public boolean canFluidFlow(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState here, @Nonnull EnumFacing side) {
        return true;
    }

    @Override
    public boolean overrideApplyDefaultsSetting() {
        return true;
    }

    private boolean isAirOrWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR || state.getMaterial() == Material.WATER;
    }

    private void setFluidloggableBlock(World world, BlockPos pos, IBlockState newState, int flags) {
        FluidState fluidState = getWaterFluidState(world, pos);

        if (fluidState.getFluid() == FluidRegistry.WATER) {
            world.setBlockState(pos, newState, flags);
            FluidloggedUtils.setFluidState(world, pos, world.getBlockState(pos), fluidState, false, flags);
            scheduleFluidTick(world, pos, fluidState);
        } else {
            world.setBlockState(pos, newState, flags);
        }
    }

    private void setBlockStateKeepingFluid(World world, BlockPos pos, int flags) {
        FluidState fluidState = FluidloggedUtils.getFluidState(world, pos, world.getBlockState(pos));
        world.setBlockState(pos, fluidState.getFluid() == FluidRegistry.WATER ? fluidState.getState() : Blocks.AIR.getDefaultState(), flags);
        scheduleFluidTick(world, pos, fluidState);
    }

    private boolean hasWaterFluid(World world, BlockPos pos) {
        return getWaterFluidState(world, pos).getFluid() == FluidRegistry.WATER;
    }

    private FluidState getWaterFluidState(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() == Material.WATER ? FluidState.of(state) : FluidloggedUtils.getFluidState(world, pos, state);
    }

    private void scheduleFluidTick(World world, BlockPos pos, FluidState fluidState) {
        if (fluidState.getFluid() == FluidRegistry.WATER) {
            world.scheduleUpdate(pos, fluidState.getState().getBlock(), fluidState.getState().getBlock().tickRate(world));
        }
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state;
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing direction = (meta & 8) == 8 ? EnumFacing.UP : EnumFacing.DOWN;
        return getDefaultState().withProperty(VERTICAL_DIRECTION, direction).withProperty(THICKNESS, Thickness.byMetadata(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int direction = state.getValue(VERTICAL_DIRECTION) == EnumFacing.UP ? 8 : 0;
        return direction | state.getValue(THICKNESS).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {VERTICAL_DIRECTION, THICKNESS});
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    public enum Thickness implements IStringSerializable {
        TIP(0, "tip"),
        FRUSTUM(1, "frustum"),
        MIDDLE(2, "middle"),
        BASE(3, "base"),
        TIP_MERGE(4, "tip_merge");

        private static final Thickness[] META_LOOKUP = new Thickness[values().length];
        private final int metadata;
        private final String name;

        Thickness(int metadata, String name) {
            this.metadata = metadata;
            this.name = name;
        }

        public int getMetadata() {
            return metadata;
        }

        public String getName() {
            return name;
        }

        public static Thickness byMetadata(int metadata) {
            if (metadata < 0 || metadata >= META_LOOKUP.length) {
                metadata = 0;
            }

            return META_LOOKUP[metadata];
        }

        static {
            for (Thickness value : values()) {
                META_LOOKUP[value.getMetadata()] = value;
            }
        }
    }
}
