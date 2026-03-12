package com.mimicenzymes.schematichelper.core;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;

public class RealContainerReader {
    public static Map<Integer, ItemStack> getRealItems(MinecraftClient mc, BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                return getDoubleChestItems(mc, pos, state);
            }
        }
        return getSingleContainerItems(mc, pos);
    }

    private static Map<Integer, ItemStack> getDoubleChestItems(MinecraftClient mc, BlockPos pos, BlockState state) {
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        Direction facing = state.get(ChestBlock.FACING);
        Direction otherHalfDir = (type == ChestType.LEFT) ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
        BlockPos otherPos = pos.offset(otherHalfDir);

        Map<Integer, ItemStack> thisHalf = getSingleContainerItems(mc, pos);
        Map<Integer, ItemStack> otherHalf = getSingleContainerItems(mc, otherPos);
        Map<Integer, ItemStack> merged = new HashMap<>();

        if (type == ChestType.RIGHT) {
            merged.putAll(thisHalf);
            otherHalf.forEach((slot, stack) -> merged.put(slot + 27, stack));
        } else {
            thisHalf.forEach((slot, stack) -> merged.put(slot + 27, stack));
            merged.putAll(otherHalf);
        }
        return merged;
    }

    private static Map<Integer, ItemStack> getSingleContainerItems(MinecraftClient mc, BlockPos pos) {
        Map<Integer, ItemStack> items = new HashMap<>();
        BlockEntity blockEntity = mc.world.getBlockEntity(pos);
        if (blockEntity == null) return items;

        RegistryWrapper.WrapperLookup registries = mc.world.getRegistryManager();
        // 🚀 核心修复：直接读取底层 NBT，无视客户端 Inventory 为空的假象
        NbtCompound nbt = blockEntity.createNbt(registries);

        if (nbt.contains("Items")) {
            NbtElement itemsElement = nbt.get("Items");
            if (itemsElement instanceof NbtList itemsList) {
                for (int i = 0; i < itemsList.size(); i++) {
                    NbtCompound itemTag = itemsList.getCompound(i).orElse(null);
                    if (itemTag != null) {
                        int slot = itemTag.getByte("Slot").get() & 255;
                        ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(registries.getOps(net.minecraft.nbt.NbtOps.INSTANCE), itemTag)
                                .resultOrPartial()
                                .orElse(ItemStack.EMPTY);

                        if (!stack.isEmpty()) {
                            items.put(slot, stack);
                        }
                    }
                }
            }
        }
        return items;
    }

    public static boolean isSatisfied(MinecraftClient mc, BlockPos pos, Map<Integer, ItemStack> required) {
        if (required == null || required.isEmpty()) return true;
        Map<Integer, ItemStack> realItems = getRealItems(mc, pos);

        for (Map.Entry<Integer, ItemStack> req : required.entrySet()) {
            ItemStack reqStack = req.getValue();
            ItemStack realStack = realItems.getOrDefault(req.getKey(), ItemStack.EMPTY);

            if (realStack.isEmpty() || !ItemMatcher.isSameItem(realStack, reqStack) || realStack.getCount() < reqStack.getCount()) {
                return false;
            }
        }
        return true;
    }
}