package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

public class AreaScanner {
    public static void executeScan(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        int radius = Configs.FILL_RADIUS.getIntegerValue();

        List<BlockPos> targets = SpatialContainerIndex.queryRadius(px, py, pz, radius);

        for (BlockPos pos : targets) {
            // 🚀 1. 检查 Litematica 渲染层级
            if (Configs.SYNC_LITE_LAYER.getBooleanValue() && !DataManager.getRenderLayerRange().isPositionWithinRange(pos)) {
                continue;
            }

            // 🚀 2. 检查是否已经填满过了
            if (CompletedContainers.isCompleted(pos)) {
                continue;
            }

            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world);
            if (items != null && !items.isEmpty()) {
                AutoFillerStateMachine.getInstance().addTask(pos, items);
            } else {
                // 如果发现不需要物品（本来就是满的），直接标记为完成，下次不再扫描
                CompletedContainers.add(pos);
            }
        }
    }
}