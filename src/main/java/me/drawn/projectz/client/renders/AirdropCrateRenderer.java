package me.drawn.projectz.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.entity.AirdropCrateEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AirdropCrateRenderer extends EntityRenderer<AirdropCrateEntity> {
    public AirdropCrateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AirdropCrateEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(AirdropCrateEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        final float scale = 1.0F;

        poseStack.translate(0.0, 0.5f, 0.0);
        poseStack.scale(scale, scale, scale);

        ItemStack stack = new ItemStack(ProjectZ.AIRDROP_ITEM.get());
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        BakedModel model = itemRenderer.getModel(stack, Minecraft.getInstance().level, null, 0);
        itemRenderer.render(stack, net.minecraft.world.item.ItemDisplayContext.NONE, false, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}


