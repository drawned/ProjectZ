package me.drawn.projectz.network;

import me.drawn.projectz.ProjectZ;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AirdropVisualPayload(
        double tx, double ty, double tz,
        double sx, double sy, double sz,
        double ex, double ey, double ez,
        float size,
        String lootId,
        int duration
) implements CustomPacketPayload {
    public static final Type<AirdropVisualPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "airdrop_visual"));

    public static final StreamCodec<FriendlyByteBuf, AirdropVisualPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AirdropVisualPayload decode(FriendlyByteBuf buf) {
            return new AirdropVisualPayload(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), // tx, ty, tz
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), // sx, sy, sz
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), // ex, ey, ez
                    buf.readFloat(),                                      // size
                    buf.readUtf(),                                        // lootId
                    buf.readVarInt()                                      // duration
            );
        }

        @Override
        public void encode(FriendlyByteBuf buf, AirdropVisualPayload payload) {
            buf.writeDouble(payload.tx());
            buf.writeDouble(payload.ty());
            buf.writeDouble(payload.tz());
            buf.writeDouble(payload.sx());
            buf.writeDouble(payload.sy());
            buf.writeDouble(payload.sz());
            buf.writeDouble(payload.ex());
            buf.writeDouble(payload.ey());
            buf.writeDouble(payload.ez());
            buf.writeFloat(payload.size());
            buf.writeUtf(payload.lootId());
            buf.writeVarInt(payload.duration());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


