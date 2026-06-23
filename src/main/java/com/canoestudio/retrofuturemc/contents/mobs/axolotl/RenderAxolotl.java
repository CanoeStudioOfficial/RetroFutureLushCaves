package com.canoestudio.retrofuturemc.contents.mobs.axolotl;

import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderAxolotl extends RenderLiving<EntityAxolotl> {
    public RenderAxolotl(RenderManager renderManager) {
        super(renderManager, new ModelAxolotl(), 0.35F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAxolotl entity) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/axolotl/axolotl_" + entity.getVariantName() + ".png");
    }

    @Override
    protected void preRenderCallback(EntityAxolotl entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(0.9F, 0.9F, 0.9F);
    }
}
