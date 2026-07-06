package me.drawn.projectz.mixin;

import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// removing F3 + S
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(
            method = "handleDebugKeys(I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelDynamicTextureDump(int key, CallbackInfoReturnable<Boolean> cir) {
        // GLFW_KEY_S = 83. Blocks when 'S' is pressed alongside f3
        if (key == GLFW.GLFW_KEY_S) {
            cir.setReturnValue(true);
        }
    }

}
