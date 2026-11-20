package me.drawn.projectz.inventory;

import me.drawn.projectz.inventory.menu.SurvivalInventoryMenu;
import me.drawn.projectz.mixin.PlayerInventoryMenuAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;

public final class PlayerInventoryMenus {
    private PlayerInventoryMenus() {}

    public static void applyForGameMode(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            useVanillaMenu(player);
        } else {
            useSurvivalMenu(player);
        }
    }

    /**
     * Servidor, depois do cliente já ter o menu certo.
     * Survival: importa restos do inventário vanilla uma vez e sincroniza a grade.
     * Criativo: devolve a grade para o inventário vanilla e só então abre o menu vanilla.
     */
    public static void completeServerSync(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            useVanillaMenu(player);
            SurvivalStorage.restoreVanillaStorage(player);
            player.inventoryMenu.broadcastFullState();
            return;
        }
        SurvivalStorage.migrateVanillaStorage(player);
        useSurvivalMenu(player);
        player.initInventoryMenu();
        GridInventoryNetworking.sendFullSync(player);
        SurvivalStorage.applySelection(player);
    }

    public static void useVanillaMenu(Player player) {
        PlayerInventoryMenuAccessor access = (PlayerInventoryMenuAccessor) player;
        AbstractContainerMenu openMenu = access.projectz$getContainerMenu();
        InventoryMenu previous = player.inventoryMenu;
        if (previous != null && !(previous instanceof SurvivalInventoryMenu) && previous.getClass() == InventoryMenu.class) {
            return;
        }
        if (previous instanceof SurvivalInventoryMenu) {
            previous.removed(player);
        }
        InventoryMenu vanilla = new InventoryMenu(player.getInventory(), !player.level().isClientSide(), player);
        access.projectz$setInventoryMenu(vanilla);
        if (openMenu == previous || openMenu == null) {
            access.projectz$setContainerMenu(vanilla);
        }
    }

    public static void useSurvivalMenu(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            useVanillaMenu(player);
            return;
        }
        PlayerInventoryMenuAccessor access = (PlayerInventoryMenuAccessor) player;
        AbstractContainerMenu openMenu = access.projectz$getContainerMenu();
        InventoryMenu previous = player.inventoryMenu;
        SurvivalInventoryMenu survival;
        if (previous instanceof SurvivalInventoryMenu existing) {
            survival = existing;
        } else {
            survival = new SurvivalInventoryMenu(player.getInventory(), !player.level().isClientSide(), player);
            access.projectz$setInventoryMenu(survival);
        }
        survival.ensureSurvivalLayout();
        if (openMenu == previous || openMenu == player.inventoryMenu) {
            access.projectz$setContainerMenu(survival);
        }
    }
}
