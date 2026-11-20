package me.drawn.projectz.inventory;

import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ProjectZ.MODID)
public class InventoryEvents {
    private static final int SERVER_MENU_SYNC_DELAY_TICKS = 1;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        handleGameModeSync(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity().level().isClientSide() || event.isWasDeath() && !event.getOriginal().isDeadOrDying()) {
            return;
        }
        PlayerGridInventory from = ModAttachments.getGrid(event.getOriginal());
        PlayerGridInventory to = ModAttachments.getGrid(event.getEntity());
        to.load(from.save(event.getOriginal().registryAccess()), event.getEntity().registryAccess());
        ModAttachments.getWeaponBar(event.getEntity()).setSelected(ModAttachments.getWeaponBar(event.getOriginal()).getSelected());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        handleGameModeSync(event.getEntity());
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        GameType next = event.getNewGameMode();
        // Saindo do survival: a grade volta para o inventário vanilla antes do menu criativo abrir.
        if (SurvivalStorage.isActive(player) && (next == GameType.CREATIVE || next == GameType.SPECTATOR)) {
            SurvivalStorage.restoreVanillaStorage(serverPlayer);
        }
        scheduleServerMenuSync(serverPlayer);
    }

    public static void handleGameModeSync(Player player) {
        if (player.level().isClientSide()) {
            PlayerInventoryMenus.applyForGameMode(player);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            scheduleServerMenuSync(serverPlayer);
        }
    }

    private static void scheduleServerMenuSync(ServerPlayer player) {
        ProjectZ.scheduler.schedule(SERVER_MENU_SYNC_DELAY_TICKS, () -> {
            if (!player.isAlive() || player.hasDisconnected()) {
                return;
            }
            PlayerInventoryMenus.completeServerSync(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !SurvivalStorage.isActive(player)) {
            return;
        }
        SurvivalStorage.clampSelection(player);
        if (player.tickCount % 20 == 0) {
            sweepStrayItems(player);
        }
    }

    /**
     * Rede de segurança: se algum mod colocou item nos slots 2–35, ele entra na grade
     * neste tick em vez de existir só no servidor.
     */
    private static void sweepStrayItems(Player player) {
        boolean moved = false;
        for (int slot = 2; slot < SurvivalStorage.STORAGE_END; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            ItemStack left = SurvivalStorage.insert(player, copy);
            if (!left.isEmpty()) {
                player.drop(left, false);
            }
            moved = true;
        }
        if (moved) {
            ModAttachments.syncGrid(player);
        }
    }

    /**
     * Recusa o pickup vanilla quando a mochila não tem célula livre. Sem isso o item ia para os
     * slots 2–35, sumia da tela e só reaparecia no drop. Quando cabe, o {@code Inventory#add}
     * desviado pelo mixin coloca o stack na grade.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!SurvivalStorage.isActive(player) || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        if (stack.isEmpty()) {
            return;
        }
        if (ModAttachments.getGrid(player).firstFit(stack) == null
                && !canMergeAny(ModAttachments.getGrid(player), stack)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    private static boolean canMergeAny(PlayerGridInventory grid, ItemStack stack) {
        ItemStack probe = stack.copy();
        for (var placed : grid.view()) {
            ItemStack target = placed.stack();
            if (ItemStack.isSameItemSameComponents(target, probe) && target.getCount() < target.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A morte vanilla só esvazia o inventário de slots. A mochila mora no attachment, então sem isto
     * o jogador morre, dropa as mãos e a grade volta no respawn (ou some se keepInventory estiver off
     * e o clone não copiar).
     */
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (event.isCanceled() || !SurvivalStorage.isActive(player)) {
            return;
        }
        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        PlayerGridInventory grid = ModAttachments.getGrid(player);
        for (ItemStack stack : grid.extractAll()) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.2, player.getZ(), stack);
            drop.setPickUpDelay(40);
            event.getDrops().add(drop);
        }
        ModAttachments.syncGrid(player);
    }
}
