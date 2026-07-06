package me.drawn.projectz.mixin;

import me.cortex.voxy.client.core.VoxyRenderSystem;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VoxyRenderSystem.class, remap = false)
public class VoxyRenderSystemMixin {

    @Inject(method = "renderOpaque*", at = @At("HEAD"), cancellable = true)
    private void preventDefaultFramebufferCrash(CallbackInfo ci) {
        // getting the current Framebuffer Object (FBO) from OpenGL
        int boundFbo = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        // if FBO is 0, skip it so voxy do not crash
        if (boundFbo == 0) {
            ci.cancel();
        }
    }
}