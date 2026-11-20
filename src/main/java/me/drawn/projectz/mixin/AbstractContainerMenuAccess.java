package me.drawn.projectz.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccess {
    @Accessor("lastSlots")
    NonNullList<ItemStack> projectz$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> projectz$getRemoteSlots();
}
