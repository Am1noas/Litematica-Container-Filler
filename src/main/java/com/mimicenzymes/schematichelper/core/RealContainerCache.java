package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RealContainerCache {
    private static final Map<BlockPos, Map<Integer, ItemStack>> CACHE = new ConcurrentHashMap<>();
    private static BlockPos lastLookedPos = null;
    private static int tickCounter = 0;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        // 记录鼠标看的位置兜底
        if (client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult bhr) {
            lastLookedPos = bhr.getBlockPos();
        }

        // 自然打开 GUI 时的监听
        if (client.currentScreen instanceof HandledScreen<?> screen) {
            updateFromScreen(client, screen);
        }

        // Servux 多人数据包兜底
        if (Configs.ENABLE_DATA_SYNC.getBooleanValue() && !client.isInSingleplayer()) {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
                if (schematicWorld == null) return;

                int r = Configs.RENDER_RADIUS.getIntegerValue();
                BlockPos center = client.player.getBlockPos();

                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = center.add(x, y, z);
                            if (!schematicWorld.getBlockState(pos).hasBlockEntity()) continue;

                            BlockEntity be = client.world.getBlockEntity(pos);
                            if (be == null) continue;

                            NbtCompound nbt = be.createNbt(client.world.getRegistryManager());
                            if (nbt != null && nbt.contains("Items")) {
                                CACHE.put(pos.toImmutable(), parseNbtInventory(nbt, client.world.getRegistryManager()));
                            }
                        }
                    }
                }
            }
        }
    }

    // 🚀 给机器人调用的“快照抢拍”方法，消除最后一件物品放进去来不及记录的 Bug
    public static void updateFromScreen(MinecraftClient client, HandledScreen<?> screen) {
        BlockPos pos = AutoFillerStateMachine.getInstance().getCurrentTaskPos();
        if (pos == null) pos = lastLookedPos;
        if (pos == null) return;

        Map<Integer, ItemStack> items = new HashMap<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory != null && slot.inventory != client.player.getInventory()) {
                if (!slot.getStack().isEmpty()) items.put(slot.getIndex(), slot.getStack().copy());
            }
        }
        CACHE.put(pos.toImmutable(), items);

        BlockState state = client.world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                Direction facing = state.get(ChestBlock.FACING);
                Direction otherHalfDir = (type == ChestType.LEFT) ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
                CACHE.put(pos.offset(otherHalfDir).toImmutable(), items);
            }
        }
    }

    // 🚀 单机模式上帝视角，直接调用底层 Inventory 读取
    private static Map<Integer, ItemStack> getSingleplayerRealItems(BlockPos pos, ServerWorld serverWorld) {
        Map<Integer, ItemStack> items = new HashMap<>();
        BlockState state = serverWorld.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock) {
            Inventory inv = ChestBlock.getInventory((ChestBlock) state.getBlock(), state, serverWorld, pos, true);
            if (inv != null) {
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (stack != null && !stack.isEmpty()) {
                        items.put(i, stack.copy());
                    }
                }
                return items;
            }
        }
        return getHalfChestItems(pos, serverWorld);
    }

    private static Map<Integer, ItemStack> getHalfChestItems(BlockPos pos, ServerWorld serverWorld) {
        Map<Integer, ItemStack> items = new HashMap<>();
        BlockEntity be = serverWorld.getBlockEntity(pos);
        if (be instanceof Inventory inv) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack != null && !stack.isEmpty()) {
                    items.put(i, stack.copy());
                }
            }
            return items;
        }
        if (be != null) {
            NbtCompound nbt = be.createNbt(serverWorld.getRegistryManager());
            if (nbt != null && nbt.contains("Items")) {
                items = parseNbtInventory(nbt, serverWorld.getRegistryManager());
            }
        }
        return items;
    }

    // 无视版本报错的暴力 NBT 解析兜底
    public static Map<Integer, ItemStack> parseNbtInventory(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        Map<Integer, ItemStack> items = new HashMap<>();
        NbtElement itemsElem = nbt.get("Items");
        if (itemsElem instanceof NbtList list) {
            for (int i = 0; i < list.size(); i++) {
                NbtElement itemElem = list.get(i);
                if (itemElem instanceof NbtCompound itemTag) {
                    int slot = 0;
                    if (itemTag.contains("Slot")) {
                        try { slot = Integer.parseInt(itemTag.get("Slot").toString().replaceAll("[^0-9]", "")) & 255; } catch (Exception ignored) {}
                    }

                    ItemStack stack = ItemStack.EMPTY;
                    try {
                        stack = ItemStack.OPTIONAL_CODEC.parse(registries.getOps(NbtOps.INSTANCE), itemTag).resultOrPartial().orElse(ItemStack.EMPTY);
                    } catch (Exception ignored) {}

                    if (stack.isEmpty() && itemTag.contains("id")) {
                        String idStr = itemTag.get("id").toString().replace("\"", "");
                        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(idStr);
                        if (id != null) {
                            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(id);
                            if (item != null && item != net.minecraft.item.Items.AIR) {
                                int count = 1;
                                try {
                                    if (itemTag.contains("Count")) count = Integer.parseInt(itemTag.get("Count").toString().replaceAll("[^0-9]", ""));
                                    else if (itemTag.contains("count")) count = Integer.parseInt(itemTag.get("count").toString().replaceAll("[^0-9]", ""));
                                } catch (Exception ignored) {}
                                stack = new ItemStack(item, count);
                            }
                        }
                    }

                    if (!stack.isEmpty()) {
                        items.put(slot, stack);
                    }
                }
            }
        }
        return items;
    }

    // 严格槽位比对校验
    public static boolean isSatisfied(BlockPos pos, Map<Integer, ItemStack> required) {
        if (required == null || required.isEmpty()) return true;

        // 1. 先查零延迟的 GUI 抢拍缓存
        if (checkMap(CACHE.get(pos), required)) return true;

        MinecraftClient client = MinecraftClient.getInstance();

        // 2. 如果没对上，再去读单机内存（防误判）
        if (client.isInSingleplayer() && client.getServer() != null && client.world != null) {
            ServerWorld serverWorld = client.getServer().getWorld(client.world.getRegistryKey());
            if (serverWorld != null) {
                Map<Integer, ItemStack> spItems = getSingleplayerRealItems(pos, serverWorld);
                if (checkMap(spItems, required)) {
                    CACHE.put(pos.toImmutable(), spItems);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkMap(Map<Integer, ItemStack> realItems, Map<Integer, ItemStack> required) {
        if (realItems == null) return false;
        for (Map.Entry<Integer, ItemStack> req : required.entrySet()) {
            ItemStack realStack = realItems.getOrDefault(req.getKey(), ItemStack.EMPTY);
            // 强迫症专属：格格对齐、物品对应、数量严谨
            if (realStack.isEmpty() || !realStack.isOf(req.getValue().getItem()) || realStack.getCount() < req.getValue().getCount()) {
                return false;
            }
        }
        return true;
    }

    public static void clear() { CACHE.clear(); }
}