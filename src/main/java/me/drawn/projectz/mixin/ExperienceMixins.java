package me.drawn.projectz.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ExperienceMixins {

    @Mixin(Monster.class)
    public abstract static class MonsterMixin {
        @Inject(method = "shouldDropExperience", at = @At("HEAD"), cancellable = true)
        private void onShouldDropExperience(CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(false);
        }
    }

    @Mixin(LivingEntity.class)
    public abstract static class LivingEntityMixin {
        @Inject(method = "shouldDropExperience", at = @At("HEAD"), cancellable = true)
        private void onShouldDropExperience(CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(false);
        }
    }

}
