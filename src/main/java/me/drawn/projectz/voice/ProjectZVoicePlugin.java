package me.drawn.projectz.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import me.drawn.projectz.HordeManager;
import me.drawn.projectz.ProjectZ;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class ProjectZVoicePlugin implements VoicechatPlugin {

    private VoicechatServerApi serverApi;

    // Cooldown para evitar disparar 50 vezes por segundo por jogador
    private final Map<UUID, Long> lastVoiceAlertTime = new ConcurrentHashMap<>();
    private final Map<UUID, OpusDecoder> playerDecoders = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return ProjectZ.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi server) {
            this.serverApi = server;
            ProjectZ.LOGGER.info("[ProjectZ] Conectado à API do Simple Voice Chat!");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (serverApi == null || event.getSenderConnection() == null) return;

        de.maxhenkel.voicechat.api.ServerPlayer vcPlayer = event.getSenderConnection().getPlayer();
        if (vcPlayer == null) return;

        Object rawPlayer = vcPlayer.getPlayer();
        if (!(rawPlayer instanceof ServerPlayer player)) return;

        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return;

        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();

        // Limita a verificação sonora a cada 500ms (2 vezes por segundo)
        Long lastAlert = lastVoiceAlertTime.get(uuid);
        if (lastAlert != null && (now - lastAlert) < 500) {
            return;
        }

        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData == null || opusData.length == 0) return;

        // Decodifica o áudio Opus para PCM 16-bit
        OpusDecoder decoder = playerDecoders.computeIfAbsent(uuid, id -> serverApi.createDecoder());
        short[] pcmSamples = decoder.decode(opusData);
        if (pcmSamples == null || pcmSamples.length == 0) return;

        // Cálculo da intensidade do som (RMS)
        long sum = 0;
        for (short sample : pcmSamples) {
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / pcmSamples.length);

        // 1. Silêncio / Ruído de fundo (ignora)
        if (rms < 500.0) {
            return;
        }

        // 2. Determinação do raio de ruído conforme o volume da voz
        double radius;
        if (rms < 1800.0) {
            // Sussurrando (apenas zumbis muito perto ouvem)
            radius = 4.0;
        } else if (rms < 5000.0) {
            // Conversa normal
            radius = 14.0;
        } else {
            // Gritando / Voz alta no microfone
            radius = 28.0;
        }

        // Jogador agachado tentando cochichar abafa a voz
        if (player.isShiftKeyDown()) {
            radius *= 0.6;
        }

        lastVoiceAlertTime.put(uuid, now);

        final double finalRadius = radius;
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        // Dispara para a thread principal do Minecraft com segurança
        player.server.execute(() -> {
            HordeManager.broadcastNoise(level, pos, finalRadius, finalRadius >= 16.0);
        });
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        if (event.getPlayerUuid() != null) {
            UUID uuid = event.getPlayerUuid();
            lastVoiceAlertTime.remove(uuid);
            OpusDecoder decoder = playerDecoders.remove(uuid);
            if (decoder != null) {
                decoder.close();
            }
        }
    }
}