package me.drawn.projectz.entity;

import me.drawn.projectz.config.LootConfigManager;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AirdropCrateEntity extends Entity implements Container {
    private static final EntityDataAccessor<String> LOOT_ID = SynchedEntityData.defineId(AirdropCrateEntity.class, EntityDataSerializers.STRING);

    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean initializedLoot = false;

    public AirdropCrateEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LOOT_ID, "");
    }

    public void setLootId(String id) {
        this.entityData.set(LOOT_ID, id);
    }

    public String getLootId() {
        return this.entityData.get(LOOT_ID);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isNoGravity() && !this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.98, 0.98));

        if (!this.level().isClientSide() && !initializedLoot && this.onGround()) {
            generateLoot();
            initializedLoot = true;
        }
    }

    private void generateLoot() {
        String id = getLootId();
        if (!id.isEmpty()) {
            var rolled = LootConfigManager.rollLoot(id);
            for (int i = 0; i < Math.min(rolled.size(), items.size()); i++) {
                this.items.set(i, rolled.get(i));
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> ChestMenu.threeRows(containerId, playerInventory, this),
                    Component.literal("Caixa de Airdrop")
            ));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        checkDespawn();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
        checkDespawn();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        checkDespawn();
    }

    public void checkDespawn() {
        if (!this.level().isClientSide() && this.isEmpty() && initializedLoot) {
            this.discard();
        }
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return this.isAlive() && player.distanceToSqr(this) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLootId(tag.getString("LootId"));
        this.initializedLoot = tag.getBoolean("InitializedLoot");
        ContainerHelper.loadAllItems(tag, this.items, this.level().registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("LootId", getLootId());
        tag.putBoolean("InitializedLoot", this.initializedLoot);
        ContainerHelper.saveAllItems(tag, this.items, this.level().registryAccess());
    }
}