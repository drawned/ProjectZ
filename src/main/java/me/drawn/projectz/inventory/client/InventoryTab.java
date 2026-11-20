package me.drawn.projectz.inventory.client;

import net.minecraft.network.chat.Component;

public enum InventoryTab {
    INVENTORY("gui.projectz.tab.inventory"),
    CRAFTING("gui.projectz.tab.crafting"),
    MAP("gui.projectz.tab.map");

    private final String translationKey;

    InventoryTab(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component label() {
        return Component.translatable(translationKey);
    }
}
