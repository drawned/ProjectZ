package me.drawn.projectz.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.entity.LootEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LootConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("projectz");
    private static final Path LOOTS_DIR = CONFIG_DIR.resolve("loots");
    private static final Path POINTS_FILE = CONFIG_DIR.resolve("loot_points.json");
    private static final Path GLOBAL_CONFIG_FILE = CONFIG_DIR.resolve("global_config.json");

    private static final Map<String, LootTableConfig> LOOT_TABLES = new HashMap<>();
    private static final List<LootPoint> LOOT_POINTS = new ArrayList<>();
    public static GlobalConfig config = new GlobalConfig();

    public static class LootTableConfig {
        public int min_items = 1;
        public int max_items = 3;
        public String model = "projectz:item/weapon_crate";
        public List<LootItem> items = new ArrayList<>();
    }

    public static class LootItem {
        public String id;
        public int count = 1;
        public int weight = 1;
    }

    public static class LootPoint {
        public UUID id;
        public BlockPos pos;
        public String dimension;
        public String lootId;
        public double range;
        public UUID activeEntityUuid;
        public long lastSpawnAttempt;

        public LootPoint(BlockPos pos, String dimension, String lootId, double range) {
            this.id = UUID.randomUUID();
            this.pos = pos;
            this.dimension = dimension;
            this.lootId = lootId;
            this.range = range;
            this.activeEntityUuid = null;
            this.lastSpawnAttempt = 0;
        }
    }

    public static class GlobalConfig {
        public int globalLootSpawnInterval = 6000;
        public double globalLootSpawnChance = 0.5;
        public float planeSize = 5.0f;
        public int planeFlightDuration = 4800; // 4800t, 2 minutes until drop
    }

    public static void init() {
        try {
            Files.createDirectories(LOOTS_DIR);
            loadGlobalConfig();
            loadLootTables();
            loadLootPoints();
        } catch (IOException e) {
            ProjectZ.LOGGER.error("Erro ao inicializar o LootConfigManager", e);
        }
    }

    public static void loadGlobalConfig() {
        if (!Files.exists(GLOBAL_CONFIG_FILE)) {
            try (Writer writer = new FileWriter(GLOBAL_CONFIG_FILE.toFile(), StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                ProjectZ.LOGGER.error("Erro ao criar a configuracao global padrao", e);
            }
            return;
        }
        try (Reader reader = new FileReader(GLOBAL_CONFIG_FILE.toFile(), StandardCharsets.UTF_8)) {
            GlobalConfig loaded = GSON.fromJson(reader, GlobalConfig.class);
            if (loaded != null) {
                config = loaded;
            }
        } catch (Exception e) {
            ProjectZ.LOGGER.error("Erro ao carregar a configuracao global", e);
        }
    }

    public static void loadLootTables() {
        LOOT_TABLES.clear();
        File[] files = LOOTS_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            createDefaultLootTable();
            files = LOOTS_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        }

        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - 5);
                try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    LootTableConfig table = GSON.fromJson(reader, LootTableConfig.class);
                    if (table != null) {
                        LOOT_TABLES.put(name, table);
                    }
                } catch (Exception e) {
                    ProjectZ.LOGGER.error("Erro ao carregar a tabela de loot: " + file.getName(), e);
                }
            }
        }
    }

    private static void createDefaultLootTable() {
        LootTableConfig def = new LootTableConfig();
        def.min_items = 2;
        def.max_items = 4;
        def.model = "projectz:item/weapon_crate";

        LootItem apple = new LootItem();
        apple.id = "minecraft:apple";
        apple.count = 5;
        apple.weight = 50;

        LootItem diamond = new LootItem();
        diamond.id = "minecraft:diamond";
        diamond.count = 1;
        diamond.weight = 10;

        def.items.add(apple);
        def.items.add(diamond);

        try (Writer writer = new FileWriter(LOOTS_DIR.resolve("default.json").toFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(def, writer);
        } catch (IOException e) {
            ProjectZ.LOGGER.error("Erro ao criar a tabela de loot padrao", e);
        }
    }

    public static void loadLootPoints() {
        LOOT_POINTS.clear();
        if (!Files.exists(POINTS_FILE)) {
            return;
        }
        try (Reader reader = new FileReader(POINTS_FILE.toFile(), StandardCharsets.UTF_8)) {
            LootPoint[] points = GSON.fromJson(reader, LootPoint[].class);
            if (points != null) {
                LOOT_POINTS.addAll(Arrays.asList(points));
            }
        } catch (Exception e) {
            ProjectZ.LOGGER.error("Erro ao carregar pontos de loot", e);
        }
    }

    public static void saveLootPoints() {
        try (Writer writer = new FileWriter(POINTS_FILE.toFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(LOOT_POINTS, writer);
        } catch (Exception e) {
            ProjectZ.LOGGER.error("Erro ao salvar os pontos de loot", e);
        }
    }

    public static List<ItemStack> rollLoot(String lootId) {
        List<ItemStack> rolled = new ArrayList<>();
        LootTableConfig table = LOOT_TABLES.get(lootId);
        if (table == null || table.items.isEmpty()) {
            return rolled;
        }

        Random random = new Random();
        int amount = table.min_items + random.nextInt(Math.max(1, table.max_items - table.min_items + 1));
        int totalWeight = table.items.stream().mapToInt(item -> item.weight).sum();

        if (totalWeight <= 0) return rolled;

        for (int i = 0; i < amount; i++) {
            int roll = random.nextInt(totalWeight);
            int current = 0;
            for (LootItem lootItem : table.items) {
                current += lootItem.weight;
                if (roll < current) {
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(lootItem.id));
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        int count = 1 + random.nextInt(Math.max(1, lootItem.count));
                        rolled.add(new ItemStack(item, count));
                    }
                    break;
                }
            }
        }
        return rolled;
    }

    public static LootTableConfig getLootConfig(String lootId) {
        return LOOT_TABLES.get(lootId);
    }

    public static List<LootPoint> getLootPoints() {
        return LOOT_POINTS;
    }

    public static void addLootPoint(LootPoint point) {
        LOOT_POINTS.add(point);
        saveLootPoints();
    }

    public static boolean removeLootPoint(int index) {
        if (index >= 0 && index < LOOT_POINTS.size()) {
            LOOT_POINTS.remove(index);
            saveLootPoints();
            return true;
        }
        return false;
    }

    public static Set<String> getAllUniqueModels() {
        Set<String> models = new HashSet<>();
        for (LootTableConfig table : LOOT_TABLES.values()) {
            if (table.model != null && !table.model.isEmpty()) {
                models.add(table.model);
            }
        }
        return models;
    }

    public static void tickLootPoints(ServerLevel level) {
        long gameTime = level.getGameTime();
        String currentDim = level.dimension().location().toString();

        for (LootPoint point : LOOT_POINTS) {
            if (!point.dimension.equals(currentDim)) continue;

            if (gameTime - point.lastSpawnAttempt < config.globalLootSpawnInterval) {
                continue;
            }
            point.lastSpawnAttempt = gameTime;

            if (point.activeEntityUuid != null) {
                Entity active = level.getEntity(point.activeEntityUuid);
                if (active != null && active.isAlive()) {
                    continue;
                } else {
                    point.activeEntityUuid = null;
                }
            }

            Player nearbyPlayer = level.getNearestPlayer(point.pos.getX(), point.pos.getY(), point.pos.getZ(), 64, false);
            if (nearbyPlayer == null) continue;

            if (level.random.nextDouble() > config.globalLootSpawnChance) {
                continue;
            }

            double angle = level.random.nextDouble() * 2 * Math.PI;
            double radius = level.random.nextDouble() * point.range;
            double spawnX = point.pos.getX() + Math.cos(angle) * radius + 0.5;
            double spawnZ = point.pos.getZ() + Math.sin(angle) * radius + 0.5;

            int startY = point.pos.getY() + 10;
            int surfaceY = -1;
            for (int y = startY; y >= point.pos.getY() - 10; y--) {
                BlockPos checkPos = BlockPos.containing(spawnX, y, spawnZ);
                if (level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.below()).isSolid()) {
                    surfaceY = y;
                    break;
                }
            }

            if (surfaceY != -1) {
                LootEntity lootEntity = new LootEntity(ProjectZ.LOOT_ENTITY.get(), level);
                lootEntity.setLootId(point.lootId);
                lootEntity.setOriginPointId(point.id);
                lootEntity.setPos(spawnX, surfaceY, spawnZ);
                level.addFreshEntity(lootEntity);
                point.activeEntityUuid = lootEntity.getUUID();
            }
        }
    }
}
