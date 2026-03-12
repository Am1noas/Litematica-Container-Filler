package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
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
        int count = 0; // 记录找到的缺货容器数量

        for (BlockPos pos : targets) {
            if (Configs.SYNC_LITE_LAYER.getBooleanValue() && !fi.dy.masa.litematica.data.DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            if (CompletedContainers.isCompleted(pos)) continue;

            Map<Integer, ItemStack> required = SchematicContainerReader.getRequiredItems(pos, mc.world);

            if (RealContainerReader.isSatisfied(mc, pos, required)) {
                CompletedContainers.add(pos);
                continue;
            }

            if (required != null && !required.isEmpty()) {
                AutoFillerStateMachine.getInstance().addTask(pos, required);
                count++;
            }
        }

        //扫描反馈
        if (count > 0) {
            mc.player.sendMessage(Text.translatable("schematic_container_helper.message.scan_start", count), true);
        }
    }
}