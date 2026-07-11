package me.drawn.projectz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.network.AirdropVisualPayload;
import me.drawn.projectz.registry.ModSoundEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientPlaneManager {
    private static final List<PlaneVisual> PLANES = new ArrayList<>();

    public static class PlaneVisual {
        public final double tx, ty, tz;
        public final double sx, sy, sz;
        public final double ex, ey, ez;
        public final float size;
        public final String lootId;
        public final int totalTicks;
        public int ticksExisted = 0;

        public double x, y, z;
        public float yaw;

        public PlaneVisual(AirdropVisualPayload payload) {
            this.tx = payload.tx(); this.ty = payload.ty(); this.tz = payload.tz();
            this.sx = payload.sx(); this.sy = payload.sy(); this.sz = payload.sz();
            this.ex = payload.ex(); this.ey = payload.ey(); this.ez = payload.ez();
            this.size = payload.size();
            this.lootId = payload.lootId();
            this.totalTicks = payload.duration();
            this.x = sx;
            this.y = sy;
            this.z = sz;

            double dx = ex - sx;
            double dz = ez - sz;
            this.yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) + 90.0f;
        }

        public void tick() {
            ticksExisted++;
            double progress = (double) ticksExisted / totalTicks;
            x = sx + (ex - sx) * progress;
            y = sy + (ey - sy) * progress;
            z = sz + (ez - sz) * progress;

            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && ticksExisted % 40 == 0) {
                var sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "airplane"));
                if (sound != null) {
                    level.playLocalSound(x, y, z, sound, SoundSource.AMBIENT, 160F, 1.0F, false);
                }
            }
        }

        public boolean isFinished() {
            return ticksExisted >= totalTicks;
        }
    }

    public static void addPlane(AirdropVisualPayload payload) {
        PLANES.add(new PlaneVisual(payload));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused() || Minecraft.getInstance().level == null) return;
        Iterator<PlaneVisual> iterator = PLANES.iterator();
        while (iterator.hasNext()) {
            PlaneVisual plane = iterator.next();
            plane.tick();
            if (plane.isFinished()) {
                iterator.remove();
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (PLANES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (PlaneVisual plane : PLANES) {
            poseStack.pushPose();

            double renderX = plane.x - camX;
            double renderY = plane.y - camY;
            double renderZ = plane.z - camZ;
            poseStack.translate(renderX, renderY, renderZ);

            poseStack.mulPose(Axis.YP.rotationDegrees(-plane.yaw));
            poseStack.scale(plane.size, plane.size, plane.size);

            ItemStack stack = new ItemStack(ProjectZ.AIRPLANE_ITEM.get());
            ItemRenderer itemRenderer = mc.getItemRenderer();

            BakedModel model = itemRenderer.getModel(stack, mc.level, null, 0);
            itemRenderer.render(stack, net.minecraft.world.item.ItemDisplayContext.NONE, false, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, model);

            poseStack.popPose();
        }
    }
}