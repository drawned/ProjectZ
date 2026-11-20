package me.drawn.projectz.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerInventoryMenuAccessor {
    @Mutable
    @Accessor("inventoryMenu")
    void projectz$setInventoryMenu(InventoryMenu menu);

    @Accessor("containerMenu")
    AbstractContainerMenu projectz$getContainerMenu();

    @Mutable
    @Accessor("containerMenu")
    void projectz$setContainerMenu(AbstractContainerMenu menu);
}
