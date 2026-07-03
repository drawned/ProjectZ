package me.drawn.projectz.mixin;

import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin {

    @Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
    private void onIsSunSensitive(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

}