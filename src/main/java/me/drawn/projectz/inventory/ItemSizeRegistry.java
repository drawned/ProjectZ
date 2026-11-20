package me.drawn.projectz.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.drawn.projectz.ProjectZ;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemSizeRegistry {
    public static final int GRID_COLUMNS = 7;
    public static final int GRID_ROWS = 7;

    private static final Map<ResourceLocation, GridDimensions> EXPLICIT = new HashMap<>();
    private static GridDimensions defaultSize = GridDimensions.ONE;

    private ItemSizeRegistry() {}

    public static void loadFromData(List<ItemSizeEntry> entries) {
        EXPLICIT.clear();
        for (ItemSizeEntry entry : entries) {
            ResourceLocation id = ResourceLocation.tryParse(entry.item);
            if (id == null) continue;
            EXPLICIT.put(id, new GridDimensions(entry.width, entry.height));
        }
    }

    public static void setDefault(GridDimensions dimensions) {
        defaultSize = dimensions;
    }

    /** Itens que cabem no slot secundário (apenas pequenos, 1x1). */
    public static boolean fitsSecondarySlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        GridDimensions size = getSize(stack);
        return size.width() == 1 && size.height() == 1;
    }

    public static GridDimensions getSize(ItemStack stack) {
        if (stack.isEmpty()) {
            return GridDimensions.ONE;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        GridDimensions explicit = EXPLICIT.get(id);
        if (explicit != null) {
            return explicit;
        }
        return inferFromItemId(id);
    }

    private static GridDimensions inferFromItemId(ResourceLocation id) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);

        if (namespace.contains("superbwarfare") || namespace.contains("pointblank") || namespace.contains("tacz")) {
            if (path.contains("shotgun") || path.contains("aa12") || path.contains("spas")) {
                return new GridDimensions(5, 2);
            }
            if (path.contains("sniper") || path.contains("awp") || path.contains("barrett") || path.contains("m200")) {
                return new GridDimensions(5, 1);
            }
            if (path.contains("rifle") || path.contains("ak") || path.contains("m4") || path.contains("ar_")
                    || path.contains("assault") || path.contains("carbine")) {
                return new GridDimensions(4, 1);
            }
            if (path.contains("smg") || path.contains("mp5") || path.contains("uzi") || path.contains("vector")) {
                return new GridDimensions(3, 1);
            }
            if (path.contains("pistol") || path.contains("glock") || path.contains("deagle") || path.contains("handgun")) {
                return new GridDimensions(2, 1);
            }
            if (path.contains("gun") || path.contains("weapon") || path.contains("firearm")) {
                return new GridDimensions(4, 1);
            }
            if (path.contains("ammo") || path.contains("magazine") || path.contains("clip")) {
                return new GridDimensions(1, 2);
            }
        }

        if (path.contains("shield")) {
            return new GridDimensions(2, 2);
        }
        if (path.contains("bow") || path.contains("crossbow") || path.contains("trident")) {
            return new GridDimensions(3, 1);
        }

        return defaultSize;
    }

    public record ItemSizeEntry(String item, int width, int height) {
        public static final Codec<ItemSizeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(ItemSizeEntry::item),
                Codec.INT.fieldOf("width").forGetter(ItemSizeEntry::width),
                Codec.INT.fieldOf("height").forGetter(ItemSizeEntry::height)
        ).apply(instance, ItemSizeEntry::new));
    }

    public record ItemSizeFile(List<ItemSizeEntry> sizes) {
        public static final Codec<ItemSizeFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemSizeEntry.CODEC.listOf().fieldOf("sizes").forGetter(ItemSizeFile::sizes)
        ).apply(instance, ItemSizeFile::new));
    }

    public static void bootstrapDefaults() {
        defaultSize = GridDimensions.ONE;
        ProjectZ.LOGGER.info("Item grid sizes: {} columns x {} rows, heuristic weapon sizing enabled",
                GRID_COLUMNS, GRID_ROWS);
    }
}
