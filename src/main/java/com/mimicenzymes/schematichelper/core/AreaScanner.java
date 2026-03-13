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
import java.util.HashMap;
import java.util.Map;

public class AreaScanner {
    private static final Map<BlockPos, Long> ATTEMPT_COOLDOWNS = new HashMap<>();

    public static void executeScan(MinecraftClient mc, boolean isSilentPrinter) {
        if (mc.player == null || mc.world == null) return;

        var schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            if (!isSilentPrinter) mc.player.sendMessage(Text.translatable("schematic_container_helper.message.no_schematic_world"), true);
            return;
        }

        BlockPos center = mc.player.getBlockPos();
        int r = Configs.FILL_RADIUS.getIntegerValue();
        boolean syncLayer = Configs.SYNC_LITE_LAYER.getBooleanValue();
        int count = 0;
        long now = System.currentTimeMillis();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);

                    if (syncLayer && !fi.dy.masa.litematica.data.DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;

                    BlockState state = schematicWorld.getBlockState(pos);
                    if (state.isAir() || !state.hasBlockEntity()) continue;

                    if (state.getBlock() instanceof ChestBlock) {
                        ChestType type = state.get(ChestBlock.CHEST_TYPE);
                        if (type == ChestType.LEFT) {
                            Direction facing = state.get(ChestBlock.FACING);
                            pos = pos.offset(facing.rotateYClockwise());
                        }
                    }

                    if (isSilentPrinter && ATTEMPT_COOLDOWNS.containsKey(pos) && now - ATTEMPT_COOLDOWNS.get(pos) < 5000) {
                        continue;
                    }

                    Map<Integer, ItemStack> required = SchematicContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());
                    if (required == null || required.isEmpty() || RealContainerCache.isSatisfied(pos, required)) continue;

                    AutoFillerStateMachine.getInstance().addTask(pos, required);
                    ATTEMPT_COOLDOWNS.put(pos, now);
                    count++;
                }
            }
        }

        if (!isSilentPrinter) {
            if (count > 0) {
                mc.player.sendMessage(Text.translatable("schematic_container_helper.message.scan_start", count), true);
            } else {
                mc.player.sendMessage(Text.translatable("schematic_container_helper.message.no_requirements"), true);
            }
        }
    }
}