package com.mimicenzymes.litematicafiller.core;

import com.mimicenzymes.litematicafiller.config.Configs;
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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RealContainerCache {
    private static final Map<BlockPos, Map<Integer, ItemStack>> CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Set<Integer>> LOCK_CACHE = new ConcurrentHashMap<>();
    private static BlockPos lastLookedPos = null;
    private static int tickCounter = 0;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        if (client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult bhr) {
            lastLookedPos = bhr.getBlockPos();
        }

        if (client.currentScreen instanceof HandledScreen<?> screen) {
            updateFromScreen(client, screen);
        }

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

    public static void updateFromScreen(MinecraftClient client, HandledScreen<?> screen) {
        BlockPos pos = AutoFillerStateMachine.getInstance().getCurrentTaskPos();
        if (pos == null) pos = lastLookedPos;
        if (pos == null) return;

        Map<Integer, ItemStack> items = new HashMap<>();
        ScreenHandler handler = screen.getScreenHandler();

        for (Slot slot : handler.slots) {
            if (slot.inventory != null && slot.inventory != client.player.getInventory()) {
                if (handler instanceof net.minecraft.screen.CrafterScreenHandler && slot.getIndex() == 9) {
                    continue;
                }
                if (!slot.getStack().isEmpty()) items.put(slot.getIndex(), slot.getStack().copy());
            }
        }
        CACHE.put(pos.toImmutable(), items);

        if (handler instanceof net.minecraft.screen.CrafterScreenHandler crafterHandler) {
            Set<Integer> locks = new HashSet<>();
            for (int i = 0; i < 9; i++) {
                if (crafterHandler.isSlotDisabled(i)) locks.add(i);
            }
            LOCK_CACHE.put(pos.toImmutable(), locks);
        }

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

    public static Set<Integer> getCachedLocks(BlockPos pos) { return LOCK_CACHE.get(pos); }

    public static boolean isSatisfied(BlockPos pos, Map<Integer, ItemStack> required) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        BlockState state = client.world.getBlockState(pos);
        boolean isCrafter = state.getBlock() instanceof net.minecraft.block.CrafterBlock;
        if (isCrafter && LitematicaContainerReader.doesCrafterNeedLocking(pos, client)) {
            return false;
        }
        if (checkMapStrict(CACHE.get(pos), required, isCrafter)) return true;
        if (client.isInSingleplayer() && client.getServer() != null) {
            ServerWorld serverWorld = client.getServer().getWorld(client.world.getRegistryKey());
            if (serverWorld != null) {
                Map<Integer, ItemStack> spItems = getSingleplayerRealItems(pos, serverWorld);
                if (checkMapStrict(spItems, required, isCrafter)) {
                    CACHE.put(pos.toImmutable(), spItems);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkMapStrict(Map<Integer, ItemStack> realItems, Map<Integer, ItemStack> required, boolean isCrafter) {
        if (realItems == null) return false;
        //合成器只看0-8槽，普通容器看 0-53
        int maxSlot = isCrafter ? 9 : 54;

        for (int i = 0; i < maxSlot; i++) {
            ItemStack real = realItems.getOrDefault(i, ItemStack.EMPTY);
            ItemStack req = (required != null) ? required.getOrDefault(i, ItemStack.EMPTY) : ItemStack.EMPTY;
            if (real.isEmpty() && req.isEmpty()) continue;
            if (real.isEmpty() != req.isEmpty() || !ItemMatcher.isSameItem(real, req) || real.getCount() != req.getCount()) {
                return false;
            }
        }
        return true;
    }

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

    //暴力NBT解析
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

    public static void clear() {
        CACHE.clear();
        LOCK_CACHE.clear();
    }
}