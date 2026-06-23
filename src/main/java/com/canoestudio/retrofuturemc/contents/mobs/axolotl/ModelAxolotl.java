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

        boolean inWater = entityIn.isInWater();
        float swim = MathHelper.sin(ageInTicks * (inWater ? 0.33F : 0.12F));
        float swimCos = MathHelper.cos(ageInTicks * (inWater ? 0.30F : 0.12F));

        body.rotateAngleX = inWater ? headPitch * 0.017453292F + swim * 0.13F : 0.08F;
        body.rotateAngleY = netHeadYaw * 0.017453292F * 0.25F;
        head.rotateAngleX = -swim * 0.16F;
        tail.rotateAngleY = swimCos * (inWater ? 0.45F : 0.12F);

        topGills.rotateAngleX = inWater ? -0.65F - swim * 0.25F : 0.45F;
        leftGills.rotateAngleY = inWater ? 0.75F + swim * 0.15F : -0.45F;
        rightGills.rotateAngleY = -leftGills.rotateAngleY;

        float legPose = inWater ? 1.8849558F : 0.95F;
        leftFrontLeg.rotateAngleX = legPose;
        rightFrontLeg.rotateAngleX = legPose;
        leftHindLeg.rotateAngleX = legPose;
        rightHindLeg.rotateAngleX = legPose;

        leftFrontLeg.rotateAngleY = 1.7F + swim * 0.2F;
        rightFrontLeg.rotateAngleY = -leftFrontLeg.rotateAngleY;
        leftHindLeg.rotateAngleY = 0.8F - swim * 0.2F;
        rightHindLeg.rotateAngleY = -leftHindLeg.rotateAngleY;
    }

    private void resetRotations() {
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
