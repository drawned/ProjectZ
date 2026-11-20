package me.drawn.projectz.inventory;

import me.drawn.projectz.inventory.menu.SurvivalInventoryMenu;
import me.drawn.projectz.network.GridInventorySyncPayload;
import me.drawn.projectz.network.WeaponBarSyncPayload;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GridInventoryNetworking {
    private GridInventoryNetworking() {}

    public static void sendFullSync(ServerPlayer player) {
        if (player == null || player.isCreative() || player.hasDisconnected()) {
            return;
        }
        PlayerGridInventory grid = ModAttachments.getGrid(player);
        PacketDistributor.sendToPlayer(player, new GridInventorySyncPayload(grid.save(player.registryAccess())));
        PacketDistributor.sendToPlayer(player, new WeaponBarSyncPayload(ModAttachments.getWeaponBar(player).getSelected()));
    }

    public static void applyClientSync(Player player, GridInventorySyncPayload payload) {
        if (player == null || payload == null || payload.gridData() == null) {
            return;
        }
        ModAttachments.getGrid(player).load(payload.gridData(), player.registryAccess());
        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof SurvivalInventoryMenu survival) {
            survival.onGridSynced();
        }
    }

    public static void applyWeaponBar(Player player, int selected) {
        if (player == null || player.isCreative()) {
            return;
        }
        SurvivalStorage.select(player, selected);
    }
}
