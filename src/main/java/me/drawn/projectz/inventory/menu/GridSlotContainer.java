package me.drawn.projectz.inventory.menu;

import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.PlayerGridInventory;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Container fantasma para os slots da grade. Todas as leituras e escritas passam pelo attachment,
 * então o slot vanilla nunca guarda uma cópia própria do stack.
 */
public class GridSlotContainer implements Container {
    private final Player player;

    public GridSlotContainer(Player player) {
        this.player = player;
    }

    private PlayerGridInventory grid() {
        return ModAttachments.getGrid(player);
    }

    public static int xOf(int index) {
        return index % ItemSizeRegistry.GRID_COLUMNS;
    }

    public static int yOf(int index) {
        return index / ItemSizeRegistry.GRID_COLUMNS;
    }

    @Override
    public int getContainerSize() {
        return ItemSizeRegistry.GRID_COLUMNS * ItemSizeRegistry.GRID_ROWS;
    }

    @Override
    public boolean isEmpty() {
        return grid().count() == 0;
    }

    @Override
    public ItemStack getItem(int index) {
        return grid().findAnchor(xOf(index), yOf(index))
                .map(placed -> placed.stack())
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        int x = xOf(index);
        int y = yOf(index);
        PlayerGridInventory grid = grid();
        ItemStack current = grid.findAnchor(x, y).map(placed -> placed.stack()).orElse(ItemStack.EMPTY);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(count, current.getCount());
        ItemStack extracted = current.copyWithCount(take);
        if (take >= current.getCount()) {
            grid.removeAtAnchor(x, y);
        } else {
            ItemStack left = current.copy();
            left.shrink(take);
            grid.setAtAnchor(x, y, left);
        }
        setChanged();
        return extracted;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return grid().removeAtAnchor(xOf(index), yOf(index));
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        grid().setAtAnchor(xOf(index), yOf(index), stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        ModAttachments.markGridDirty(player);
    }

    @Override
    public boolean stillValid(Player user) {
        return user == player || !user.isRemoved();
    }

    @Override
    public void clearContent() {
        grid().clear();
        setChanged();
    }
}
