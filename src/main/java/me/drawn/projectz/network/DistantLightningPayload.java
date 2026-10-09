package me.drawn.projectz.network;

import me.drawn.projectz.ProjectZ;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DistantLightningPayload(double x, double y, double z, long seed) implements CustomPacketPayload {
    public static final Type<DistantLightningPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "distant_lightning"));

    public static final StreamCodec<FriendlyByteBuf, DistantLightningPayload> STREAM_CODEC = CustomPacketPayload.codec(
            DistantLightningPayload::write,
            DistantLightningPayload::new
    );

    public DistantLightningPayload(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readLong());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeLong(this.seed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}