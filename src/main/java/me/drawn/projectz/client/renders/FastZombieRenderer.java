package me.drawn.projectz.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drawn.projectz.client.RealisticZombieModel;
import me.drawn.projectz.client.textures.ZombieTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import org.jetbrains.annotations.NotNull;

public class FastZombieRenderer extends MobRenderer<Zombie, ZombieModel<Zombie>> {

    @Override
    public @NotNull ResourceLocation getTextureLocation(Zombie zombie) {
        long uuidBits = zombie.getUUID().getLeastSignificantBits();
        int textureIndex = Math.floorMod(uuidBits, ZombieTextures.TEXTURES.length);

        return ZombieTextures.TEXTURES[textureIndex];
    }

    public FastZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new RealisticZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.0F);
        // removing all layers for max optimization, maybe could be removed later, who knows
        this.layers.clear();
    }

    @Override
    public void render(Zombie entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double distSqr = entity.distanceToSqr(mc.player);

        // turn off total at >64 blocks
        if (distSqr > 4096.0) return;

        // turn off animation at >16 blocks
        if (distSqr > 256.0) {
            entity.walkAnimation.setSpeed(0.0F);
            entity.walkAnimation.position(0.0F);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}