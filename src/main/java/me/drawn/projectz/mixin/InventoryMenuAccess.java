package me.drawn.projectz.mixin;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InventoryMenu.class)
public interface InventoryMenuAccess {
    @Accessor("craftSlots")
    CraftingContainer projectz$getCraftSlots();

    @Accessor("resultSlots")
    ResultContainer projectz$getResultSlots();
}
