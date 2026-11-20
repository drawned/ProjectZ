package me.drawn.projectz.inventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class InventoryTabButton extends AbstractWidget {
    private final InventoryTab tab;
    private final Runnable onSelect;
    private boolean active;

    public InventoryTabButton(int x, int y, InventoryTab tab, boolean active, Runnable onSelect) {
        super(x, y, SurvivalInventoryUi.TAB_WIDTH, SurvivalInventoryUi.TAB_HEIGHT, tab.label());
        this.tab = tab;
        this.active = active;
        this.onSelect = onSelect;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public InventoryTab tab() {
        return tab;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int back = active ? SurvivalInventoryUi.COLOR_TAB_ACTIVE : SurvivalInventoryUi.COLOR_TAB_BACK;
        int top = getY() - (active ? 1 : 0);
        int bottom = getY() + height;
        graphics.fill(getX(), top, getX() + width, bottom, back);
        int accent = active ? SurvivalInventoryUi.COLOR_ACCENT : SurvivalInventoryUi.COLOR_ACCENT_DIM;
        graphics.fill(getX(), top, getX() + width, top + 1, accent);
        if (isHovered() && !active) {
            graphics.fill(getX(), bottom - 1, getX() + width, bottom, SurvivalInventoryUi.COLOR_ACCENT_DIM);
        }
        int textColor = active ? SurvivalInventoryUi.COLOR_TEXT : SurvivalInventoryUi.COLOR_TEXT_DIM;
        graphics.drawCenteredString(Minecraft.getInstance().font, tab.label(), getX() + width / 2, top + 7, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, tab.label());
    }
}
