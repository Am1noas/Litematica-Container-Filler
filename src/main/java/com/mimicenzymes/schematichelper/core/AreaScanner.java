package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;
import java.util.Map;

public class AreaScanner {
    public static void executeScan(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        var schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return;

        BlockPos center = mc.player.getBlockPos();
        int r = Configs.FILL_RADIUS.getIntegerValue();
        boolean syncLayer = Configs.SYNC_LITE_LAYER.getBooleanValue();
        int count = 0;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);

                    if (syncLayer && !fi.dy.masa.litematica.data.DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;

                    BlockState state = schematicWorld.getBlockState(pos);
                    if (state.isAir() || !state.hasBlockEntity()) continue;

                    // 🚀 核心补丁：坐标归一化，终结大箱子开两次的问题！
                    // 不管扫到大箱子的哪一边，统一将坐标篡改为右半边 (ChestType.RIGHT)
                    if (state.getBlock() instanceof ChestBlock) {
                        ChestType type = state.get(ChestBlock.CHEST_TYPE);
                        if (type == ChestType.LEFT) {
                            Direction facing = state.get(ChestBlock.FACING);
                            pos = pos.offset(facing.rotateYClockwise());
                        }
                    }

                    Map<Integer, ItemStack> required = SchematicContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());
                    if (required == null || required.isEmpty() || RealContainerCache.isSatisfied(pos, required)) continue;

                    // 经过归一化后，大箱子的第二单会在这里被状态机内部的去重逻辑直接拦截！
                    AutoFillerStateMachine.getInstance().addTask(pos, required);
                    count++;
                }
            }
        }

        if (count > 0) {
            mc.player.sendMessage(Text.translatable("schematic_container_helper.message.scan_start", count), true);
        }
    }
}