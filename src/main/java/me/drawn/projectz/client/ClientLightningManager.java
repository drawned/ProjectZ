package me.drawn.projectz.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.network.DistantLightningPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = ProjectZ.MODID, value = Dist.CLIENT)
public class ClientLightningManager {

    private static final List<VisualStrike> ACTIVE_STRIKES = new ArrayList<>();
    private static final List<PendingThunder> PENDING_SOUNDS = new ArrayList<>();

    private record VisualStrike(Vec3 basePos, long seed, int maxAge, int[] currentAge) {}
    private record PendingThunder(Vec3 strikePos, int[] delayTicks) {}

    public static void spawnDistantLightning(DistantLightningPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 strikePos = new Vec3(payload.x(), payload.y(), payload.z());
        ACTIVE_STRIKES.add(new VisualStrike(strikePos, payload.seed(), 6, new int[]{0}));

        // Velocidade do som (~17 blocos por tick)
        double distance = mc.player.position().distanceTo(strikePos);
        int delay = (int) Math.max(1, distance / 17.0);

        PENDING_SOUNDS.add(new PendingThunder(strikePos, new int[]{delay}));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Atualiza os raios e sincroniza o clarão próximo com shaders/vanilla
        ACTIVE_STRIKES.removeIf(s -> {
            s.currentAge[0]++;

            // Se o raio caiu perto (< 280 blocos), ativa a iluminação global nativa
            if (mc.player != null && mc.level != null) {
                double dist = mc.player.position().distanceTo(s.basePos);
                if (dist < 280.0) {
                    int age = s.currentAge[0];
                    // Pulso duplo nos frames de pico
                    if (age == 1 || age == 3) {
                        mc.level.setSkyFlashTime(2);
                    }
                }
            }

            return s.currentAge[0] >= s.maxAge;
        });

        // 2. Toca os trovões atrasados
        if (mc.player == null) return;

        Iterator<PendingThunder> it = PENDING_SOUNDS.iterator();
        while (it.hasNext()) {
            PendingThunder pt = it.next();
            pt.delayTicks[0]--;
            if (pt.delayTicks[0] <= 0) {
                playDirectionalThunder(mc, pt.strikePos);
                it.remove();
            }
        }
    }

    private static void playDirectionalThunder(Minecraft mc, Vec3 strikePos) {
        if (mc.player == null) return;

        double dx = strikePos.x - mc.player.getX();
        double dz = strikePos.z - mc.player.getZ();
        double angle = Math.atan2(dz, dx);

        double soundX = mc.player.getX() + Math.cos(angle) * 8.0;
        double soundZ = mc.player.getZ() + Math.sin(angle) * 8.0;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "far_thunder"));
        if (sound == null) return;

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.WEATHER,
                1.0F,
                0.85F + (float) (Math.random() * 0.25F),
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.LINEAR,
                soundX, mc.player.getY(), soundZ,
                false
        );

        mc.getSoundManager().play(soundInstance);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        if (ACTIVE_STRIKES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f mat = event.getPoseStack().last().pose();

        // Configuração OpenGL (Compatível com Shaders e Voxy)
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        // Blend aditivo para brilho de plasma incandescente
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (VisualStrike strike : ACTIVE_STRIKES) {
            float flashFactor = getFlashIntensity(strike.currentAge[0]);
            if (flashFactor <= 0.01F) continue;

            // 1. O Raio principal
            renderBoltSegments(buffer, mat, cam, strike.basePos, strike.seed, flashFactor);

            // 2. Clarão atmosférico nas nuvens (ilumina a cúpula do céu)
            renderCloudFlash(buffer, mat, cam, strike.basePos, flashFactor);

            // 3. Clarão de impacto no solo
            renderGroundFlash(buffer, mat, cam, strike.basePos, flashFactor);
        }

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        // Restaura estados
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * Curva física de piscada de raio (Double-Return Stroke)
     */
    private static float getFlashIntensity(int age) {
        return switch (age) {
            case 0 -> 0.65F; // Ataque inicial
            case 1 -> 1.00F; // Pico máximo de luminosidade
            case 2 -> 0.15F; // Queda rápida da primeira descarga
            case 3 -> 0.85F; // Segundo pulso forte (piscada)
            case 4 -> 0.35F; // Dissipação
            default -> 0.00F;
        };
    }

    private static void renderBoltSegments(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, long seed, float intensity) {
        Random rand = new Random(seed);

        double curX = basePos.x;
        double curZ = basePos.z;
        double curY = 460.0;
        double targetY = Math.max(30.0, basePos.y);

        int segments = 32;
        double stepY = (curY - targetY) / segments;

        for (int i = 0; i < segments; i++) {
            double nextY = curY - stepY;
            double nextX = curX + (rand.nextDouble() - 0.5) * 28.0;
            double nextZ = curZ + (rand.nextDouble() - 0.5) * 28.0;

            float x1 = (float) (curX - cam.x);
            float y1 = (float) (curY - cam.y);
            float z1 = (float) (curZ - cam.z);

            float x2 = (float) (nextX - cam.x);
            float y2 = (float) (nextY - cam.y);
            float z2 = (float) (nextZ - cam.z);

            float width = 2.4F;

            // Núcleo branco
            drawSegmentQuad(buffer, mat, x1, y1, z1, x2, y2, z2, width, 1.0F, 1.0F, 1.0F, 0.95F * intensity);
            // Halo azul-elétrico
            drawSegmentQuad(buffer, mat, x1, y1, z1, x2, y2, z2, width * 3.5F, 0.4F, 0.65F, 1.0F, 0.45F * intensity);

            curX = nextX;
            curY = nextY;
            curZ = nextZ;
        }
    }

    /**
     * Iluminação volumétrica gigante nas nuvens (raio de ~180 blocos no topo do céu)
     */
    private static void renderCloudFlash(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, float intensity) {
        float cx = (float) (basePos.x - cam.x);
        float cy = (float) (390.0 - cam.y);
        float cz = (float) (basePos.z - cam.z);

        float radius = 180.0F;
        float alpha = 0.28F * intensity;

        // Quad horizontal no topo da tempestade
        buffer.addVertex(mat, cx - radius, cy, cz - radius).setColor(0.55F, 0.75F, 1.0F, alpha);
        buffer.addVertex(mat, cx + radius, cy, cz - radius).setColor(0.55F, 0.75F, 1.0F, alpha);
        buffer.addVertex(mat, cx + radius, cy, cz + radius).setColor(0.55F, 0.75F, 1.0F, alpha);
        buffer.addVertex(mat, cx - radius, cy, cz + radius).setColor(0.55F, 0.75F, 1.0F, alpha);
    }

    /**
     * Clarão circular espalhado pelo chão ao redor do ponto de queda
     */
    private static void renderGroundFlash(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, float intensity) {
        float gx = (float) (basePos.x - cam.x);
        float gy = (float) (basePos.y + 1.2 - cam.y);
        float gz = (float) (basePos.z - cam.z);

        float groundRadius = 55.0F;
        float alpha = 0.35F * intensity;

        buffer.addVertex(mat, gx - groundRadius, gy, gz - groundRadius).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx + groundRadius, gy, gz - groundRadius).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx + groundRadius, gy, gz + groundRadius).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx - groundRadius, gy, gz + groundRadius).setColor(0.7F, 0.85F, 1.0F, alpha);
    }

    private static void drawSegmentQuad(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        buffer.addVertex(mat, x1 - width, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(mat, x1 + width, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 + width, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 - width, y2, z2).setColor(r, g, b, a);
    }
}