package me.drawn.projectz.inventory.menu;

import me.drawn.projectz.inventory.ItemSizeRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot secundário: só itens 1×1, sem limite artificial de quantidade. */
public class WeaponSecondarySlot extends Slot {
    public WeaponSecondarySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ItemSizeRegistry.fitsSecondarySlot(stack);
    }
}
