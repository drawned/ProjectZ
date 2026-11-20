package me.drawn.projectz.inventory;

import net.minecraft.world.item.ItemStack;

public record PlacedStack(int x, int y, ItemStack stack) {
    public PlacedStack {
        stack = stack == null ? ItemStack.EMPTY : stack;
    }

    public PlacedStack withStack(ItemStack newStack) {
        return new PlacedStack(x, y, newStack);
    }

    public boolean isAnchor(int cellX, int cellY) {
        return x == cellX && y == cellY;
    }
}
