package me.drawn.projectz.inventory;

import me.drawn.projectz.network.WeaponSelectPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WeaponBarHandler {
    public static final int PRIMARY_INV_SLOT = SurvivalStorage.PRIMARY_SLOT;
    public static final int SECONDARY_INV_SLOT = SurvivalStorage.SECONDARY_SLOT;

    private WeaponBarHandler() {}

    public static void select(Player player, int which) {
        if (!SurvivalStorage.isActive(player)) {
            return;
        }
        SurvivalStorage.select(player, which);
        if (player instanceof LocalPlayer) {
            PacketDistributor.sendToServer(new WeaponSelectPayload(
                    which == WeaponBarState.SECONDARY ? WeaponBarState.SECONDARY : WeaponBarState.PRIMARY));
        } else if (player instanceof ServerPlayer serverPlayer) {
            GridInventoryNetworking.sendFullSync(serverPlayer);
        }
    }

    public static void toggle(Player player) {
        int next = me.drawn.projectz.registry.ModAttachments.getWeaponBar(player).getSelected() == WeaponBarState.SECONDARY
                ? WeaponBarState.PRIMARY
                : WeaponBarState.SECONDARY;
        select(player, next);
    }

    public static void applySelection(Player player) {
        SurvivalStorage.applySelection(player);
    }

    public static void clampHotbarSelection(Player player) {
        SurvivalStorage.clampSelection(player);
    }
}
