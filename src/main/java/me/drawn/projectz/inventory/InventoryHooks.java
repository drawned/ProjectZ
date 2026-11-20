package me.drawn.projectz.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryHooks {
    private InventoryHooks() {}

    public static boolean tryInsertIntoGrid(Player player, ItemStack stack) {
        return SurvivalStorage.tryConsumeIntoGrid(player, stack);
    }
}
