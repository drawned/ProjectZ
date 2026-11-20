package me.drawn.projectz.network;

import me.drawn.projectz.ProjectZ;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponBarSyncPayload(int selected) implements CustomPacketPayload {
    public static final Type<WeaponBarSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "weapon_bar_sync"));

    public static final StreamCodec<FriendlyByteBuf, WeaponBarSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WeaponBarSyncPayload::selected,
            WeaponBarSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
