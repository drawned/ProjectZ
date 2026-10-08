package me.drawn.projectz.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import me.cortex.voxy.common.world.other.Mapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Mapper.StateEntry.class, remap = false)
public class VoxyStateEntryMixin {

    @Redirect(
            method = "deserialize",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/datafixers/DataFixer;update(Lcom/mojang/datafixers/DSL$TypeReference;Lcom/mojang/serialization/Dynamic;II)Lcom/mojang/serialization/Dynamic;",
                    remap = false
            ),
            remap = false
    )
    private static Dynamic<?> projectz$bypassBrokenDfu(DataFixer fixer, DSL.TypeReference type, Dynamic<?> input, int oldVersion, int newVersion) {
        // No 1.21.1 o DFU quebra com IllegalArgumentException para block_state.
        // Retornamos o próprio input intacto para o Voxy seguir para o fallback de Ar.
        return input;
    }
}