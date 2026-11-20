package me.drawn.projectz.inventory.menu;

import me.drawn.projectz.inventory.GridDimensions;
import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.PlacedStack;
import me.drawn.projectz.inventory.PlayerGridInventory;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class GridInventorySlot extends Slot {
    private final Player owner;
    private final int cellX;
    private final int cellY;

    public GridInventorySlot(Player owner, int cellX, int cellY, int screenX, int screenY) {
        super(new GridSlotContainer(owner), cellX + cellY * ItemSizeRegistry.GRID_COLUMNS, screenX, screenY);
        this.owner = owner;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    private PlayerGridInventory grid() {
        return ModAttachments.getGrid(owner);
    }

    @Override
    public boolean isActive() {
        return !isBlockedByMultiCell();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty() || isBlockedByMultiCell()) {
            return false;
        }
        return grid().canPlace(stack, cellX, cellY, grid().findAnchor(cellX, cellY).orElse(null));
    }

    @Override
    public boolean mayPickup(Player player) {
        return hasItem();
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public ItemStack getItem() {
        return grid().findAnchor(cellX, cellY)
                .map(PlacedStack::stack)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void set(ItemStack stack) {
        if (isBlockedByMultiCell() && !stack.isEmpty()) {
            return;
        }
        grid().setAtAnchor(cellX, cellY, stack);
        setChanged();
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        set(newStack);
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack removed = this.container.removeItem(this.getContainerSlot(), amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public int getMaxStackSize() {
        return getMaxStackSize(getItem());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 64;
        }
        return stack.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ModAttachments.markGridDirty(owner);
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public boolean isBlockedByMultiCell() {
        Optional<PlacedStack> at = grid().findAt(cellX, cellY);
        return at.isPresent() && !at.get().isAnchor(cellX, cellY);
    }

    public Optional<PlacedStack> occupant() {
        return grid().findAt(cellX, cellY);
    }

    public GridDimensions getOccupantSize() {
        return occupant()
                .map(placed -> ItemSizeRegistry.getSize(placed.stack()))
                .orElse(GridDimensions.ONE);
    }
}
