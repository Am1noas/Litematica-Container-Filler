package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class SchematicContainerReader {

    public static Map<Integer, ItemStack> getRequiredItems(BlockPos worldPos, World realWorld) {
        BlockState state = realWorld.getBlockState(worldPos);
        RegistryWrapper.WrapperLookup registries = realWorld.getRegistryManager();

        if (state.getBlock() instanceof ChestBlock) {
            ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
            if (chestType != ChestType.SINGLE) {
                return getDoubleChestItems(worldPos, state, registries);
            }
        }
        return getSingleContainerItems(worldPos, registries);
    }

    private static Map<Integer, ItemStack> getDoubleChestItems(BlockPos pos, BlockState state, RegistryWrapper.WrapperLookup registries) {
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        Direction facing = state.get(ChestBlock.FACING);

        Direction otherHalfDir = (type == ChestType.LEFT) ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
        BlockPos otherPos = pos.offset(otherHalfDir);

        Map<Integer, ItemStack> thisHalfItems = getSingleContainerItems(pos, registries);
        Map<Integer, ItemStack> otherHalfItems = getSingleContainerItems(otherPos, registries);

        Map<Integer, ItemStack> mergedItems = new HashMap<>();

        if (type == ChestType.RIGHT) {
            mergedItems.putAll(thisHalfItems);
            otherHalfItems.forEach((slot, stack) -> mergedItems.put(slot + 27, stack));
        } else {
            thisHalfItems.forEach((slot, stack) -> mergedItems.put(slot + 27, stack));
            mergedItems.putAll(otherHalfItems);
        }

        return mergedItems;
    }

    private static Map<Integer, ItemStack> getSingleContainerItems(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
        Map<Integer, ItemStack> items = new HashMap<>();

        var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return items;

        BlockEntity blockEntity = schematicWorld.getBlockEntity(pos);
        if (blockEntity == null) return items;

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
}