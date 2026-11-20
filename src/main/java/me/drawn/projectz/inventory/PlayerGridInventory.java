package me.drawn.projectz.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Autoridade única do inventário em grade. Cada item ocupa uma âncora e as células cobertas
 * ficam bloqueadas. Stacks nunca são compartilhados com o inventário vanilla.
 */
public class PlayerGridInventory {
    private final List<PlacedStack> placed = new ArrayList<>();
    /** Evita reentrada quando o menu lê o grid enquanto estamos gravando nele. */
    private boolean mutating;

    public List<PlacedStack> view() {
        List<PlacedStack> copy = new ArrayList<>(placed.size());
        for (PlacedStack entry : placed) {
            copy.add(new PlacedStack(entry.x(), entry.y(), entry.stack().copy()));
        }
        return List.copyOf(copy);
    }

    public boolean isMutating() {
        return mutating;
    }

    public int count() {
        return placed.size();
    }

    public Optional<PlacedStack> findAt(int cellX, int cellY) {
        for (PlacedStack entry : placed) {
            GridDimensions size = ItemSizeRegistry.getSize(entry.stack());
            if (size.covers(entry.x(), entry.y(), cellX, cellY)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public boolean isAnchor(int cellX, int cellY) {
        return findAnchor(cellX, cellY).isPresent();
    }

    public Optional<PlacedStack> findAnchor(int cellX, int cellY) {
        for (PlacedStack entry : placed) {
            if (entry.isAnchor(cellX, cellY)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public boolean canPlace(ItemStack stack, int x, int y) {
        return canPlace(stack, x, y, null);
    }

    public boolean canPlace(ItemStack stack, int x, int y, PlacedStack ignored) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        GridDimensions size = ItemSizeRegistry.getSize(stack);
        if (!size.fits(x, y, ItemSizeRegistry.GRID_COLUMNS, ItemSizeRegistry.GRID_ROWS)) {
            return false;
        }
        for (int dx = 0; dx < size.width(); dx++) {
            for (int dy = 0; dy < size.height(); dy++) {
                Optional<PlacedStack> occupant = findAt(x + dx, y + dy);
                if (occupant.isPresent() && occupant.get() != ignored) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean place(ItemStack stack, int x, int y) {
        if (!canPlace(stack, x, y)) {
            return false;
        }
        placed.add(new PlacedStack(x, y, stack.copy()));
        return true;
    }

    public ItemStack removeAtAnchor(int x, int y) {
        Iterator<PlacedStack> it = placed.iterator();
        while (it.hasNext()) {
            PlacedStack entry = it.next();
            if (entry.isAnchor(x, y)) {
                it.remove();
                return entry.stack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Substitui o item cuja âncora está em (x, y). Se a célula estiver coberta por outro item,
     * a escrita é recusada — nunca cria um segundo stack por cima.
     */
    public boolean setAtAnchor(int x, int y, ItemStack stack) {
        if (mutating) {
            return false;
        }
        mutating = true;
        try {
            PlacedStack existing = findAnchor(x, y).orElse(null);
            if (stack == null || stack.isEmpty()) {
                if (existing != null) {
                    removeInternal(existing);
                }
                return true;
            }
            if (!canPlace(stack, x, y, existing)) {
                return false;
            }
            if (existing != null) {
                removeInternal(existing);
            }
            placed.add(new PlacedStack(x, y, stack.copy()));
            return true;
        } finally {
            mutating = false;
        }
    }

    private void removeInternal(PlacedStack entry) {
        placed.remove(entry);
    }

    public boolean tryMergeInto(PlacedStack target, ItemStack incoming) {
        if (target == null || incoming == null || incoming.isEmpty()) {
            return false;
        }
        ItemStack inGrid = target.stack();
        if (!ItemStack.isSameItemSameComponents(incoming, inGrid)) {
            return false;
        }
        int space = inGrid.getMaxStackSize() - inGrid.getCount();
        if (space <= 0) {
            return false;
        }
        int move = Math.min(space, incoming.getCount());
        inGrid.grow(move);
        incoming.shrink(move);
        return true;
    }

    /**
     * Insere o máximo possível (merge, depois primeira célula livre). Retorna o que não coube.
     */
    public ItemStack insertItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        mergeIntoExisting(remaining);
        while (!remaining.isEmpty()) {
            GridDimensions size = ItemSizeRegistry.getSize(remaining);
            int[] cell = firstFit(remaining, size);
            if (cell == null) {
                break;
            }
            ItemStack chunk = remaining.copy();
            int perCell = Math.min(chunk.getMaxStackSize(), chunk.getCount());
            chunk.setCount(perCell);
            placed.add(new PlacedStack(cell[0], cell[1], chunk));
            remaining.shrink(perCell);
        }
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private void mergeIntoExisting(ItemStack remaining) {
        for (PlacedStack entry : placed) {
            if (remaining.isEmpty()) {
                return;
            }
            tryMergeInto(entry, remaining);
        }
    }

    public int[] firstFit(ItemStack stack) {
        return firstFit(stack, ItemSizeRegistry.getSize(stack));
    }

    private int[] firstFit(ItemStack stack, GridDimensions size) {
        int maxY = ItemSizeRegistry.GRID_ROWS - size.height();
        int maxX = ItemSizeRegistry.GRID_COLUMNS - size.width();
        for (int y = 0; y <= maxY; y++) {
            for (int x = 0; x <= maxX; x++) {
                if (canPlace(stack, x, y)) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    public void clear() {
        placed.clear();
    }

    public List<ItemStack> extractAll() {
        List<ItemStack> stacks = new ArrayList<>();
        for (PlacedStack entry : placed) {
            if (!entry.stack().isEmpty()) {
                stacks.add(entry.stack().copy());
            }
        }
        placed.clear();
        return stacks;
    }

    public void pruneInvalid() {
        placed.removeIf(entry -> entry.stack().isEmpty() || !fitsBounds(entry));
        collapseOverlaps();
    }

    private boolean fitsBounds(PlacedStack entry) {
        return ItemSizeRegistry.getSize(entry.stack())
                .fits(entry.x(), entry.y(), ItemSizeRegistry.GRID_COLUMNS, ItemSizeRegistry.GRID_ROWS);
    }

    /** Se dois itens se sobrepõem (dados corrompidos), o segundo volta para a primeira célula livre. */
    private void collapseOverlaps() {
        List<PlacedStack> snapshot = new ArrayList<>(placed);
        placed.clear();
        for (PlacedStack entry : snapshot) {
            ItemStack leftover = insertItem(entry.stack());
            if (!leftover.isEmpty()) {
                // Sem espaço após colapso: mantém no fim da lista só se couber na posição original.
                if (canPlace(leftover, entry.x(), entry.y())) {
                    placed.add(new PlacedStack(entry.x(), entry.y(), leftover));
                }
            }
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (PlacedStack entry : placed) {
            if (entry.stack().isEmpty()) {
                continue;
            }
            CompoundTag saved = (CompoundTag) entry.stack().save(registries);
            CompoundTag row = new CompoundTag();
            row.putInt("X", entry.x());
            row.putInt("Y", entry.y());
            row.put("Stack", saved);
            list.add(row);
        }
        tag.put("Items", list);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        placed.clear();
        if (tag == null || !tag.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        List<ItemStack> overflow = new ArrayList<>();
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag entry)) {
                continue;
            }
            int x = entry.getInt("X");
            int y = entry.getInt("Y");
            ItemStack stack = ItemStack.parse(registries, entry.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            if (!canPlace(stack, x, y)) {
                overflow.add(stack);
                continue;
            }
            placed.add(new PlacedStack(x, y, stack));
        }
        for (ItemStack leftover : overflow) {
            insertItem(leftover);
        }
    }

    public PlayerGridInventory copy(HolderLookup.Provider registries) {
        PlayerGridInventory copy = new PlayerGridInventory();
        copy.load(save(registries), registries);
        return copy;
    }

    /**
     * Importa stacks do inventário vanilla sem duplicar o que já está no grid.
     * Usado só na migração (login / troca para survival).
     */
    public List<ItemStack> importLooseStacks(Iterable<ItemStack> stacks) {
        List<ItemStack> overflow = new ArrayList<>();
        if (stacks == null) {
            return overflow;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack left = insertItem(stack);
            if (!left.isEmpty()) {
                overflow.add(left);
            }
        }
        return overflow;
    }
}
