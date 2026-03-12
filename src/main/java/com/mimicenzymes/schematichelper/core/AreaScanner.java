package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

public class AreaScanner {
    // 真正的 AreaScanner 代码在这里
    public static void executeScan(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        int radius = Configs.FILL_RADIUS.getIntegerValue();

        List<BlockPos> targets = SpatialContainerIndex.queryRadius(px, py, pz, radius);

        for (BlockPos pos : targets) {
            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world);
            if (items != null && !items.isEmpty()) {
                AutoFillerStateMachine.getInstance().addTask(pos, items);
            }
        }
    }
}