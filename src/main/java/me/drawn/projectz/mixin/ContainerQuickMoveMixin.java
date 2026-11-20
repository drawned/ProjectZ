package me.drawn.projectz.mixin;

import me.drawn.projectz.inventory.SurvivalStorage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click de baú e de outros menus copia para os slots 9–35 do jogador.
 * Esses slots estão escondidos no survival, então a cópia termina na mochila.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerQuickMoveMixin {
    @Inject(
            method = "moveItemStackTo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectz$moveIntoPack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (stack.isEmpty() || startIndex < 0 || endIndex > self.slots.size() || startIndex >= endIndex) {
            return;
        }
        Slot first = self.slots.get(startIndex);
        if (!(first.container instanceof Inventory inventory)) {
            return;
        }
        Player player = inventory.player;
        if (player == null || player.level().isClientSide() || !SurvivalStorage.isActive(player)) {
            return;
        }
        boolean targetsStorage = false;
        for (int i = startIndex; i < endIndex; i++) {
            Slot slot = self.slots.get(i);
            if (slot.container != inventory) {
                return;
            }
            int containerSlot = slot.getContainerSlot();
            if (containerSlot >= 2 && containerSlot < 36) {
                targetsStorage = true;
            }
        }
        if (!targetsStorage) {
            return;
        }
        int before = stack.getCount();
        ItemStack left = SurvivalStorage.insert(player, stack.copy());
        stack.setCount(left.getCount());
        cir.setReturnValue(left.getCount() != before);
    }
}
