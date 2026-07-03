package me.drawn.projectz.client;

import com.deadzoke.ignitehud.api.widget.WidgetAttribute;
import com.deadzoke.ignitehud.client.GuiPlayerAttributes;
import com.deadzoke.ignitehud.util.ColorUtil;
import me.drawn.projectz.client.renders.FastZombieRenderer;
import me.drawn.projectz.ProjectZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ProjectZ.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, FastZombieRenderer::new);
    }

    private static final ResourceLocation STAMINA = ResourceLocation.parse("minecraft:textures/hud/stamina.png");
    private static final ResourceLocation STAMINA_EXHAUSTED = ResourceLocation.parse("minecraft:textures/hud/stamina_exhausted.png");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            GuiPlayerAttributes.register(
                    new WidgetAttribute.Builder("stamina")
                            .withColor(ColorUtil.hexToDecimal("#ffff38"))
                            /*.withFade(() -> {
                                // more complex fade soon
                                final StaminaValues current = getCurrentStamina();
                                float fade = 1.0F;

                                if(current.exhausted) {
                                    fade = 0.1F;
                                }

                                return fade;
                            })*/
                            .withValue(() -> String.valueOf(getCurrentStamina().hudValue()))
                            .withIcon(() -> getCurrentStamina().exhausted ? STAMINA_EXHAUSTED : STAMINA)
                            .build()
            );
        });
    }

    private static StaminaValues getCurrentStamina() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if(player == null) return StaminaValues.empty;

        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains("parcool:stamina", Tag.TAG_COMPOUND)) {
            CompoundTag staminaTag = persistentData.getCompound("parcool:stamina");

            int current = staminaTag.getInt("value");
            int max     = staminaTag.getInt("max");
            boolean exhausted = staminaTag.getBoolean("exhausted");

            int hudValue = (current / max) * 100;

            return new StaminaValues(current, max, exhausted, hudValue);
        }
        return StaminaValues.empty;
    }

    public record StaminaValues(int curent, int max, boolean exhausted, int hudValue) {
        public static final StaminaValues empty = new StaminaValues(2000, 2000, false, 100);
    }

}
