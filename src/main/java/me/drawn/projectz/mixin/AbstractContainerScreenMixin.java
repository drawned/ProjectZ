package me.drawn.projectz.mixin;

import me.drawn.projectz.inventory.client.SurvivalInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla só testa um quadrado de 16px. Itens maiores precisam ocupar a área inteira no hit-test,
 * senão clicar no meio de um rifle acerta a célula coberta (e não faz nada) ou dropa o item.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void projectz$expandedSlotHit(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!(self instanceof SurvivalInventoryScreen screen)) {
            return;
        }
        cir.setReturnValue(screen.hitsSlot(slot, mouseX, mouseY));
    }

    /** Slots da outra aba continuam ativos para o sync, mas não podem receber clique. */
    @Inject(method = "findSlot", at = @At("HEAD"), cancellable = true)
    private void projectz$visibleSlotsOnly(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!(self instanceof SurvivalInventoryScreen screen)) {
            return;
        }
        cir.setReturnValue(screen.findVisibleSlot(mouseX, mouseY));
    }

    /**
     * Teclas 3–9 fariam SWAP para slots que o survival não mostra. Só 1 e 2 (mãos) continuam válidas.
     */
    @Inject(method = "checkHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
    private void projectz$onlyTwoHands(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!(self instanceof SurvivalInventoryScreen screen)) {
            return;
        }
        if (!screen.allowHotbarSwapKey(keyCode, scanCode)) {
            cir.setReturnValue(true);
        }
    }
}
