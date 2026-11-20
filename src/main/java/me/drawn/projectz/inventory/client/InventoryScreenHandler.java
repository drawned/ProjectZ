package me.drawn.projectz.inventory.client;

import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.inventory.PlayerInventoryMenus;
import me.drawn.projectz.inventory.menu.SurvivalInventoryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ProjectZ.MODID, value = Dist.CLIENT)
public class InventoryScreenHandler {
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        PlayerInventoryMenus.useSurvivalMenu(player);
        if (player.inventoryMenu instanceof SurvivalInventoryMenu survivalMenu) {
            survivalMenu.ensureSurvivalLayout();
            event.setNewScreen(new SurvivalInventoryScreen(survivalMenu, player));
        }
    }
}
