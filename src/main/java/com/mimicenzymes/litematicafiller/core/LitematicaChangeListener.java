package com.mimicenzymes.litematicafiller.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.MinecraftClient;

public class LitematicaChangeListener {
    private static Object lastSchematic = null;

    public static void tick(MinecraftClient mc) {
        Object current = SchematicWorldHandler.getSchematicWorld();

        if (current != lastSchematic) {
            lastSchematic = current;
            // 重新构建容器全局索引
            LitematicaContainerIndex.rebuildIndex(mc);

            // 换投影时一键清空缓存
            RealContainerCache.clear();
            LitematicaCache.clear();
        }
    }
}