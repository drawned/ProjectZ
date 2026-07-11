package me.drawn.projectz.client;

import me.drawn.projectz.ProjectZ;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ProjectZ.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientGameEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientPlaneManager.onClientTick(event);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ClientPlaneManager.onRenderLevelStage(event);
    }
}
