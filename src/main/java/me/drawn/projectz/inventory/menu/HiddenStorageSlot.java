package me.drawn.projectz.inventory.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Slots 2–35 do inventário vanilla. Continuam no menu para o sync de container não mudar de índice,
 * mas não aceitam nem mostram item. Pickup é desviado para a grade antes de chegar aqui.
 */
public class HiddenStorageSlot extends Slot {
    public HiddenStorageSlot(Container container, int index) {
        super(container, index, -2000, -2000);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isHighlightable() {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack getItem() {
        ItemStack current = super.getItem();
        if (!current.isEmpty()) {
            this.container.setItem(this.getContainerSlot(), ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }
}
