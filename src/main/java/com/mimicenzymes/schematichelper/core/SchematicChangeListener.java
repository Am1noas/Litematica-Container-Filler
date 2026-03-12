package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.MinecraftClient;

public class SchematicChangeListener {
    private static Object lastSchematic = null;

    public static void tick(MinecraftClient mc) {
        // 获取当前投影世界实例
        Object current = SchematicWorldHandler.getSchematicWorld();

        // 检查投影是否发生了切换（比如换了蓝图，或者关闭了投影）
        if (current != lastSchematic) {
            lastSchematic = current;

            // 1. 重新构建容器坐标索引（KDTree/列表）
            SchematicContainerIndex.rebuildIndex(mc);

            // 2. 核心：清空已完成记录
            // 只有清空了这里，新蓝图里的箱子才会重新亮起高亮红框
            CompletedContainers.clear();

            // 3. 可选：清空扫描缓存
            SchematicCache.clear();
        }
    }
}