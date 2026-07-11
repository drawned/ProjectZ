package me.drawn.projectz.client;

import com.deadzoke.ignitehud.api.widget.WidgetAttribute;
import com.deadzoke.ignitehud.client.GuiPlayerAttributes;
import com.deadzoke.ignitehud.util.ColorUtil;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.client.renders.AirdropCrateRenderer;
import me.drawn.projectz.client.renders.LootEntityRenderer;
import me.drawn.projectz.config.LootConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ProjectZ.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    private static final ResourceLocation STAMINA = ResourceLocation.parse("minecraft:textures/hud/stamina.png");
    private static final ResourceLocation STAMINA_EXHAUSTED = ResourceLocation.parse("minecraft:textures/hud/stamina_exhausted.png");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProjectZ.AIRDROP_CRATE_ENTITY.get(), AirdropCrateRenderer::new);
        event.registerEntityRenderer(ProjectZ.LOOT_ENTITY.get(), LootEntityRenderer::new);
    }

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

        CompoundTag attachmentsNbt = player.serializeAttachments(player.registryAccess());
        if (attachmentsNbt != null && attachmentsNbt.contains("parcool:stamina")) {
            CompoundTag staminaTag = attachmentsNbt.getCompound("parcool:stamina");

            int current = staminaTag.getInt("value");
            int max     = staminaTag.getInt("max");
            boolean exhausted = staminaTag.getBoolean("exhausted");

            int hudValue = (int) ((current / (float) max) * 100);

            return new StaminaValues(current, max, exhausted, hudValue);
        }
        return StaminaValues.empty;
    }

    public record StaminaValues(int current, int max, boolean exhausted, int hudValue) {
        public static final StaminaValues empty = new StaminaValues(2000, 2000, false, 100);
    }

}
