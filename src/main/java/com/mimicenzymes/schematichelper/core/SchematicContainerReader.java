package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;

public class SchematicContainerReader {

    public static Map<Integer, ItemStack> getRequiredItems(BlockPos worldPos, RegistryWrapper.WrapperLookup registries) {
        Map<Integer, ItemStack> items = new HashMap<>();
        var schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return items;

        BlockState state = schematicWorld.getBlockState(worldPos);

        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                Direction facing = state.get(ChestBlock.FACING);
                Direction otherHalfDir = (type == ChestType.LEFT) ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();

                // 🚀 核心大修复：强制统一大箱子的合并顺序！
                // 不管传入的是哪一半，永远让 ChestType.RIGHT 作为上半部分(0-26)，ChestType.LEFT 作为下半部分(27-53)
                // 这完美匹配了原版大箱子 GUI 的底层逻辑
                BlockPos rightPos = (type == ChestType.RIGHT) ? worldPos : worldPos.offset(otherHalfDir);
                BlockPos leftPos = (type == ChestType.LEFT) ? worldPos : worldPos.offset(otherHalfDir);

                Map<Integer, ItemStack> rightHalf = getSingleContainerItems(schematicWorld, rightPos, registries);
                Map<Integer, ItemStack> leftHalf = getSingleContainerItems(schematicWorld, leftPos, registries);

                items.putAll(rightHalf);
                leftHalf.forEach((slot, stack) -> items.put(slot + 27, stack));

                return items;
            }
        }
        return getSingleContainerItems(schematicWorld, worldPos, registries);
    }

    private static Map<Integer, ItemStack> getSingleContainerItems(net.minecraft.world.World schematicWorld, BlockPos pos, RegistryWrapper.WrapperLookup registries) {
        Map<Integer, ItemStack> items = new HashMap<>();
        BlockEntity blockEntity = schematicWorld.getBlockEntity(pos);
        if (blockEntity == null) return items;

        NbtCompound nbt = blockEntity.createNbt(registries);
        if (nbt != null && nbt.contains("Items")) {
            items.putAll(RealContainerCache.parseNbtInventory(nbt, registries));
        }
        return items;
    }
}