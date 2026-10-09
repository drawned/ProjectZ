package me.drawn.projectz.registry;

import me.drawn.projectz.ProjectZ;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ProjectZ.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> AIRPLANE_SOUND = SOUND_EVENTS.register("airplane",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "airplane")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FAR_THUNDER = SOUND_EVENTS.register(
            "far_thunder",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "far_thunder"))
    );
}