package me.drawn.projectz.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A troca das mãos é tratada em {@code InventoryClientEvents#onScroll}.
 * O mixin permanece registrado para não quebrar configs antigas, mas não intercepta mais o scroll.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
}
