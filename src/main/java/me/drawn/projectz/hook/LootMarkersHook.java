package me.drawn.projectz.hook;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.config.LootConfigManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Arrays;
import java.util.UUID;

public class LootMarkersHook {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long tickCount = event.getServer().getTickCount();
        if (tickCount % 40 != 0) return;

        BlueMapAPI.getInstance().ifPresent(api -> {
            api.getWorld("world#minecraft:overworld").ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    MarkerSet loots = map.getMarkerSets()
                            .computeIfAbsent("loots", id -> MarkerSet.builder()
                                    .label("Loots")
                                    .toggleable(true)
                                    .defaultHidden(false)
                                    .build());
                    loots.getMarkers().clear();

                    for(LootConfigManager.LootPoint point : LootConfigManager.getLootPoints()) {
                        POIMarker marker = POIMarker.builder()
                                .label(point.lootId)
                                .icon(determineIcon(point.lootId), 32, 32)
                                .styleClasses("loot", point.lootId)
                                .position(new Vector3d(
                                        point.pos.getX() + 0.5,
                                        point.pos.getY(),
                                        point.pos.getZ() + 0.5
                                ))
                                // Opcional: melhora usabilidade
                                .detail("<b>" + point.lootId + "</b><br> Loot Point")
                                .build();

                        loots.getMarkers().put(UUID.randomUUID().toString(), marker);
                    }

                    map.getMarkerSets().put("loots", loots);
                }
            });
        });
    }

    public static String determineIcon(String id) {
        return switch (id) {
            case "weapon" -> "icons/weapon.png";
            case "melee" -> "icons/melee.png";
            case "radio" -> "icons/radio.png";
            case "crate", "small" -> "icons/crate.png";
            case "car", "car_parts", "mechanics", "toolbox" -> "icons/car_crate.png";
            case "drink" -> "icons/drink.png";
            case "large", "big", "large_crate" -> "icons/large.png";
            case "medic" -> "icons/medic.png";
            case "ammo" -> "icons/ammo.png";
            default -> "icons/crate.png"; // fallback
        };
    }
}