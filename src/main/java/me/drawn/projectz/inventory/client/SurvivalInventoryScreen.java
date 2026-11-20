package me.drawn.projectz.inventory.client;

import me.drawn.projectz.inventory.GridDimensions;
import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.WeaponBarState;
import me.drawn.projectz.inventory.menu.GridInventorySlot;
import me.drawn.projectz.inventory.menu.HiddenStorageSlot;
import me.drawn.projectz.inventory.menu.SurvivalInventoryMenu;
import me.drawn.projectz.inventory.menu.SurvivalInventoryMenuLayout;
import me.drawn.projectz.inventory.menu.WeaponSecondarySlot;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SurvivalInventoryScreen extends AbstractContainerScreen<SurvivalInventoryMenu> {
    private InventoryTab activeTab = InventoryTab.INVENTORY;
    private final List<InventoryTabButton> tabButtons = new ArrayList<>();

    public SurvivalInventoryScreen(SurvivalInventoryMenu menu, Player player) {
        super(menu, player.getInventory(), Component.translatable("gui.projectz.survival_inventory"));
        this.imageWidth = SurvivalInventoryUi.PANEL_WIDTH;
        this.imageHeight = SurvivalInventoryUi.PANEL_HEIGHT;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        int tabX = this.leftPos;
        int tabY = this.topPos + SurvivalInventoryUi.TAB_STRIP_Y;
        for (InventoryTab tab : InventoryTab.values()) {
            InventoryTabButton button = new InventoryTabButton(tabX, tabY, tab, tab == activeTab, () -> selectTab(tab));
            tabButtons.add(button);
            addRenderableWidget(button);
            tabX += SurvivalInventoryUi.TAB_WIDTH + SurvivalInventoryUi.TAB_GAP;
        }
    }

    private void selectTab(InventoryTab tab) {
        if (tab == activeTab) {
            return;
        }
        activeTab = tab;
        tabButtons.forEach(button -> button.setActive(button.tab() == activeTab));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, SurvivalInventoryUi.COLOR_SCRIM);

        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, SurvivalInventoryUi.COLOR_PANEL);
        graphics.renderOutline(x, y, imageWidth, imageHeight, SurvivalInventoryUi.COLOR_PANEL_EDGE);

        if (activeTab == InventoryTab.INVENTORY) {
            renderInventoryChrome(graphics, x, y);
        } else if (activeTab == InventoryTab.CRAFTING) {
            renderCraftingChrome(graphics, x, y);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.projectz.map_placeholder"),
                    x + 24, y + 72, SurvivalInventoryUi.COLOR_TEXT_DIM, false);
        }
    }

    private void renderInventoryChrome(GuiGraphics graphics, int x, int y) {
        drawSection(graphics, x + 10, y + 18, 164, 142,
                Component.translatable("gui.projectz.section.character"));
        drawSection(graphics, x + SurvivalInventoryUi.GRID_X - 8, y + 18,
                ItemSizeRegistry.GRID_COLUMNS * SurvivalInventoryUi.CELL + 16,
                ItemSizeRegistry.GRID_ROWS * SurvivalInventoryUi.CELL + 28,
                Component.translatable("gui.projectz.section.pack"));
        drawSection(graphics, x + 10, y + SurvivalInventoryUi.WEAPON_PRIMARY_Y - 18, 164, 72,
                Component.translatable("gui.projectz.section.hands"));

        drawCellGrid(graphics, x + SurvivalInventoryUi.ARMOR_X, y + SurvivalInventoryUi.ARMOR_Y, 1, 4);
        drawCellGrid(graphics, x + SurvivalInventoryUi.GRID_X, y + SurvivalInventoryUi.GRID_Y,
                ItemSizeRegistry.GRID_COLUMNS, ItemSizeRegistry.GRID_ROWS);

        boolean primary = this.minecraft != null && this.minecraft.player != null
                && ModAttachments.getWeaponBar(this.minecraft.player).getSelected() == WeaponBarState.PRIMARY;
        drawWell(graphics, x + SurvivalInventoryUi.WEAPON_PRIMARY_X, y + SurvivalInventoryUi.WEAPON_PRIMARY_Y,
                SurvivalInventoryUi.WEAPON_PRIMARY_SIZE, SurvivalInventoryUi.WEAPON_PRIMARY_SIZE,
                primary ? SurvivalInventoryUi.COLOR_SLOT_ACTIVE : SurvivalInventoryUi.COLOR_SLOT);
        drawWell(graphics, x + SurvivalInventoryUi.WEAPON_SECONDARY_X, y + SurvivalInventoryUi.WEAPON_SECONDARY_Y,
                SurvivalInventoryUi.WEAPON_SECONDARY_SIZE, SurvivalInventoryUi.WEAPON_SECONDARY_SIZE,
                primary ? SurvivalInventoryUi.COLOR_SLOT : SurvivalInventoryUi.COLOR_SLOT_ACTIVE);
        drawWell(graphics, x + SurvivalInventoryUi.OFFHAND_X, y + SurvivalInventoryUi.OFFHAND_Y,
                SurvivalInventoryUi.CELL, SurvivalInventoryUi.CELL, SurvivalInventoryUi.COLOR_SLOT);

        graphics.drawString(this.font, "1", x + SurvivalInventoryUi.WEAPON_PRIMARY_X + 3, y + SurvivalInventoryUi.WEAPON_PRIMARY_Y + 3,
                SurvivalInventoryUi.COLOR_TEXT_DIM, false);
        graphics.drawString(this.font, "2", x + SurvivalInventoryUi.WEAPON_SECONDARY_X + 3, y + SurvivalInventoryUi.WEAPON_SECONDARY_Y + 3,
                SurvivalInventoryUi.COLOR_TEXT_DIM, false);

        shadeCoveredCells(graphics, x, y);
    }

    private void renderCraftingChrome(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.translatable("gui.projectz.tab.crafting"),
                x + 20, y + 28, SurvivalInventoryUi.COLOR_TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.projectz.crafting.hint"),
                x + 20, y + 42, SurvivalInventoryUi.COLOR_TEXT_DIM, false);
        drawCellGrid(graphics, x + SurvivalInventoryUi.CRAFT_GRID_X, y + SurvivalInventoryUi.CRAFT_GRID_Y, 3, 3);
        drawWell(graphics, x + SurvivalInventoryUi.CRAFT_RESULT_X, y + SurvivalInventoryUi.CRAFT_RESULT_Y,
                SurvivalInventoryUi.CELL, SurvivalInventoryUi.CELL, SurvivalInventoryUi.COLOR_SLOT);
        graphics.drawString(this.font, ">", x + SurvivalInventoryUi.CRAFT_ARROW_X, y + SurvivalInventoryUi.CRAFT_ARROW_Y,
                SurvivalInventoryUi.COLOR_TEXT_DIM, false);
    }

    private void drawSection(GuiGraphics graphics, int x, int y, int w, int h, Component title) {
        graphics.fill(x, y, x + w, y + h, SurvivalInventoryUi.COLOR_PANEL_INSET);
        graphics.renderOutline(x, y, w, h, SurvivalInventoryUi.COLOR_PANEL_EDGE);
        graphics.drawString(this.font, title, x + 6, y + 4, SurvivalInventoryUi.COLOR_TEXT_DIM, false);
    }

    private void drawCellGrid(GuiGraphics graphics, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawWell(graphics, x + col * SurvivalInventoryUi.CELL, y + row * SurvivalInventoryUi.CELL,
                        SurvivalInventoryUi.CELL, SurvivalInventoryUi.CELL, SurvivalInventoryUi.COLOR_SLOT);
            }
        }
    }

    private void shadeCoveredCells(GuiGraphics graphics, int originX, int originY) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        for (Slot slot : this.menu.slots) {
            if (!(slot instanceof GridInventorySlot grid) || !grid.isBlockedByMultiCell()) {
                continue;
            }
            int x = originX + slot.x;
            int y = originY + slot.y;
            graphics.fill(x + 1, y + 1, x + SurvivalInventoryUi.CELL - 1, y + SurvivalInventoryUi.CELL - 1,
                    SurvivalInventoryUi.COLOR_BLOCKED);
        }
    }

    private void drawWell(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        graphics.renderOutline(x, y, w, h, SurvivalInventoryUi.COLOR_SLOT_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (activeTab == InventoryTab.INVENTORY && this.minecraft != null && this.minecraft.player != null) {
            int mx = this.leftPos + SurvivalInventoryUi.MODEL_X;
            int my = this.topPos + SurvivalInventoryUi.MODEL_Y;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    mx, my,
                    mx + SurvivalInventoryUi.MODEL_W, my + SurvivalInventoryUi.MODEL_H,
                    36, 0.0625F,
                    mouseX, mouseY,
                    this.minecraft.player
            );
        }
        renderGhost(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (activeTab == InventoryTab.INVENTORY) {
            graphics.drawString(this.font, this.title, 16, 6, SurvivalInventoryUi.COLOR_TEXT, false);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!isSlotVisible(slot)) {
            return;
        }
        if (slot instanceof GridInventorySlot grid) {
            renderGridSlot(graphics, grid);
            return;
        }
        if (isWideSlot(slot) && slot.hasItem()) {
            int size = slot.index == SurvivalInventoryMenuLayout.WEAPON_PRIMARY_SLOT_INDEX
                    ? SurvivalInventoryUi.WEAPON_PRIMARY_SIZE
                    : SurvivalInventoryUi.WEAPON_SECONDARY_SIZE;
            int icon = 16;
            int x = slot.x + (size - icon) / 2;
            int y = slot.y + (size - icon) / 2;
            graphics.renderItem(slot.getItem(), x, y, slot.x + slot.y * this.imageWidth);
            graphics.renderItemDecorations(this.font, slot.getItem(), x, y);
            return;
        }
        super.renderSlot(graphics, slot);
    }

    private void renderGridSlot(GuiGraphics graphics, GridInventorySlot slot) {
        if (slot.isBlockedByMultiCell() || !slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();
        GridDimensions size = ItemSizeRegistry.getSize(stack);
        int pixelW = size.width() * SurvivalInventoryUi.CELL;
        int pixelH = size.height() * SurvivalInventoryUi.CELL;
        graphics.fill(slot.x + 1, slot.y + 1, slot.x + pixelW - 1, slot.y + pixelH - 1, SurvivalInventoryUi.COLOR_ITEM_BACK);
        graphics.renderOutline(slot.x, slot.y, pixelW, pixelH, SurvivalInventoryUi.COLOR_ACCENT_DIM);

        int icon = Math.min(32, Math.max(16, Math.min(pixelW, pixelH) - 4));
        float scale = icon / 16f;
        int drawX = slot.x + (pixelW - icon) / 2;
        int drawY = slot.y + (pixelH - icon) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.renderItem(stack, 0, 0, slot.x + slot.y * this.imageWidth);
        graphics.pose().popPose();
        if (stack.getCount() > 1) {
            String count = String.valueOf(stack.getCount());
            int tx = slot.x + pixelW - this.font.width(count) - 2;
            int ty = slot.y + pixelH - 9;
            graphics.drawString(this.font, count, tx, ty, SurvivalInventoryUi.COLOR_TEXT, true);
        }
        if (stack.isBarVisible()) {
            int barX = slot.x + 2;
            int barY = slot.y + pixelH - 3;
            int barW = pixelW - 4;
            int filled = Math.round(stack.getBarWidth() / 13f * barW);
            graphics.fill(barX, barY, barX + barW, barY + 1, 0xFF000000);
            graphics.fill(barX, barY, barX + filled, barY + 1, stack.getBarColor() | 0xFF000000);
        }
    }

    private boolean isWideSlot(Slot slot) {
        return slot.index == SurvivalInventoryMenuLayout.WEAPON_PRIMARY_SLOT_INDEX || slot instanceof WeaponSecondarySlot;
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (!isSlotVisible(slot) || slot instanceof HiddenStorageSlot) {
            return;
        }
        if (slot instanceof GridInventorySlot grid && grid.isBlockedByMultiCell()) {
            return;
        }
        int w = SurvivalSlotGeometry.hitWidth(slot);
        int h = SurvivalSlotGeometry.hitHeight(slot);
        graphics.fill(slot.x, slot.y, slot.x + w, slot.y + h, SurvivalInventoryUi.COLOR_SLOT_HOVER);
    }

    private void renderGhost(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty() || activeTab == InventoryTab.MAP) {
            return;
        }
        GridDimensions size = ItemSizeRegistry.getSize(carried);
        int w = Math.max(SurvivalInventoryUi.CELL, size.width() * SurvivalInventoryUi.CELL);
        int h = Math.max(SurvivalInventoryUi.CELL, size.height() * SurvivalInventoryUi.CELL);
        boolean valid = ghostFits(mouseX, mouseY, carried);
        int color = valid ? 0x553A3A3A : (SurvivalInventoryUi.COLOR_DANGER & 0x00FFFFFF) | 0x66000000;
        graphics.fill(mouseX, mouseY, mouseX + w, mouseY + h, color);
        graphics.renderOutline(mouseX, mouseY, w, h, valid ? SurvivalInventoryUi.COLOR_ACCENT_DIM : SurvivalInventoryUi.COLOR_DANGER);
    }

    private boolean ghostFits(int mouseX, int mouseY, ItemStack carried) {
        return SurvivalSlotGeometry.findSlot(this.menu.slots, this.leftPos, this.topPos, mouseX, mouseY, this::isSlotVisible)
                .filter(slot -> slot.mayPlace(carried) || (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), carried)))
                .isPresent();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Quick-craft vanilla espalharia um stack por célula coberta.
        return false;
    }

    /** Número 1 e 2 trocam com as mãos. 3–9 não existem neste inventário. */
    public boolean allowHotbarSwapKey(int keyCode, int scanCode) {
        if (this.minecraft == null) {
            return false;
        }
        KeyMapping[] slots = this.minecraft.options.keyHotbarSlots;
        for (int i = 2; i < slots.length && i < 9; i++) {
            if (slots[i].isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))) {
                return false;
            }
        }
        return true;
    }

    public Slot findVisibleSlot(double mouseX, double mouseY) {
        Slot found = null;
        int bestArea = Integer.MAX_VALUE;
        for (Slot slot : this.menu.slots) {
            if (!hitsSlot(slot, mouseX, mouseY)) {
                continue;
            }
            int area = SurvivalSlotGeometry.hitWidth(slot) * SurvivalSlotGeometry.hitHeight(slot);
            if (area < bestArea) {
                found = slot;
                bestArea = area;
            }
        }
        return found;
    }

    /** Chamado pelo mixin de hit-test. Coordenadas absolutas da tela. */
    public boolean hitsSlot(Slot slot, double mouseX, double mouseY) {
        if (!isSlotVisible(slot) || !slot.isActive()) {
            return false;
        }
        if (slot instanceof GridInventorySlot grid && grid.isBlockedByMultiCell()) {
            return false;
        }
        return SurvivalSlotGeometry.contains(slot, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (mouseY >= top + SurvivalInventoryUi.TAB_STRIP_Y && mouseY < top && mouseX >= left && mouseX < left + imageWidth) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    private boolean isSlotVisible(Slot slot) {
        if (slot instanceof HiddenStorageSlot || slot.x < 0 || slot.y < 0) {
            return false;
        }
        int index = slot.index;
        if (activeTab == InventoryTab.INVENTORY) {
            return index < SurvivalInventoryMenuLayout.CRAFT3_RESULT_SLOT;
        }
        if (activeTab == InventoryTab.CRAFTING) {
            return index == SurvivalInventoryMenuLayout.CRAFT3_RESULT_SLOT
                    || SurvivalInventoryMenuLayout.isCraftInput(index);
        }
        return false;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem() && isSlotVisible(this.hoveredSlot)) {
            ItemStack stack = this.hoveredSlot.getItem();
            graphics.renderTooltip(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), mouseX, mouseY);
        }
    }

}
