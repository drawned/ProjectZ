package me.drawn.projectz.inventory.client;

import me.drawn.projectz.inventory.GridDimensions;
import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.PlacedStack;
import me.drawn.projectz.inventory.menu.GridInventorySlot;
import me.drawn.projectz.inventory.menu.HiddenStorageSlot;
import me.drawn.projectz.inventory.menu.SurvivalInventoryMenuLayout;
import me.drawn.projectz.inventory.menu.WeaponSecondarySlot;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

public final class SurvivalSlotGeometry {
    private SurvivalSlotGeometry() {}

    public static Optional<Slot> findSlot(List<Slot> slots, int leftPos, int topPos, double mouseX, double mouseY, SlotVisibility visibility) {
        Slot best = null;
        int bestArea = Integer.MAX_VALUE;
        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot slot = slots.get(i);
            if (!visibility.isVisible(slot) || slot instanceof HiddenStorageSlot) {
                continue;
            }
            if (!contains(slot, leftPos, topPos, mouseX, mouseY)) {
                continue;
            }
            int area = hitWidth(slot) * hitHeight(slot);
            if (slot instanceof GridInventorySlot grid && grid.isBlockedByMultiCell()) {
                Optional<Slot> anchor = findAnchor(slots, grid);
                if (anchor.isPresent()) {
                    int anchorArea = hitWidth(anchor.get()) * hitHeight(anchor.get());
                    if (anchorArea < bestArea) {
                        best = anchor.get();
                        bestArea = anchorArea;
                    }
                }
                continue;
            }
            if (area < bestArea) {
                best = slot;
                bestArea = area;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<Slot> findAnchor(List<Slot> slots, GridInventorySlot covered) {
        return covered.occupant().flatMap(placed -> findAnchor(slots, placed.x(), placed.y()));
    }

    public static Optional<Slot> findAnchor(List<Slot> slots, int cellX, int cellY) {
        for (Slot slot : slots) {
            if (slot instanceof GridInventorySlot grid && grid.getCellX() == cellX && grid.getCellY() == cellY) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public static boolean contains(Slot slot, int leftPos, int topPos, double mouseX, double mouseY) {
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        return mouseX >= x && mouseX < x + hitWidth(slot) && mouseY >= y && mouseY < y + hitHeight(slot);
    }

    public static int hitWidth(Slot slot) {
        if (slot instanceof GridInventorySlot grid) {
            if (grid.isBlockedByMultiCell()) {
                return 0;
            }
            if (grid.hasItem()) {
                return ItemSizeRegistry.getSize(grid.getItem()).width() * SurvivalInventoryUi.CELL;
            }
            return SurvivalInventoryUi.CELL;
        }
        if (slot.index == SurvivalInventoryMenuLayout.WEAPON_PRIMARY_SLOT_INDEX) {
            return SurvivalInventoryUi.WEAPON_PRIMARY_SIZE;
        }
        if (slot instanceof WeaponSecondarySlot) {
            return SurvivalInventoryUi.WEAPON_SECONDARY_SIZE;
        }
        return SurvivalInventoryUi.CELL;
    }

    public static int hitHeight(Slot slot) {
        if (slot instanceof GridInventorySlot grid) {
            if (grid.isBlockedByMultiCell()) {
                return 0;
            }
            if (grid.hasItem()) {
                return ItemSizeRegistry.getSize(grid.getItem()).height() * SurvivalInventoryUi.CELL;
            }
            return SurvivalInventoryUi.CELL;
        }
        if (slot.index == SurvivalInventoryMenuLayout.WEAPON_PRIMARY_SLOT_INDEX) {
            return SurvivalInventoryUi.WEAPON_PRIMARY_SIZE;
        }
        if (slot instanceof WeaponSecondarySlot) {
            return SurvivalInventoryUi.WEAPON_SECONDARY_SIZE;
        }
        return SurvivalInventoryUi.CELL;
    }

    public static GridDimensions sizeOf(Slot slot) {
        if (slot instanceof GridInventorySlot grid && grid.hasItem()) {
            return ItemSizeRegistry.getSize(grid.getItem());
        }
        return GridDimensions.ONE;
    }

    public static Optional<PlacedStack> placedAt(GridInventorySlot slot) {
        return slot.occupant();
    }

    /** Ponto dentro dos 16px do ícone, onde o clique vanilla acerta o slot. */
    public static double vanillaClickX(int leftPos, Slot slot) {
        return leftPos + slot.x + 8;
    }

    public static double vanillaClickY(int topPos, Slot slot) {
        return topPos + slot.y + 8;
    }

    public interface SlotVisibility {
        boolean isVisible(Slot slot);
    }
}
