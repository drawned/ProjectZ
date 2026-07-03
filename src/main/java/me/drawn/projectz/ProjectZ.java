package me.drawn.projectz;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ProjectZ.MODID)
public class ProjectZ {
    public static final String MODID = "projectz";

    public static int HORDE_CAP = 800;
    public static int HORDE_DAY_INTERVAL = 5;

    public ProjectZ(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(new ServerEvents());
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
