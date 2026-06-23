package com.canoestudio.retrofuturemc.contents.mobs.axolotl;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class ModelAxolotl extends ModelBase {
    private final ModelRenderer body;
    private final ModelRenderer head;
    private final ModelRenderer topGills;
    private final ModelRenderer leftGills;
    private final ModelRenderer rightGills;
    private final ModelRenderer tail;
    private final ModelRenderer leftHindLeg;
    private final ModelRenderer rightHindLeg;
    private final ModelRenderer leftFrontLeg;
    private final ModelRenderer rightFrontLeg;

    public ModelAxolotl() {
        textureWidth = 64;
        textureHeight = 64;

        body = new ModelRenderer(this, 0, 11);
        body.setRotationPoint(0.0F, 19.5F, 5.0F);
        body.addBox(-4.0F, -2.0F, -9.0F, 8, 4, 10);
        body.setTextureOffset(2, 17).addBox(0.0F, -3.0F, -8.0F, 0, 5, 9);

        head = new ModelRenderer(this, 0, 1);
        head.setRotationPoint(0.0F, 0.0F, -9.0F);
        head.addBox(-4.0F, -3.0F, -5.0F, 8, 5, 5);
        body.addChild(head);

        topGills = new ModelRenderer(this, 3, 37);
        topGills.setRotationPoint(0.0F, -3.0F, -1.0F);
        topGills.addBox(-4.0F, -3.0F, 0.0F, 8, 3, 0);
        head.addChild(topGills);

        leftGills = new ModelRenderer(this, 0, 40);
        leftGills.setRotationPoint(-4.0F, 0.0F, -1.0F);
        leftGills.addBox(-3.0F, -5.0F, 0.0F, 3, 7, 0);
        head.addChild(leftGills);

        rightGills = new ModelRenderer(this, 11, 40);
        rightGills.setRotationPoint(4.0F, 0.0F, -1.0F);
        rightGills.addBox(0.0F, -5.0F, 0.0F, 3, 7, 0);
        head.addChild(rightGills);

        tail = new ModelRenderer(this, 2, 19);
        tail.setRotationPoint(0.0F, 0.0F, 1.0F);
        tail.addBox(0.0F, -3.0F, 0.0F, 0, 5, 12);
        body.addChild(tail);

        leftHindLeg = createLeftLeg(-1.0F);
        rightHindLeg = createRightLeg(-1.0F);
        leftFrontLeg = createLeftLeg(-8.0F);
        rightFrontLeg = createRightLeg(-8.0F);
    }

    private ModelRenderer createLeftLeg(float z) {
        ModelRenderer leg = new ModelRenderer(this, 2, 13);
        leg.setRotationPoint(3.5F, 1.0F, z);
        leg.addBox(-1.0F, 0.0F, 0.0F, 3, 5, 0);
        body.addChild(leg);
        return leg;
    }

    private ModelRenderer createRightLeg(float z) {
        ModelRenderer leg = new ModelRenderer(this, 2, 13);
        leg.setRotationPoint(-3.5F, 1.0F, z);
        leg.addBox(-2.0F, 0.0F, 0.0F, 3, 5, 0);
        body.addChild(leg);
        return leg;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        body.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        resetRotations();

        EntityAxolotl axolotl = entityIn instanceof EntityAxolotl ? (EntityAxolotl)entityIn : null;
        float partialTicks = MathHelper.clamp(ageInTicks - (float)entityIn.ticksExisted, 0.0F, 1.0F);
        float playingDeadFactor;
        float inWaterFactor;
        float onGroundFactor;
        float movingFactor;

        if (axolotl != null) {
            playingDeadFactor = axolotl.getPlayingDeadAnimationFactor(partialTicks);
            inWaterFactor = axolotl.getInWaterAnimationFactor(partialTicks);
            onGroundFactor = axolotl.getOnGroundAnimationFactor(partialTicks);
            movingFactor = Math.max(axolotl.getMovingAnimationFactor(partialTicks), MathHelper.clamp(limbSwingAmount, 0.0F, 1.0F));
        } else {
            playingDeadFactor = 0.0F;
            inWaterFactor = entityIn.isInWater() ? 1.0F : 0.0F;
            onGroundFactor = !entityIn.isInWater() && entityIn.onGround ? 1.0F : 0.0F;
            double horizontalMotion = entityIn.motionX * entityIn.motionX + entityIn.motionZ * entityIn.motionZ;
            movingFactor = horizontalMotion > 7.5E-5D || limbSwingAmount > 0.01F ? 1.0F : 0.0F;
        }

        float notMovingFactor = MathHelper.clamp(1.0F - movingFactor, 0.0F, 1.0F);
        float mirroredLegsFactor = 1.0F - Math.min(onGroundFactor, movingFactor);

        body.rotateAngleY += netHeadYaw * 0.017453292F * 0.35F;
        setupSwimmingAnimation(ageInTicks, headPitch, Math.min(movingFactor, inWaterFactor));
        setupWaterHoveringAnimation(ageInTicks, Math.min(notMovingFactor, inWaterFactor));
        setupGroundCrawlingAnimation(ageInTicks, Math.min(movingFactor, onGroundFactor));
        setupLayStillOnGroundAnimation(ageInTicks, Math.min(notMovingFactor, onGroundFactor));
        setupPlayDeadAnimation(playingDeadFactor);
        applyMirrorLegRotations(mirroredLegsFactor);
    }

    private void setupLayStillOnGroundAnimation(float ageInTicks, float factor) {
        if (factor > 1.0E-5F) {
            float animMoveSpeed = ageInTicks * 0.09F;
            float sineSway = MathHelper.sin(animMoveSpeed);
            float cosineSway = MathHelper.cos(animMoveSpeed);
            float movement = sineSway * sineSway - 2.0F * sineSway;
            float movement2 = cosineSway * cosineSway - 3.0F * sineSway;

            head.rotateAngleX += -0.09F * movement * factor;
            head.rotateAngleZ += -0.2F * factor;
            tail.rotateAngleY += (-0.1F + 0.1F * movement) * factor;
            float gillAngle = (0.6F + 0.05F * movement2) * factor;

            topGills.rotateAngleX += gillAngle;
            leftGills.rotateAngleY -= gillAngle;
            rightGills.rotateAngleY += gillAngle;
            leftHindLeg.rotateAngleX += 1.1F * factor;
            leftHindLeg.rotateAngleY += 1.0F * factor;
            leftFrontLeg.rotateAngleX += 0.8F * factor;
            leftFrontLeg.rotateAngleY += 2.3F * factor;
            leftFrontLeg.rotateAngleZ -= 0.5F * factor;
        }
    }

    private void setupGroundCrawlingAnimation(float ageInTicks, float factor) {
        if (factor > 1.0E-5F) {
            float animMoveSpeed = ageInTicks * 0.11F;
            float cosineSway = MathHelper.cos(animMoveSpeed);
            float hindLegYRotSway = (cosineSway * cosineSway - 2.0F * cosineSway) / 5.0F;
            float frontLegYRotSway = 0.7F * cosineSway;
            float headAndTailYRot = 0.09F * cosineSway * factor;

            head.rotateAngleY += headAndTailYRot;
            tail.rotateAngleY += headAndTailYRot;
            float gillAngle = (0.6F - 0.08F * (cosineSway * cosineSway + 2.0F * MathHelper.sin(animMoveSpeed))) * factor;

            topGills.rotateAngleX += gillAngle;
            leftGills.rotateAngleY -= gillAngle;
            rightGills.rotateAngleY += gillAngle;
            float hindLegXRot = 0.9424779F * factor;
            float frontLegXRot = 1.0995574F * factor;

            leftHindLeg.rotateAngleX += hindLegXRot;
            leftHindLeg.rotateAngleY += (1.5F - hindLegYRotSway) * factor;
            leftHindLeg.rotateAngleZ += -0.1F * factor;
            leftFrontLeg.rotateAngleX += frontLegXRot;
            leftFrontLeg.rotateAngleY += (1.5707964F - frontLegYRotSway) * factor;
            rightHindLeg.rotateAngleX += hindLegXRot;
            rightHindLeg.rotateAngleY += (-1.0F - hindLegYRotSway) * factor;
            rightFrontLeg.rotateAngleX += frontLegXRot;
            rightFrontLeg.rotateAngleY += (-1.5707964F - frontLegYRotSway) * factor;
        }
    }

    private void setupWaterHoveringAnimation(float ageInTicks, float factor) {
        if (factor > 1.0E-5F) {
            float animMoveSpeed = ageInTicks * 0.075F;
            float cosineSway = MathHelper.cos(animMoveSpeed);
            float sineSway = MathHelper.sin(animMoveSpeed) * 0.15F;
            float bodyXRot = (-0.15F + 0.075F * cosineSway) * factor;

            body.rotateAngleX += bodyXRot;
            body.rotationPointY -= sineSway * factor;
            head.rotateAngleX -= bodyXRot;
            topGills.rotateAngleX += 0.2F * cosineSway * factor;
            float gillYRot = (-0.3F * cosineSway - 0.19F) * factor;

            leftGills.rotateAngleY += gillYRot;
            rightGills.rotateAngleY -= gillYRot;
            leftHindLeg.rotateAngleX += (2.3561945F - cosineSway * 0.11F) * factor;
            leftHindLeg.rotateAngleY += 0.47123894F * factor;
            leftHindLeg.rotateAngleZ += 1.7278761F * factor;
            leftFrontLeg.rotateAngleX += (0.7853982F - cosineSway * 0.2F) * factor;
            leftFrontLeg.rotateAngleY += 2.042035F * factor;
            tail.rotateAngleY += 0.5F * cosineSway * factor;
        }
    }

    private void setupSwimmingAnimation(float ageInTicks, float xRot, float factor) {
        if (factor > 1.0E-5F) {
            float animMoveSpeed = ageInTicks * 0.33F;
            float sineSway = MathHelper.sin(animMoveSpeed);
            float cosineSway = MathHelper.cos(animMoveSpeed);
            float bodySway = 0.13F * sineSway;

            body.rotateAngleX += (xRot * 0.017453292F + bodySway) * factor;
            head.rotateAngleX -= bodySway * 1.8F * factor;
            body.rotationPointY -= 0.45F * cosineSway * factor;
            topGills.rotateAngleX += (-0.5F * sineSway - 0.8F) * factor;
            float gillYRot = (0.3F * sineSway + 0.9F) * factor;

            leftGills.rotateAngleY += gillYRot;
            rightGills.rotateAngleY -= gillYRot;
            tail.rotateAngleY += 0.3F * MathHelper.cos(animMoveSpeed * 0.9F) * factor;
            leftHindLeg.rotateAngleX += 1.8849558F * factor;
            leftHindLeg.rotateAngleY += -0.4F * sineSway * factor;
            leftHindLeg.rotateAngleZ += 1.5707964F * factor;
            leftFrontLeg.rotateAngleX += 1.8849558F * factor;
            leftFrontLeg.rotateAngleY += (-0.2F * cosineSway - 0.1F) * factor;
            leftFrontLeg.rotateAngleZ += 1.5707964F * factor;
        }
    }

    private void setupPlayDeadAnimation(float factor) {
        if (factor > 1.0E-5F) {
            leftHindLeg.rotateAngleX += 1.4137167F * factor;
            leftHindLeg.rotateAngleY += 1.0995574F * factor;
            leftHindLeg.rotateAngleZ += 0.7853982F * factor;
            leftFrontLeg.rotateAngleX += 0.7853982F * factor;
            leftFrontLeg.rotateAngleY += 2.042035F * factor;
            body.rotateAngleX += -0.15F * factor;
            body.rotateAngleZ += 0.35F * factor;
        }
    }

    private void applyMirrorLegRotations(float factor) {
        if (factor > 1.0E-5F) {
            rightHindLeg.rotateAngleX += leftHindLeg.rotateAngleX * factor;
            rightHindLeg.rotateAngleY += -leftHindLeg.rotateAngleY * factor;
            rightHindLeg.rotateAngleZ += -leftHindLeg.rotateAngleZ * factor;
            rightFrontLeg.rotateAngleX += leftFrontLeg.rotateAngleX * factor;
            rightFrontLeg.rotateAngleY += -leftFrontLeg.rotateAngleY * factor;
            rightFrontLeg.rotateAngleZ += -leftFrontLeg.rotateAngleZ * factor;
        }
    }

    private void resetRotations() {
        body.rotationPointY = 19.5F;
        body.rotateAngleX = 0.0F;
        body.rotateAngleY = 0.0F;
        body.rotateAngleZ = 0.0F;
        head.rotateAngleX = 0.0F;
        head.rotateAngleY = 0.0F;
        head.rotateAngleZ = 0.0F;
        topGills.rotateAngleX = 0.0F;
        topGills.rotateAngleY = 0.0F;
        topGills.rotateAngleZ = 0.0F;
        leftGills.rotateAngleX = 0.0F;
        leftGills.rotateAngleY = 0.0F;
        leftGills.rotateAngleZ = 0.0F;
        rightGills.rotateAngleX = 0.0F;
        rightGills.rotateAngleY = 0.0F;
        rightGills.rotateAngleZ = 0.0F;
        tail.rotateAngleX = 0.0F;
        tail.rotateAngleY = 0.0F;
        tail.rotateAngleZ = 0.0F;
        leftHindLeg.rotateAngleX = 0.0F;
        leftHindLeg.rotateAngleY = 0.0F;
        leftHindLeg.rotateAngleZ = 0.0F;
        rightHindLeg.rotateAngleX = 0.0F;
        rightHindLeg.rotateAngleY = 0.0F;
        rightHindLeg.rotateAngleZ = 0.0F;
        leftFrontLeg.rotateAngleX = 0.0F;
        leftFrontLeg.rotateAngleY = 0.0F;
        leftFrontLeg.rotateAngleZ = 0.0F;
        rightFrontLeg.rotateAngleX = 0.0F;
        rightFrontLeg.rotateAngleY = 0.0F;
        rightFrontLeg.rotateAngleZ = 0.0F;
    }
}
