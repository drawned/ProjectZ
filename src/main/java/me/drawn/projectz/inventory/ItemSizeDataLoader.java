package me.drawn.projectz.inventory;

import com.mojang.serialization.JsonOps;
import me.drawn.projectz.ProjectZ;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ItemSizeDataLoader extends SimplePreparableReloadListener<Void> {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(ProjectZ.MODID, "item_grid_sizes.json");

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
        Optional<Resource> resource = manager.getResource(FILE);
        if (resource.isEmpty()) {
            return;
        }
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            var json = net.minecraft.util.GsonHelper.parse(reader);
            ItemSizeRegistry.ItemSizeFile.CODEC.parse(JsonOps.INSTANCE, json)
                    .result()
                    .ifPresentOrElse(
                            file -> ItemSizeRegistry.loadFromData(file.sizes()),
                            () -> ProjectZ.LOGGER.warn("Failed to parse {}", FILE)
                    );
        } catch (Exception e) {
            ProjectZ.LOGGER.error("Failed to load item grid sizes from {}", FILE, e);
        }
    }
}
