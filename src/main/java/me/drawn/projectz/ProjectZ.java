package me.drawn.projectz;

import com.mojang.logging.LogUtils;
import me.drawn.projectz.client.ClientPlaneManager;
import me.drawn.projectz.command.ModCommands;
import me.drawn.projectz.config.LootConfigManager;
import me.drawn.projectz.entity.AirdropCrateEntity;
import me.drawn.projectz.entity.LootEntity;
import me.drawn.projectz.network.AirdropVisualPayload;
import me.drawn.projectz.registry.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod(ProjectZ.MODID)
public class ProjectZ {
    public static final String MODID = "projectz";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static int HORDE_CAP = 800;
    public static int HORDE_DAY_INTERVAL = 5;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<Item, Item> AIRPLANE_ITEM = ITEMS.register("airplane", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AIRDROP_ITEM = ITEMS.register("airdrop", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CRATE = ITEMS.register("small_crate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRATE_CAR = ITEMS.register("car_crate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRATE_LARGE = ITEMS.register("crate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRATE_WEAPON = ITEMS.register("weapon_crate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRATE_AMMO = ITEMS.register("ammo_crate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRATE_MEDIC = ITEMS.register("medic_crate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<EntityType<?>, EntityType<AirdropCrateEntity>> AIRDROP_CRATE_ENTITY = ENTITIES.register("airdrop_crate",
            () -> EntityType.Builder.<AirdropCrateEntity>of(AirdropCrateEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F)
                    .build("airdrop_crate")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<LootEntity>> LOOT_ENTITY = ENTITIES.register("loot",
            () -> EntityType.Builder.<LootEntity>of(LootEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .build("loot")
    );

    public static final TickScheduler scheduler = new TickScheduler();

    public ProjectZ(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(new ServerEvents());

        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        ModSoundEvents.SOUND_EVENTS.register(modBus);

        LootConfigManager.init();

        modBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(scheduler);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                AirdropVisualPayload.TYPE,
                AirdropVisualPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPlaneManager.addPlane(payload))
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LootConfigManager.tickLootPoints(serverLevel);
        }
    }

    public static class TickScheduler {
        private final List<ScheduledTask> tasks = new ArrayList<>();
        private int tickCount = 0;

        private record ScheduledTask(int runTick, Runnable action) {}

        public synchronized void schedule(int delayTicks, Runnable action) {
            tasks.add(new ScheduledTask(tickCount + delayTicks, action));
        }

        @SubscribeEvent
        public synchronized void onServerTick(ServerTickEvent.Post event) {
            tickCount++;
            Iterator<ScheduledTask> it = tasks.iterator();
            while (it.hasNext()) {
                ScheduledTask task = it.next();
                if (tickCount >= task.runTick) {
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        ProjectZ.LOGGER.error("Erro ao executar tarefa agendada", e);
                    }
                    it.remove();
                }
            }
        }
    }

    private static final long DAMAGE_INTERVAL = 20L; // maybe change to half a sec later

    public static class ServerEvents {
        // rain events
        @SubscribeEvent
        public void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;

            long gameTime = player.level().getGameTime();
            if (gameTime % DAMAGE_INTERVAL != 0) return;

            BlockPos pos = player.blockPosition();

            boolean raining = player.level().isRainingAt(pos)
                    || player.level().isRainingAt(
                    BlockPos.containing(pos.getX(), player.getBoundingBox().maxY, pos.getZ())
            );

            if (!raining) return;
            if (player.isPassenger()) return;

            ItemStack helmet = player.getInventory().getArmor(3);

            if (!helmet.isEmpty() && helmet.isDamageableItem()) {
                if(Math.random() > 0.5)
                    helmet.hurtAndBreak(1, player, EquipmentSlot.HEAD);

                if (helmet.isEmpty()) {
                    player.hurt(player.damageSources().generic(), 1.0F);
                }
            } else {
                player.hurt(player.damageSources().generic(), 1.0F);
            }
        }
    }
}
