package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.Map;

public class AutoSyncManager {
    private static int tickCounter = 0;

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || !Configs.ENABLE_DATA_SYNC.getBooleanValue()) return;

        tickCounter++;
        // 🚀 放慢巡检频率，每 1 秒查一次足矣，防止疯狂读 NBT 卡顿
        if (tickCounter >= 20) {
            tickCounter = 0;
            List<BlockPos> completed = CompletedContainers.getCompletedList();

            for (BlockPos pos : completed) {
                // 1. 🚀 刚填完的箱子，给 5 秒“免死金牌”（需要上一个回答中的 CompletedContainers 更新配合）
                if (CompletedContainers.isInGracePeriod(pos)) continue;

                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null) continue;

                NbtCompound nbt = be.createNbt(mc.world.getRegistryManager());

                // 2. 🚀 防单机致盲修复！
                // 如果 NBT 里根本没有 Items 标签，说明原版单机客户端没有收到服务端的库存更新包！
                // 此时它是一个“假空箱子”，绝对不能执行 remove！
                if (!nbt.contains("Items")) continue;

                Map<Integer, ItemStack> required = SchematicContainerReader.getRequiredItems(pos, mc.world);
                // 只有当有 Items 标签，且确实发现东西不够时，才真正撤销高亮
                if (!RealContainerReader.isSatisfied(mc, pos, required)) {
                    CompletedContainers.remove(pos);
                }
            }
        }
    }
}