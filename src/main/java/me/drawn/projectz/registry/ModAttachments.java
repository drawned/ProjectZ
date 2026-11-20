package me.drawn.projectz.registry;

import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.inventory.GridInventoryNetworking;
import me.drawn.projectz.inventory.PlayerGridInventory;
import me.drawn.projectz.inventory.WeaponBarState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ProjectZ.MODID);

    public static final Supplier<AttachmentType<PlayerGridInventory>> GRID_INVENTORY = ATTACHMENT_TYPES.register(
            "grid_inventory",
            () -> AttachmentType.builder(PlayerGridInventory::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerGridInventory>() {
                        @Override
                        public PlayerGridInventory read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            PlayerGridInventory inv = new PlayerGridInventory();
                            inv.load(tag, provider);
                            return inv;
                        }

                        @Override
                        public CompoundTag write(PlayerGridInventory attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<WeaponBarState>> WEAPON_BAR = ATTACHMENT_TYPES.register(
            "weapon_bar",
            () -> AttachmentType.builder(WeaponBarState::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, WeaponBarState>() {
                        @Override
                        public WeaponBarState read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            WeaponBarState state = new WeaponBarState();
                            state.load(tag);
                            return state;
                        }

                        @Override
                        public CompoundTag write(WeaponBarState attachment, HolderLookup.Provider provider) {
                            CompoundTag tag = new CompoundTag();
                            attachment.save(tag);
                            return tag;
                        }
                    })
                    .copyOnDeath()
                    .build()
    );

    private ModAttachments() {}

    public static PlayerGridInventory getGrid(Player player) {
        return player.getData(GRID_INVENTORY);
    }

    public static WeaponBarState getWeaponBar(Player player) {
        return player.getData(WEAPON_BAR);
    }

    public static void syncGrid(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !player.isCreative() && !player.level().isClientSide()) {
            GridInventoryNetworking.sendFullSync(serverPlayer);
        }
    }

    /** Marca a grade para o próximo broadcast do container. Sync completo só no servidor. */
    public static void markGridDirty(Player player) {
        if (player instanceof ServerPlayer && !player.isCreative()) {
            syncGrid(player);
        }
    }
}
