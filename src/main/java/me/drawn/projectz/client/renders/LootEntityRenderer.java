package me.drawn.projectz.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.entity.LootEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class LootEntityRenderer extends EntityRenderer<LootEntity> {

    private static final ItemStack DUMMY_NON_EMPTY_STACK = new ItemStack(Items.STONE);

    public LootEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(LootEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(@NotNull LootEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        final float scale = 1.0F;

        poseStack.translate(0.0, 0.5f, 0.0);
        poseStack.scale(scale, scale, scale);

        Item item = switch (entity.getModel().toLowerCase()) {
            case "weapon" -> ProjectZ.CRATE_WEAPON.get();
            case "crate", "small" -> ProjectZ.CRATE.get();
            case "car", "mechanics", "toolbox" -> ProjectZ.CRATE_CAR.get();
            case "large", "big", "large_crate" -> ProjectZ.CRATE_LARGE.get();
            case "medic" -> ProjectZ.CRATE_MEDIC.get();
            case "ammo" -> ProjectZ.CRATE_AMMO.get();
            default -> Items.CHEST.asItem();
        };

        ItemStack stack = new ItemStack(item);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        BakedModel model = itemRenderer.getModel(stack, Minecraft.getInstance().level, null, 0);
        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack, buffer, 15728880,
                OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
