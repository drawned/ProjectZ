package me.drawn.projectz.inventory;

import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponte entre o grid (autoridade) e os dois slots de mão do inventário vanilla.
 * Slots 2–35 ficam sempre vazios no survival, então pickup, /give e drop não podem
 * criar itens invisíveis nem duplicá-los no criativo.
 */
public final class SurvivalStorage {
    public static final int PRIMARY_SLOT = 0;
    public static final int SECONDARY_SLOT = 1;
    public static final int STORAGE_START = 9;
    public static final int STORAGE_END = 36;

    private SurvivalStorage() {}

    public static boolean isActive(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }

    public static ItemStack primary(Player player) {
        return player.getInventory().getItem(PRIMARY_SLOT);
    }

    public static ItemStack secondary(Player player) {
        return player.getInventory().getItem(SECONDARY_SLOT);
    }

    public static int selectedInventorySlot(Player player) {
        return ModAttachments.getWeaponBar(player).getSelected() == WeaponBarState.SECONDARY
                ? SECONDARY_SLOT
                : PRIMARY_SLOT;
    }

    public static void applySelection(Player player) {
        if (!isActive(player)) {
            return;
        }
        player.getInventory().selected = selectedInventorySlot(player);
    }

    public static void select(Player player, int which) {
        if (!isActive(player)) {
            return;
        }
        ModAttachments.getWeaponBar(player).setSelected(which == WeaponBarState.SECONDARY
                ? WeaponBarState.SECONDARY
                : WeaponBarState.PRIMARY);
        applySelection(player);
    }

    public static void toggle(Player player) {
        if (!isActive(player)) {
            return;
        }
        ModAttachments.getWeaponBar(player).toggle();
        applySelection(player);
    }

    /**
     * Hotbar vanilla só existe nos slots 0 e 1. Qualquer outro índice volta para a arma ativa.
     */
    public static void clampSelection(Player player) {
        if (!isActive(player)) {
            return;
        }
        int selected = player.getInventory().selected;
        if (selected != PRIMARY_SLOT && selected != SECONDARY_SLOT) {
            applySelection(player);
            return;
        }
        ModAttachments.getWeaponBar(player).setSelected(selected == SECONDARY_SLOT
                ? WeaponBarState.SECONDARY
                : WeaponBarState.PRIMARY);
    }

    /**
     * Esvazia os slots de armazenamento vanilla que o jogador não deve mais ver.
     * Itens encontrados lá são importados uma vez para o grid.
     */
    public static void migrateVanillaStorage(Player player) {
        if (!isActive(player) || player.level().isClientSide()) {
            return;
        }
        Inventory inventory = player.getInventory();
        PlayerGridInventory grid = ModAttachments.getGrid(player);
        List<ItemStack> loose = new ArrayList<>();
        for (int slot = 2; slot < STORAGE_END; slot++) {
            if (slot >= 2 && slot < STORAGE_START || slot >= STORAGE_START) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()) {
                    loose.add(stack.copy());
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        spill(player, grid.importLooseStacks(loose));
        applySelection(player);
    }

    /**
     * Move o grid inteiro de volta para o inventário vanilla antes do criativo.
     * Nada é apagado e nada é copiado duas vezes.
     */
    public static void restoreVanillaStorage(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        PlayerGridInventory grid = ModAttachments.getGrid(player);
        if (grid.count() == 0) {
            return;
        }
        List<ItemStack> stacks = grid.extractAll();
        Inventory inventory = player.getInventory();
        List<ItemStack> leftover = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!inventory.add(stack)) {
                leftover.add(stack);
            }
        }
        spill(player, leftover);
        ModAttachments.syncGrid(player);
    }

    public static ItemStack insert(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isActive(player)) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        ItemStack leftover = ModAttachments.getGrid(player).insertItem(stack);
        if (player instanceof ServerPlayer) {
            ModAttachments.syncGrid(player);
        }
        return leftover;
    }

    /**
     * Tenta colocar na grade. Retorna true somente se o stack original foi totalmente consumido.
     * O ItemStack recebido é encolhido no lugar, como o {@code Inventory#add} espera.
     */
    public static boolean tryConsumeIntoGrid(Player player, ItemStack stack) {
        if (!isActive(player) || stack == null || stack.isEmpty()) {
            return false;
        }
        int before = stack.getCount();
        ItemStack leftover = insert(player, stack);
        int accepted = before - leftover.getCount();
        if (accepted <= 0) {
            return false;
        }
        stack.shrink(accepted);
        return stack.isEmpty();
    }

    public static void spill(Player player, List<ItemStack> stacks) {
        if (player.level().isClientSide() || stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
