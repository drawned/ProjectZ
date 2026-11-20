package me.drawn.projectz.inventory.client;

import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.SurvivalStorage;
import me.drawn.projectz.inventory.WeaponBarState;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Duas mãos no lugar da hotbar. Scroll e as teclas 1/2 alternam qual está ativa.
 */
public final class WeaponHud implements LayeredDraw.Layer {
    public static final WeaponHud INSTANCE = new WeaponHud();

    private WeaponHud() {}

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || player.isCreative() || mc.options.hideGui) {
            return;
        }
        int selected = ModAttachments.getWeaponBar(player).getSelected();
        ItemStack primary = player.getInventory().getItem(SurvivalStorage.PRIMARY_SLOT);
        ItemStack secondary = player.getInventory().getItem(SurvivalStorage.SECONDARY_SLOT);

        int primarySize = 32;
        int secondarySize = 22;
        int gap = 6;
        int total = primarySize + gap + secondarySize;
        int x = (graphics.guiWidth() - total) / 2;
        int y = graphics.guiHeight() - primarySize - 8;

        drawHand(graphics, mc.font, x, y + (primarySize - primarySize), primarySize, primary, "1",
                selected == WeaponBarState.PRIMARY, primary);
        drawHand(graphics, mc.font, x + primarySize + gap, y + (primarySize - secondarySize), secondarySize, secondary, "2",
                selected == WeaponBarState.SECONDARY, secondary);
    }

    private static void drawHand(GuiGraphics graphics, Font font, int x, int y, int size, ItemStack stack, String key, boolean active, ItemStack shown) {
        int fill = active ? SurvivalInventoryUi.COLOR_SLOT_ACTIVE : 0xC0141414;
        graphics.fill(x, y, x + size, y + size, fill);
        graphics.renderOutline(x, y, size, size, active ? SurvivalInventoryUi.COLOR_ACCENT : SurvivalInventoryUi.COLOR_SLOT_BORDER);
        graphics.drawString(font, key, x + 2, y + 2, active ? SurvivalInventoryUi.COLOR_TEXT : SurvivalInventoryUi.COLOR_TEXT_DIM, false);
        if (shown.isEmpty()) {
            return;
        }
        int icon = 16;
        int ix = x + (size - icon) / 2;
        int iy = y + (size - icon) / 2;
        graphics.renderItem(shown, ix, iy);
        graphics.renderItemDecorations(font, shown, ix, iy);
        if (active && ItemSizeRegistry.getSize(shown).area() > 1) {
            graphics.drawString(font, Component.literal(ItemSizeRegistry.getSize(shown).width() + "×" + ItemSizeRegistry.getSize(shown).height()),
                    x, y - 10, SurvivalInventoryUi.COLOR_TEXT_DIM, false);
        }
    }
}
