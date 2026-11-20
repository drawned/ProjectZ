package me.drawn.projectz.network;

import me.drawn.projectz.ProjectZ;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GridInventorySyncPayload(CompoundTag gridData) implements CustomPacketPayload {
    public static final Type<GridInventorySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "grid_inventory_sync"));

    public static final StreamCodec<FriendlyByteBuf, GridInventorySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GridInventorySyncPayload decode(FriendlyByteBuf buf) {
            return new GridInventorySyncPayload(buf.readNbt());
        }

        @Override
        public void encode(FriendlyByteBuf buf, GridInventorySyncPayload payload) {
            buf.writeNbt(payload.gridData());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
