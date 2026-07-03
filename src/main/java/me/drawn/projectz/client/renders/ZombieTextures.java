package me.drawn.projectz.client.renders;

import net.minecraft.resources.ResourceLocation;

public class ZombieTextures {
    private static final int MAX = 1631;

    public static final ResourceLocation[] TEXTURES = newTextures();

    private static ResourceLocation[] newTextures() {
        ResourceLocation[] arr = new ResourceLocation[MAX];
        for (int i = 0; i < MAX; i++) {
            arr[i] = ResourceLocation.parse("minecraft:optifine/random/entity/zombie/zombie" + i + ".png");
        }
        return arr;
    }
}
