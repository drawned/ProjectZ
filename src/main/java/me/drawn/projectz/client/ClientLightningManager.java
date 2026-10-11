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

        ACTIVE_STRIKES.removeIf(s -> {
            s.currentAge[0]++;

            // Ilumina o ambiente e as nuvens volumétricas dos shaders no pico da piscada
            if (mc.player != null && mc.level != null) {
                int age = s.currentAge[0];
                if (age == 1 || age == 3) {
                    double dist = mc.player.position().distanceTo(s.basePos);
                    // Faz as nuvens do shader e o horizonte piscarem mesmo a distâncias longas
                    if (dist < 850.0) {
                        mc.level.setSkyFlashTime(2);
                    }
                }
            }

            return s.currentAge[0] >= s.maxAge;
        });

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

        // Estados OpenGL
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull(); // CORREÇÃO: Impede que o raio suma dependendo da rotação da câmera
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (VisualStrike strike : ACTIVE_STRIKES) {
            float flashFactor = getFlashIntensity(strike.currentAge[0]);
            if (flashFactor <= 0.01F) continue;

            // 1. Tronco vertical do raio com Billboarding 3D
            renderBoltSegments(buffer, mat, cam, strike.basePos, strike.seed, flashFactor);

            // 2. Galhos no topo que se espalham pelas nuvens
            renderCloudBranches(buffer, mat, cam, strike.basePos, strike.seed, flashFactor);

            // 3. Clarão radial suave no céu (sem cantos quadrados)
            renderSmoothCloudGlow(buffer, mat, cam, strike.basePos, flashFactor);

            // 4. Clarão de impacto no chão
            renderSmoothGroundGlow(buffer, mat, cam, strike.basePos, flashFactor);
        }

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        // Restaura estados
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float getFlashIntensity(int age) {
        return switch (age) {
            case 0 -> 0.70F;
            case 1 -> 1.00F;
            case 2 -> 0.15F;
            case 3 -> 0.85F;
            case 4 -> 0.30F;
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
            double nextX = curX + (rand.nextDouble() - 0.5) * 26.0;
            double nextZ = curZ + (rand.nextDouble() - 0.5) * 26.0;

            float x1 = (float) (curX - cam.x);
            float y1 = (float) (curY - cam.y);
            float z1 = (float) (curZ - cam.z);

            float x2 = (float) (nextX - cam.x);
            float y2 = (float) (nextY - cam.y);
            float z2 = (float) (nextZ - cam.z);

            float width = 2.4F;

            // Núcleo branco puro
            drawBillboardSegment(buffer, mat, x1, y1, z1, x2, y2, z2, width, 1.0F, 1.0F, 1.0F, 0.95F * intensity);
            // Halo azul elétrico
            drawBillboardSegment(buffer, mat, x1, y1, z1, x2, y2, z2, width * 3.5F, 0.45F, 0.7F, 1.0F, 0.40F * intensity);

            curX = nextX;
            curY = nextY;
            curZ = nextZ;
        }
    }

    /**
     * Gera ramos horizontais no topo que rasgam as nuvens (Anvil Crawlers)
     */
    private static void renderCloudBranches(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, long seed, float intensity) {
        Random rand = new Random(seed + 9999);

        int branchCount = 3 + rand.nextInt(3); // 3 a 5 ramos principais
        for (int b = 0; b < branchCount; b++) {
            double curX = basePos.x;
            double curY = 450.0 + (rand.nextDouble() - 0.5) * 15.0;
            double curZ = basePos.z;

            double angle = rand.nextDouble() * Math.PI * 2;
            int branchSegments = 6 + rand.nextInt(5);

            for (int i = 0; i < branchSegments; i++) {
                double nextX = curX + Math.cos(angle) * 16.0 + (rand.nextDouble() - 0.5) * 10.0;
                double nextY = curY + (rand.nextDouble() - 0.5) * 6.0;
                double nextZ = curZ + Math.sin(angle) * 16.0 + (rand.nextDouble() - 0.5) * 10.0;

                float x1 = (float) (curX - cam.x);
                float y1 = (float) (curY - cam.y);
                float z1 = (float) (curZ - cam.z);

                float x2 = (float) (nextX - cam.x);
                float y2 = (float) (nextY - cam.y);
                float z2 = (float) (nextZ - cam.z);

                float width = 1.2F;

                drawBillboardSegment(buffer, mat, x1, y1, z1, x2, y2, z2, width, 1.0F, 1.0F, 1.0F, 0.85F * intensity);
                drawBillboardSegment(buffer, mat, x1, y1, z1, x2, y2, z2, width * 2.8F, 0.4F, 0.65F, 1.0F, 0.35F * intensity);

                curX = nextX;
                curY = nextY;
                curZ = nextZ;
            }
        }
    }

    /**
     * Clarão com fade radial suave (elimina cantos ou quadrados visíveis)
     */
    private static void renderSmoothCloudGlow(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, float intensity) {
        float cx = (float) (basePos.x - cam.x);
        float cy = (float) (450.0 - cam.y);
        float cz = (float) (basePos.z - cam.z);

        float r = 260.0F; // Raio de iluminação suave
        float alphaCenter = 0.32F * intensity;

        // Malha de 4 quads com centro brilhante e todas as bordas com ALFA ZERO
        // Quadrante 1 (Noroeste)
        buffer.addVertex(mat, cx, cy, cz).setColor(0.6F, 0.8F, 1.0F, alphaCenter);
        buffer.addVertex(mat, cx - r, cy, cz).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx - r, cy, cz - r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx, cy, cz - r).setColor(0.6F, 0.8F, 1.0F, 0.0F);

        // Quadrante 2 (Nordeste)
        buffer.addVertex(mat, cx, cy, cz).setColor(0.6F, 0.8F, 1.0F, alphaCenter);
        buffer.addVertex(mat, cx, cy, cz - r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx + r, cy, cz - r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx + r, cy, cz).setColor(0.6F, 0.8F, 1.0F, 0.0F);

        // Quadrante 3 (Sudeste)
        buffer.addVertex(mat, cx, cy, cz).setColor(0.6F, 0.8F, 1.0F, alphaCenter);
        buffer.addVertex(mat, cx + r, cy, cz).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx + r, cy, cz + r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx, cy, cz + r).setColor(0.6F, 0.8F, 1.0F, 0.0F);

        // Quadrante 4 (Sudoeste)
        buffer.addVertex(mat, cx, cy, cz).setColor(0.6F, 0.8F, 1.0F, alphaCenter);
        buffer.addVertex(mat, cx, cy, cz + r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx - r, cy, cz + r).setColor(0.6F, 0.8F, 1.0F, 0.0F);
        buffer.addVertex(mat, cx - r, cy, cz).setColor(0.6F, 0.8F, 1.0F, 0.0F);
    }

    private static void renderSmoothGroundGlow(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 basePos, float intensity) {
        float gx = (float) (basePos.x - cam.x);
        float gy = (float) (basePos.y + 1.2 - cam.y);
        float gz = (float) (basePos.z - cam.z);

        float r = 65.0F;
        float alpha = 0.38F * intensity;

        // Malha radial no chão com bordas com alfa zero
        buffer.addVertex(mat, gx, gy, gz).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx - r, gy, gz).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx - r, gy, gz - r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx, gy, gz - r).setColor(0.7F, 0.85F, 1.0F, 0.0F);

        buffer.addVertex(mat, gx, gy, gz).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx, gy, gz - r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx + r, gy, gz - r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx + r, gy, gz).setColor(0.7F, 0.85F, 1.0F, 0.0F);

        buffer.addVertex(mat, gx, gy, gz).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx + r, gy, gz).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx + r, gy, gz + r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx, gy, gz + r).setColor(0.7F, 0.85F, 1.0F, 0.0F);

        buffer.addVertex(mat, gx, gy, gz).setColor(0.7F, 0.85F, 1.0F, alpha);
        buffer.addVertex(mat, gx, gy, gz + r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx - r, gy, gz + r).setColor(0.7F, 0.85F, 1.0F, 0.0F);
        buffer.addVertex(mat, gx - r, gy, gz).setColor(0.7F, 0.85F, 1.0F, 0.0F);
    }

    /**
     * Billboarding 3D por Produto Vetorial:
     * Calcula o vetor perpendicular à linha de visão da câmera, garantindo que o raio
     * tenha espessura perfeita e NUNCA fique invisível ou em "fio de navalha", de qualquer ângulo.
     */
    private static void drawBillboardSegment(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;

        // Ponto central do segmento
        float midX = (x1 + x2) * 0.5F;
        float midY = (y1 + y2) * 0.5F;
        float midZ = (z1 + z2) * 0.5F;

        // Produto vetorial entre a direção do raio e a linha de visão do jogador
        float nx = dy * midZ - dz * midY;
        float ny = dz * midX - dx * midZ;
        float nz = dx * midY - dy * midX;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 0.001F) {
            nx = 1.0F; ny = 0.0F; nz = 0.0F;
        } else {
            nx /= len; ny /= len; nz /= len;
        }

        float offX = nx * width;
        float offY = ny * width;
        float offZ = nz * width;

        buffer.addVertex(mat, x1 - offX, y1 - offY, z1 - offZ).setColor(r, g, b, a);
        buffer.addVertex(mat, x1 + offX, y1 + offY, z1 + offZ).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 + offX, y2 + offY, z2 + offZ).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 - offX, y2 - offY, z2 - offZ).setColor(r, g, b, a);
    }
}