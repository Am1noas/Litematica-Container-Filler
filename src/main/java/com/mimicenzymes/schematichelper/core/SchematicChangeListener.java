package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.MinecraftClient;

public class SchematicChangeListener {
    private static Object lastSchematic = null;

    public static void tick(MinecraftClient mc) {
        Object current = SchematicWorldHandler.getSchematicWorld();

        if (current != lastSchematic) {
            lastSchematic = current;
            // 重新构建容器全局索引
            SchematicContainerIndex.rebuildIndex(mc);

            // 换投影时一键清空缓存
            RealContainerCache.clear();
            SchematicCache.clear();
        }
    }
}