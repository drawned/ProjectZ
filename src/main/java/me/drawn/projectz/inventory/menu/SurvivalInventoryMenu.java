package me.drawn.projectz.inventory.menu;

import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.SurvivalStorage;
import me.drawn.projectz.mixin.AbstractContainerMenuAccess;
import me.drawn.projectz.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

public class SurvivalInventoryMenu extends InventoryMenu {
    public static final EquipmentSlot[] ARMOR_ORDER = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final TransientCraftingContainer craftSlots3x3 = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots3x3 = new ResultContainer();
    private final Player owner;
    private boolean survivalLayoutApplied;

    public SurvivalInventoryMenu(Inventory playerInventory, boolean active, Player owner) {
        super(playerInventory, active, owner);
        this.owner = owner;
        // O super registra os slots vanilla antes dos listeners. Trocar a lista aqui garante que o
        // primeiro broadcast já use os índices da mochila, não os 46 slots do criativo.
        ensureSurvivalLayout();
    }

    public Player owner() {
        return owner;
    }

    public void ensureSurvivalLayout() {
        if (survivalLayoutApplied || owner.isCreative()) {
            return;
        }
        survivalLayoutApplied = true;
        SurvivalInventoryMenuLayout.apply(this, owner.getInventory(), owner, craftSlots3x3, resultSlots3x3);
    }

    public void clearLayoutSlots() {
        AbstractContainerMenuAccess tracking = (AbstractContainerMenuAccess) this;
        this.slots.clear();
        tracking.projectz$getLastSlots().clear();
        tracking.projectz$getRemoteSlots().clear();
    }

    public void registerLayoutSlot(Slot slot) {
        this.addSlot(slot);
    }

    public void onGridSynced() {
        for (int i = SurvivalInventoryMenuLayout.GRID_SLOT_START;
             i < SurvivalInventoryMenuLayout.GRID_SLOT_END && i < this.slots.size();
             i++) {
            this.getSlot(i).setChanged();
        }
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (inventory == craftSlots3x3) {
            updateCraft3Result();
        }
    }

    private void updateCraft3Result() {
        if (owner.level().isClientSide() || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CraftingInput input = craftSlots3x3.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        var optional = owner.level().getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, owner.level());
        if (optional.isPresent() && resultSlots3x3.setRecipeUsed(owner.level(), serverPlayer, optional.get())) {
            result = optional.get().value().assemble(input, owner.level().registryAccess());
            if (!result.isItemEnabled(owner.level().enabledFeatures())) {
                result = ItemStack.EMPTY;
            }
        }
        resultSlots3x3.setItem(0, result);
        int resultSlot = SurvivalInventoryMenuLayout.CRAFT3_RESULT_SLOT;
        if (resultSlot < this.slots.size()) {
            this.setRemoteSlot(resultSlot, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    this.containerId, this.incrementStateId(), resultSlot, result));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!player.isCreative() && slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot instanceof HiddenStorageSlot) {
                return;
            }
            if (slot instanceof GridInventorySlot grid && grid.isBlockedByMultiCell()) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
        if (!player.level().isClientSide() && !player.isCreative()) {
            ModAttachments.syncGrid(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.isCreative() || index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem() || slot instanceof HiddenStorageSlot) {
            return ItemStack.EMPTY;
        }
        if (slot instanceof GridInventorySlot grid && grid.isBlockedByMultiCell()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index == SurvivalInventoryMenuLayout.CRAFT3_RESULT_SLOT) {
            // Uma leva por shift. O menu vanilla repetiria enquanto o resultado continuasse cheio.
            ItemStack crafted = stackInSlot.copyWithCount(1);
            ItemStack left = ModAttachments.getGrid(player).insertItem(crafted);
            if (!left.isEmpty()) {
                left = tryPlaceInHandsReturning(left);
            }
            if (!left.isEmpty()) {
                return ItemStack.EMPTY;
            }
            consumeCraftGrid();
            ModAttachments.syncGrid(player);
            return original;
        }

        if (SurvivalInventoryMenuLayout.isCraftInput(index)
                || SurvivalInventoryMenuLayout.isArmor(index)
                || SurvivalInventoryMenuLayout.isHandSlot(index)) {
            if (!moveIntoGrid(stackInSlot)) {
                return ItemStack.EMPTY;
            }
        } else if (SurvivalInventoryMenuLayout.isGrid(index)) {
            if (!shiftFromGrid(player, stackInSlot)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        if (stackInSlot.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stackInSlot);
        ModAttachments.syncGrid(player);
        return original;
    }

    /** Uma leva da grade 3×3. Sem isso o shift do resultado não gasta os ingredientes. */
    private void consumeCraftGrid() {
        for (int i = 0; i < craftSlots3x3.getContainerSize(); i++) {
            ItemStack ingredient = craftSlots3x3.getItem(i);
            if (ingredient.isEmpty()) {
                continue;
            }
            if (ingredient.getItem().hasCraftingRemainingItem()) {
                ItemStack remaining = ingredient.getItem().getCraftingRemainingItem(ingredient);
                ingredient.shrink(1);
                if (ingredient.isEmpty()) {
                    craftSlots3x3.setItem(i, remaining);
                } else if (!remaining.isEmpty()) {
                    ItemStack overflow = SurvivalStorage.insert(owner, remaining);
                    if (!overflow.isEmpty()) {
                        owner.drop(overflow, false);
                    }
                }
            } else {
                ingredient.shrink(1);
                if (ingredient.isEmpty()) {
                    craftSlots3x3.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        slotsChanged(craftSlots3x3);
    }

    private boolean shiftFromGrid(Player player, ItemStack stack) {
        EquipmentSlot equip = player.getEquipmentSlotForItem(stack);
        if (equip.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && stack.getCount() == 1) {
            int armorIndex = SurvivalInventoryMenuLayout.ARMOR_START + (3 - equip.getIndex());
            if (armorIndex >= 0 && armorIndex < this.slots.size() && !this.slots.get(armorIndex).hasItem()
                    && this.slots.get(armorIndex).mayPlace(stack)) {
                this.slots.get(armorIndex).setByPlayer(stack.copyWithCount(1));
                stack.shrink(1);
                return true;
            }
        }
        if (tryPlaceInHands(stack)) {
            return true;
        }
        return moveIntoCraft(stack);
    }

    private boolean tryPlaceInHands(ItemStack stack) {
        tryPlaceInHandsReturning(stack);
        return stack.isEmpty();
    }

    private ItemStack tryPlaceInHandsReturning(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (ItemSizeRegistry.fitsSecondarySlot(stack)) {
            placeIntoHand(SurvivalInventoryMenuLayout.WEAPON_SECONDARY_SLOT_INDEX, stack);
        }
        if (!stack.isEmpty()) {
            placeIntoHand(SurvivalInventoryMenuLayout.WEAPON_PRIMARY_SLOT_INDEX, stack);
        }
        return stack;
    }

    private boolean placeIntoHand(int menuIndex, ItemStack stack) {
        if (menuIndex >= this.slots.size()) {
            return false;
        }
        Slot slot = this.slots.get(menuIndex);
        if (!slot.mayPlace(stack)) {
            return false;
        }
        ItemStack current = slot.getItem();
        if (current.isEmpty()) {
            int move = Math.min(stack.getCount(), slot.getMaxStackSize(stack));
            slot.setByPlayer(stack.copyWithCount(move));
            stack.shrink(move);
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, stack)) {
            return false;
        }
        int space = Math.min(current.getMaxStackSize(), slot.getMaxStackSize(stack)) - current.getCount();
        if (space <= 0) {
            return false;
        }
        int move = Math.min(space, stack.getCount());
        current.grow(move);
        stack.shrink(move);
        slot.setChanged();
        return true;
    }

    private boolean moveIntoGrid(ItemStack stack) {
        ItemStack before = stack.copy();
        ItemStack left = ModAttachments.getGrid(owner).insertItem(stack.copy());
        stack.setCount(left.getCount());
        return left.getCount() != before.getCount();
    }

    private boolean moveIntoCraft(ItemStack stack) {
        return this.moveItemStackTo(stack,
                SurvivalInventoryMenuLayout.CRAFT3_SLOT_START,
                SurvivalInventoryMenuLayout.CRAFT3_SLOT_END,
                false);
    }

    @Override
    public int getGridWidth() {
        return 3;
    }

    @Override
    public int getGridHeight() {
        return 3;
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.craftSlots3x3.asCraftInput(), this.owner.level());
    }

    public TransientCraftingContainer getCraftSlots3x3() {
        return craftSlots3x3;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.clearContainer(player, craftSlots3x3);
        }
        resultSlots3x3.clearContent();
    }

    /**
     * Itens do crafting 3×3 voltam para a grade, não para os slots escondidos do inventário vanilla.
     */
    @Override
    protected void clearContainer(Player player, Container container) {
        if (player.level().isClientSide() || container == null) {
            return;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.removeItemNoUpdate(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack left = SurvivalStorage.insert(player, stack);
            if (!left.isEmpty()) {
                player.drop(left, false);
            }
        }
    }
}
