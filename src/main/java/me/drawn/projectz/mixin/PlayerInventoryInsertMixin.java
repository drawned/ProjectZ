package me.drawn.projectz.mixin;

import me.drawn.projectz.inventory.SurvivalStorage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pickup, /give e qualquer {@code inventory.add} no survival vão para a grade.
 * No criativo o método vanilla segue intacto — por isso trocar de modo não duplica itens.
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryInsertMixin {
    @Shadow
    public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void projectz$routeSurvivalInsert(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.level().isClientSide() || !SurvivalStorage.isActive(player)) {
            return;
        }
        if (stack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        // Equipar armadura pelo pickup vanilla continua tentando o slot de armadura primeiro
        // quando o caller passa um índice de armadura (36–39) ou offhand (40).
        if (slot >= 36) {
            return;
        }
        boolean consumed = SurvivalStorage.tryConsumeIntoGrid(player, stack);
        cir.setReturnValue(consumed);
    }
}
