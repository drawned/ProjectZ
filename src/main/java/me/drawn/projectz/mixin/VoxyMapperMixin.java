package me.drawn.projectz.mixin;

import me.cortex.voxy.common.world.other.Mapper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Mapper.class, remap = false)
public class VoxyMapperMixin {

    @Redirect(
            method = "loadFromStorage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z",
                    remap = true
            ),
            remap = false
    )
    private boolean projectz$ignoreMissingBlockAir(BlockState state) {
        // Retorna false para enganar o Voxy e fazê-lo aceitar o bloco nulo como Ar legítimo,
        // impedindo o sorteio de blocos aleatórios que corrompe o carregamento dos LODs.
        return false;
    }
}