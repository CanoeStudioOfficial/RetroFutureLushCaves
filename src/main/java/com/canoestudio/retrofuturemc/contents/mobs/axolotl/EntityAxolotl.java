package com.canoestudio.retrofuturemc.contents.mobs.axolotl;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EntityAxolotl extends EntityWaterMob {
    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityAxolotl.class, DataSerializers.VARINT);
    private static final int MAX_AIR = 6000;
    private static final String[] VARIANT_NAMES = new String[] {"lucy", "wild", "gold", "cyan", "blue"};

    private float randomMotionSpeed;
    private float randomMotionVecX;
    private float randomMotionVecY;
    private float randomMotionVecZ;

    public EntityAxolotl(World world) {
        super(world);
        setSize(0.75F, 0.42F);
        setAir(MAX_AIR);
        this.randomMotionSpeed = 0.08F;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(VARIANT, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.7D);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        setRandomVariant();
        return super.onInitialSpawn(difficulty, livingdata);
    }

    public void setRandomVariant() {
        if (rand.nextInt(1200) == 0) {
            setVariant(4);
        } else {
            setVariant(rand.nextInt(4));
        }
    }

    public int getVariant() {
        return dataManager.get(VARIANT);
    }

    public void setVariant(int variant) {
        dataManager.set(VARIANT, MathHelper.clamp(variant, 0, VARIANT_NAMES.length - 1));
    }

    public String getVariantName() {
        return VARIANT_NAMES[getVariant()];
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();

        if (isEntityAlive() && isInWater()) {
            setAir(MAX_AIR);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (isInWater()) {
            if (!hasMovementVector() || rand.nextInt(45) == 0) {
                float angle = rand.nextFloat() * ((float)Math.PI * 2F);
                randomMotionVecX = MathHelper.cos(angle) * 0.16F;
                randomMotionVecY = -0.04F + rand.nextFloat() * 0.11F;
                randomMotionVecZ = MathHelper.sin(angle) * 0.16F;
                randomMotionSpeed = 0.7F + rand.nextFloat() * 0.35F;
            }

            motionX = randomMotionVecX * randomMotionSpeed;
            motionY = randomMotionVecY * randomMotionSpeed;
            motionZ = randomMotionVecZ * randomMotionSpeed;

            float horizontal = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
            renderYawOffset += (-((float)MathHelper.atan2(motionX, motionZ)) * (180F / (float)Math.PI) - renderYawOffset) * 0.12F;
            rotationYaw = renderYawOffset;
            rotationPitch += (-((float)MathHelper.atan2(horizontal, motionY)) * (180F / (float)Math.PI) - rotationPitch) * 0.1F;
        } else {
            randomMotionVecX = 0.0F;
            randomMotionVecY = 0.0F;
            randomMotionVecZ = 0.0F;

            if (onGround && rand.nextInt(20) == 0) {
                motionX += (rand.nextDouble() - 0.5D) * 0.25D;
                motionY = 0.25D;
                motionZ += (rand.nextDouble() - 0.5D) * 0.25D;
                rotationYaw = rand.nextFloat() * 360.0F;
            }
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = new BlockPos(this);
        return world.getBlockState(pos).getMaterial().isLiquid() && pos.getY() < world.getSeaLevel() && super.getCanSpawnHere();
    }

    @Override
    public float getEyeHeight() {
        return height * 0.55F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isInWater() ? SoundEvents.ENTITY_SQUID_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SQUID_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    protected boolean canDespawn() {
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("Variant", getVariant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("Variant")) {
            setVariant(compound.getInteger("Variant"));
        }
    }

    private boolean hasMovementVector() {
        return randomMotionVecX != 0.0F || randomMotionVecY != 0.0F || randomMotionVecZ != 0.0F;
    }
}
