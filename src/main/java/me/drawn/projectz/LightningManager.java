package me.drawn.projectz;

import me.drawn.projectz.network.DistantLightningPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ProjectZ.MODID)
public class LightningManager {

    // Configurações gerais
    public static int CHECK_INTERVAL = 10;      // Checa a cada 10 ticks (0.5 segundo)
    public static double MIN_DISTANCE = 120;  // Distância mínima dos raios distantes
    public static double MAX_DISTANCE = 2200.0;  // Distância máxima (visível nos LODs do Voxy)

    // Chances de queda
    public static int RAIN_CHANCE = 45;         // 45% de chance durante chuva comum (bastante constante)
    public static int THUNDER_CHANCE = 75;      // 85% de chance durante tempestade (quase ininterrupto)

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Roda a cada meio segundo
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;
        if (level.players().isEmpty()) return;

        boolean isThundering = level.isThundering();
        boolean isRaining = level.isRaining();

        // Se o tempo estiver limpo, não faz nada
        if (!isRaining && !isThundering) return;

        // Define a probabilidade e a agressividade com base no clima atual
        int currentChance = isThundering ? THUNDER_CHANCE : RAIN_CHANCE;

        if (level.random.nextInt(100) < currentChance) {
            ServerPlayer anchor = level.players().get(level.random.nextInt(level.players().size()));

            // 1. Primeiro raio principal
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + (level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE));
            sendStrike(level, anchor, angle, distance);

            // 2. Raios adicionais (Multistrikes)
            if (isThundering) {
                // Em tempestade: 50% de chance de um 2º raio cair junto
                if (level.random.nextInt(100) < 50) {
                    double angle2 = angle + (level.random.nextDouble() - 0.5) * 1.8;
                    double dist2 = MIN_DISTANCE + (level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE));
                    sendStrike(level, anchor, angle2, dist2);
                }

                // Em tempestade: 20% de chance de um 3º raio rasgar outro ponto do céu
                if (level.random.nextInt(100) < 20) {
                    double angle3 = level.random.nextDouble() * Math.PI * 2;
                    double dist3 = MIN_DISTANCE + (level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE));
                    sendStrike(level, anchor, angle3, dist3);
                }
            } else {
                // Em chuva comum: apenas 15% de chance de raio duplo ocasional
                if (level.random.nextInt(100) < 15) {
                    double angle2 = angle + (level.random.nextDouble() - 0.5) * 1.2;
                    double dist2 = distance * (0.85 + level.random.nextDouble() * 0.3);
                    sendStrike(level, anchor, angle2, dist2);
                }
            }
        }
    }

    private static void sendStrike(ServerLevel level, ServerPlayer anchor, double angle, double distance) {
        double strikeX = anchor.getX() + Math.cos(angle) * distance;
        double strikeZ = anchor.getZ() + Math.sin(angle) * distance;
        double strikeY = 64.0; // Altura base do terreno

        long seed = level.random.nextLong();

        PacketDistributor.sendToPlayersInDimension(
                level,
                new DistantLightningPayload(strikeX, strikeY, strikeZ, seed)
        );
    }
}