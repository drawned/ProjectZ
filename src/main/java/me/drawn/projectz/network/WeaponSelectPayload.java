package me.drawn.projectz.network;

import me.drawn.projectz.ProjectZ;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponSelectPayload(int selected) implements CustomPacketPayload {
    public static final Type<WeaponSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "weapon_select"));

    public static final StreamCodec<FriendlyByteBuf, WeaponSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WeaponSelectPayload::selected,
            WeaponSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
