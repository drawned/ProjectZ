package me.drawn.projectz.inventory.menu;

import com.mojang.datafixers.util.Pair;
import me.drawn.projectz.inventory.ItemSizeRegistry;
import me.drawn.projectz.inventory.SurvivalStorage;
import me.drawn.projectz.inventory.client.SurvivalInventoryUi;
import me.drawn.projectz.mixin.InventoryMenuAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class SurvivalInventoryMenuLayout {
    /** Índices 0–4 são o crafting 2×2 herdado do InventoryMenu (escondido). */
    public static final int ARMOR_START = 5;
    public static final int ARMOR_END = 9;

    public static final int GRID_SLOT_START = 9;
    public static final int GRID_SLOT_COUNT = ItemSizeRegistry.GRID_COLUMNS * ItemSizeRegistry.GRID_ROWS;
    public static final int GRID_SLOT_END = GRID_SLOT_START + GRID_SLOT_COUNT;

    public static final int WEAPON_PRIMARY_SLOT_INDEX = GRID_SLOT_END;
    public static final int WEAPON_SECONDARY_SLOT_INDEX = WEAPON_PRIMARY_SLOT_INDEX + 1;
    public static final int WEAPON_SLOT_END = WEAPON_SECONDARY_SLOT_INDEX + 1;
    public static final int OFFHAND_SLOT = WEAPON_SLOT_END;
    public static final int HIDDEN_STORAGE_START = OFFHAND_SLOT + 1;
    public static final int HIDDEN_STORAGE_END = HIDDEN_STORAGE_START + 34;

    public static final int CRAFT3_RESULT_SLOT = HIDDEN_STORAGE_END;
    public static final int CRAFT3_SLOT_START = CRAFT3_RESULT_SLOT + 1;
    public static final int CRAFT3_SLOT_COUNT = 9;
    public static final int CRAFT3_SLOT_END = CRAFT3_SLOT_START + CRAFT3_SLOT_COUNT;

    public static final int PLAYER_SLOT_END = CRAFT3_SLOT_END;

    private static final int HIDDEN = -2000;

    private static final EquipmentSlot[] ARMOR_ORDER = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<EquipmentSlot, ResourceLocation> ARMOR_ICONS = Map.of(
            EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    );

    private SurvivalInventoryMenuLayout() {}

    public static void apply(
            SurvivalInventoryMenu menu,
            Inventory playerInventory,
            Player owner,
            TransientCraftingContainer craft3x3,
            ResultContainer result3x3
    ) {
        InventoryMenuAccess access = (InventoryMenuAccess) menu;
        var craft2 = access.projectz$getCraftSlots();
        var result2 = access.projectz$getResultSlots();

        menu.clearLayoutSlots();

        menu.registerLayoutSlot(new ResultSlot(owner, craft2, result2, 0, HIDDEN, HIDDEN) {
            @Override
            public boolean isActive() {
                return false;
            }
        });
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                menu.registerLayoutSlot(new Slot(craft2, j + i * 2, HIDDEN, HIDDEN) {
                    @Override
                    public boolean isActive() {
                        return false;
                    }
                });
            }
        }

        for (int k = 0; k < 4; k++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[k];
            menu.registerLayoutSlot(new SurvivalArmorSlot(
                    playerInventory, owner, equipmentSlot, 39 - k,
                    SurvivalInventoryUi.ARMOR_X, SurvivalInventoryUi.ARMOR_Y + k * SurvivalInventoryUi.CELL,
                    ARMOR_ICONS.get(equipmentSlot)));
        }

        for (int y = 0; y < ItemSizeRegistry.GRID_ROWS; y++) {
            for (int x = 0; x < ItemSizeRegistry.GRID_COLUMNS; x++) {
                menu.registerLayoutSlot(new GridInventorySlot(
                        owner, x, y,
                        SurvivalInventoryUi.GRID_X + x * SurvivalInventoryUi.CELL,
                        SurvivalInventoryUi.GRID_Y + y * SurvivalInventoryUi.CELL));
            }
        }

        menu.registerLayoutSlot(new Slot(playerInventory, SurvivalStorage.PRIMARY_SLOT,
                SurvivalInventoryUi.WEAPON_PRIMARY_X, SurvivalInventoryUi.WEAPON_PRIMARY_Y));
        menu.registerLayoutSlot(new WeaponSecondarySlot(playerInventory, SurvivalStorage.SECONDARY_SLOT,
                SurvivalInventoryUi.WEAPON_SECONDARY_X, SurvivalInventoryUi.WEAPON_SECONDARY_Y));

        menu.registerLayoutSlot(new Slot(playerInventory, 40, SurvivalInventoryUi.OFFHAND_X, SurvivalInventoryUi.OFFHAND_Y) {
            @Override
            public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        // Slots 2–35 do inventário. Índices reais: 2–8 (hotbar morta) e 9–35 (storage).
        for (int slot = 2; slot < SurvivalStorage.STORAGE_END; slot++) {
            menu.registerLayoutSlot(new HiddenStorageSlot(playerInventory, slot));
        }

        menu.registerLayoutSlot(new ResultSlot(owner, craft3x3, result3x3, 0,
                SurvivalInventoryUi.CRAFT_RESULT_X, SurvivalInventoryUi.CRAFT_RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                menu.registerLayoutSlot(new Slot(craft3x3, col + row * 3,
                        SurvivalInventoryUi.CRAFT_GRID_X + col * SurvivalInventoryUi.CELL,
                        SurvivalInventoryUi.CRAFT_GRID_Y + row * SurvivalInventoryUi.CELL));
            }
        }
    }

    public static boolean isGrid(int index) {
        return index >= GRID_SLOT_START && index < GRID_SLOT_END;
    }

    public static boolean isHandSlot(int index) {
        return index == WEAPON_PRIMARY_SLOT_INDEX
                || index == WEAPON_SECONDARY_SLOT_INDEX
                || index == OFFHAND_SLOT;
    }

    public static boolean isArmor(int index) {
        return index >= ARMOR_START && index < ARMOR_END;
    }

    public static boolean isCraftInput(int index) {
        return index >= CRAFT3_SLOT_START && index < CRAFT3_SLOT_END;
    }
}
