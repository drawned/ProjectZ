package me.drawn.projectz.inventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.inventory.InventoryEvents;
import me.drawn.projectz.inventory.SurvivalStorage;
import me.drawn.projectz.inventory.WeaponBarHandler;
import me.drawn.projectz.inventory.WeaponBarState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ProjectZ.MODID, value = Dist.CLIENT)
public class InventoryClientEvents {
    public static final KeyMapping PRIMARY_KEY = new KeyMapping(
            "key.projectz.primary",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_1,
            "key.categories.projectz"
    );
    public static final KeyMapping SECONDARY_KEY = new KeyMapping(
            "key.projectz.secondary",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_2,
            "key.categories.projectz"
    );

    @EventBusSubscriber(modid = ProjectZ.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(PRIMARY_KEY);
            event.register(SECONDARY_KEY);
        }

        @SubscribeEvent
        public static void registerHud(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, ProjectZ.id("weapon_hud"), WeaponHud.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void hideSurvivalHudHotbar(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getPlayer() != null) {
            InventoryEvents.handleGameModeSync(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide()) {
            InventoryEvents.handleGameModeSync(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity().level().isClientSide()) {
            InventoryEvents.handleGameModeSync(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator() || mc.screen != null) {
            return;
        }
        SurvivalStorage.clampSelection(mc.player);
        while (PRIMARY_KEY.consumeClick()) {
            WeaponBarHandler.select(mc.player, WeaponBarState.PRIMARY);
        }
        while (SECONDARY_KEY.consumeClick()) {
            WeaponBarHandler.select(mc.player, WeaponBarState.SECONDARY);
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator() || mc.screen != null || event.getScrollDeltaY() == 0) {
            return;
        }
        int next = event.getScrollDeltaY() > 0 ? WeaponBarState.PRIMARY : WeaponBarState.SECONDARY;
        WeaponBarHandler.select(mc.player, next);
        event.setCanceled(true);
    }
}
