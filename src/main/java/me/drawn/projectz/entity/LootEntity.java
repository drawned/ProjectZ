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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LootEntity extends Entity implements Container {
    private static final EntityDataAccessor<String> LOOT_ID = SynchedEntityData.defineId(LootEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_RES = SynchedEntityData.defineId(LootEntity.class, EntityDataSerializers.STRING);

    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean initializedLoot = false;
    private int playerAwayTicks = 0;
    private UUID originPointId = null;

    public LootEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LOOT_ID, "");
        builder.define(MODEL_RES, "projectz:weapon_crate");
    }

    public void setLootId(String id) {
        this.entityData.set(LOOT_ID, id);
        var table = LootConfigManager.getLootConfig(id);
        if (table != null && table.model != null) {
            setModel(table.model);
        }
    }

    public String getLootId() {
        return this.entityData.get(LOOT_ID);
    }

    public void setModel(String model) {
        this.entityData.set(MODEL_RES, model);
    }

    public String getModel() {
        return this.entityData.get(MODEL_RES);
    }

    public void setOriginPointId(UUID originPointId) {
        this.originPointId = originPointId;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public UUID getOriginPointId() {
        return this.originPointId;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isNoGravity() && !this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide()) {
            if (!initializedLoot) {
                generateLoot();
                initializedLoot = true;
            }

            Player closestPlayer = this.level().getNearestPlayer(this, 48.0);
            if (closestPlayer == null) {
                playerAwayTicks++;
                if (playerAwayTicks >= 200) {
                    this.discard();
                }
            } else {
                playerAwayTicks = 0;
            }
        }
    }

    private void generateLoot() {
        String id = getLootId();
        if (!id.isEmpty()) {
            var rolled = LootConfigManager.rollLoot(id);

            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < 27; i++) {
                slots.add(i);
            }

            Collections.shuffle(slots);

            for (int i = 0; i < Math.min(rolled.size(), slots.size()); i++) {
                int randomSlot = slots.get(i);
                this.items.set(randomSlot, rolled.get(i));
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> ChestMenu.threeRows(containerId, playerInventory, this),
                    Component.literal("Ponto de Loot")
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
    public void remove(Entity.@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide() && originPointId != null) {
            for (LootConfigManager.LootPoint pt : LootConfigManager.getLootPoints()) {
                if (pt.id.equals(originPointId)) {
                    pt.activeEntityUuid = null;
                    break;
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLootId(tag.getString("LootId"));
        this.initializedLoot = tag.getBoolean("InitializedLoot");
        if (tag.hasUUID("OriginPointId")) {
            this.originPointId = tag.getUUID("OriginPointId");
        }
        ContainerHelper.loadAllItems(tag, this.items, this.level().registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("LootId", getLootId());
        tag.putBoolean("InitializedLoot", this.initializedLoot);
        if (originPointId != null) {
            tag.putUUID("OriginPointId", originPointId);
        }
        ContainerHelper.saveAllItems(tag, this.items, this.level().registryAccess());
    }
}