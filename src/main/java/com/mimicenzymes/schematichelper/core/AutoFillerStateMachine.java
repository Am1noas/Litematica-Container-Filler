package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.Configs;
import com.mimicenzymes.schematichelper.dependency.DependencyChecker;
import com.mimicenzymes.schematichelper.dependency.DummyExtractor;
import com.mimicenzymes.schematichelper.dependency.IShulkerExtractor;
import com.mimicenzymes.schematichelper.dependency.QuickShulkerWrapper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoFillerStateMachine {

    public static class FillTask {
        public final BlockPos targetPos;
        public final Map<Integer, ItemStack> requiredItems;
        public FillTask(BlockPos targetPos, Map<Integer, ItemStack> requiredItems) {
            this.targetPos = targetPos;
            this.requiredItems = requiredItems;
        }
    }

    private static final AutoFillerStateMachine INSTANCE = new AutoFillerStateMachine();
    public static AutoFillerStateMachine getInstance() { return INSTANCE; }

    private final Queue<FillTask> taskQueue = new ConcurrentLinkedQueue<>();
    private FillTask currentTask = null;
    private SlotMapper currentMapper = null;
    private final Deque<Runnable> actionQueue = new LinkedList<>();
    private int actionWaitTicks = 0;
    private int watchdogTimer = 0;
    private final IShulkerExtractor shulkerExtractor;
    private boolean silentlyExtracting = false;

    private int lastOpenedShulkerSlot = -1;
    private final Set<Item> borrowedItems = new HashSet<>();

    private AutoFillerStateMachine() {
        this.shulkerExtractor = DependencyChecker.HAS_QUICK_SHULKER ? new QuickShulkerWrapper() : new DummyExtractor();
    }

    // 🚀 核心修复 1：拦截积压任务，拒绝重复塞单！
    public void addTask(BlockPos pos, Map<Integer, ItemStack> requiredItems) {
        // 如果当前正在处理这个箱子，无视新订单
        if (currentTask != null && currentTask.targetPos.equals(pos)) return;

        // 如果排队列表里已经有这个箱子了，无视新订单
        for (FillTask t : taskQueue) {
            if (t.targetPos.equals(pos)) return;
        }

        taskQueue.add(new FillTask(pos, requiredItems));
    }

    public boolean isSilentlyExtracting() { return silentlyExtracting; }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) { reset(); return; }

        if (currentTask != null) {
            watchdogTimer++;
            if (watchdogTimer > 100) {
                sendFeedback(client, "§c操作超时，自动重置状态", true);
                reset();
                return;
            }
        }

        if (actionWaitTicks > 0) { actionWaitTicks--; watchdogTimer = 0; return; }
        if (!actionQueue.isEmpty()) { actionQueue.poll().run(); watchdogTimer = 0; return; }

        if (currentTask == null) {
            if (!taskQueue.isEmpty()) {
                currentTask = taskQueue.poll();
                openTargetContainer(client, currentTask.targetPos);
            }
            return;
        }

        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            if (!silentlyExtracting && actionQueue.isEmpty()) {
                if (currentMapper == null) {
                    currentMapper = new SlotMapper(handledScreen.getScreenHandler(), client.player.getInventory());
                }
                executeBurstFill(client, handledScreen.getScreenHandler());
            }
        }
    }

    private void executeBurstFill(MinecraftClient client, ScreenHandler handler) {
        int syncId = handler.syncId;
        int delay = Configs.FILL_DELAY.getIntegerValue();

        if (!handler.getCursorStack().isEmpty()) {
            if (!tryPlaceCursorItem(client, handler)) {
                sendFeedback(client, "§c鼠标卡住，请手动清理物品栏", true);
                reset(); return;
            }
            if (delay > 0) { actionWaitTicks = delay; return; }
        }

        boolean allMatched = true;
        int containerSize = handler.slots.size() - 36;
        if (containerSize <= 0) { reset(); return; }

        for (int containerSlot = 0; containerSlot < containerSize; containerSlot++) {
            int uiSlot = currentMapper.getUiSlotForContainer(containerSlot);
            if (uiSlot == -1 || uiSlot >= handler.slots.size()) continue;

            ItemStack required = currentTask.requiredItems.getOrDefault(containerSlot, ItemStack.EMPTY);
            ItemStack current = handler.slots.get(uiSlot).getStack();

            if (current.isEmpty() && required.isEmpty()) continue;

            if (!current.isEmpty() && (!ItemMatcher.isSameItem(current, required) || current.getCount() > required.getCount())) {
                allMatched = false;
                if (countEmptyPlayerSlots(client) == 0) {
                    int shulkerSlot = findShulkerWithEmptySlot(client);
                    if (shulkerSlot != -1) {
                        int trashSlot = findTrashSlotSafe(client, required.isEmpty() ? current : required, shulkerSlot);
                        if (trashSlot != -1) {
                            queueFreeUpInventorySpace(client, shulkerSlot, trashSlot);
                            return;
                        }
                    }
                    reset(); return;
                }
                client.interactionManager.clickSlot(syncId, uiSlot, 0, SlotActionType.QUICK_MOVE, client.player);
                if (delay > 0) { actionWaitTicks = delay; return; }
                continue;
            }

            if (current.getCount() < required.getCount()) {
                allMatched = false;
                int playerSlot = findItemInPlayerInv(client, required);
                if (playerSlot != -1) {
                    fillFromPlayerInv(client, syncId, playerSlot, uiSlot, required.getCount() - current.getCount());
                    if (delay > 0) { actionWaitTicks = delay; return; }
                } else {
                    ShulkerSearchResult result = findItemInShulkers(client, required);
                    if (result != null) {
                        queueSmartShulkerExtraction(client, result.shulkerPlayerSlot, result.itemSlotInShulker, required);
                        return;
                    }
                    if (current.isEmpty()) currentTask.requiredItems.remove(containerSlot);
                    else currentTask.requiredItems.put(containerSlot, current.copy());
                }
            }
        }

        if (allMatched) {
            // 🚀 核心修复 2：在机器人关上 GUI 的前一微秒，强制执行一次全局抓拍更新缓存！
            // 彻底杜绝关箱子太快导致缓存没跟上的情况。
            if (client.currentScreen instanceof HandledScreen<?> hs) {
                RealContainerCache.updateFromScreen(client, hs);
            }

            if (lastOpenedShulkerSlot != -1 && !borrowedItems.isEmpty()) {
                queueReturnBorrowedItems(client, lastOpenedShulkerSlot);
            } else {
                client.player.closeHandledScreen();
                reset();
            }
        }
    }

    private void sendFeedback(MinecraftClient client, String key, boolean ignored) {
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(key), true);
        }
    }

    // ... [中间的潜影盒和物品挪动逻辑保持不变] ...

    private void queueSmartShulkerExtraction(MinecraftClient client, int shulkerSlot, int itemInShulker, ItemStack targetItem) {
        List<Integer> emptyUiSlots = new ArrayList<>();
        for (int i = 9; i < 36; i++) if (client.player.getInventory().getStack(i).isEmpty()) emptyUiSlots.add(i + 18);
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isEmpty()) emptyUiSlots.add(i + 54);

        int emptySlotCount = emptyUiSlots.size();
        borrowedItems.add(targetItem.getItem());
        lastOpenedShulkerSlot = shulkerSlot;

        int totalNeeded = 0;
        for (ItemStack req : currentTask.requiredItems.values()) {
            if (ItemMatcher.isSameItem(req, targetItem)) totalNeeded += req.getCount();
        }

        int maxCapacity = emptySlotCount * targetItem.getMaxCount();
        final int targetExtraction = Math.min(totalNeeded, maxCapacity);

        actionQueue.add(() -> { client.player.closeHandledScreen(); silentlyExtracting = true; currentMapper = null; });
        actionQueue.add(() -> shulkerExtractor.requestOpenShulker(shulkerSlot));
        actionQueue.add(this::waitForUi);

        actionQueue.add(() -> {
            ScreenHandler h = client.player.currentScreenHandler;
            if (emptySlotCount > 0) {
                int remainingToExtract = targetExtraction;
                int usedEmptySlots = 0;
                for (int i = 0; i < 27; i++) {
                    if (remainingToExtract <= 0) break;
                    ItemStack s = h.slots.get(i).getStack();
                    if (ItemMatcher.isSameItem(s, targetItem)) {
                        int takeAmount = Math.min(s.getCount(), remainingToExtract);
                        if (takeAmount == s.getCount()) {
                            client.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
                            remainingToExtract -= takeAmount;
                            usedEmptySlots++;
                        } else {
                            if (usedEmptySlots < emptyUiSlots.size()) {
                                int targetUiEmptySlot = emptyUiSlots.get(usedEmptySlots);
                                client.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.PICKUP, client.player);
                                for (int j = 0; j < takeAmount; j++) {
                                    client.interactionManager.clickSlot(h.syncId, targetUiEmptySlot, 1, SlotActionType.PICKUP, client.player);
                                }
                                client.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.PICKUP, client.player);
                                remainingToExtract -= takeAmount;
                                usedEmptySlots++;
                            }
                        }
                    }
                }
            } else {
                int trashSlot = findTrashSlotSafe(client, targetItem, shulkerSlot);
                if (trashSlot != -1) {
                    int uiTrashSlot = (trashSlot < 9) ? trashSlot + 54 : trashSlot + 18;
                    client.interactionManager.clickSlot(h.syncId, itemInShulker, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(h.syncId, uiTrashSlot, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(h.syncId, itemInShulker, 0, SlotActionType.PICKUP, client.player);
                }
            }
        });

        actionQueue.add(() -> actionWaitTicks = 2);
        actionQueue.add(() -> {
            client.player.closeHandledScreen();
            silentlyExtracting = false;
            openTargetContainer(client, currentTask.targetPos);
        });
    }

    private void queueReturnBorrowedItems(MinecraftClient client, int shulkerSlot) {
        ItemStack box = client.player.getInventory().getStack(shulkerSlot);
        if (!(box.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock)) {
            client.player.closeHandledScreen();
            reset();
            return;
        }

        actionQueue.add(() -> { client.player.closeHandledScreen(); silentlyExtracting = true; currentMapper = null; });
        actionQueue.add(() -> shulkerExtractor.requestOpenShulker(shulkerSlot));
        actionQueue.add(this::waitForUi);

        actionQueue.add(() -> {
            ScreenHandler h = client.player.currentScreenHandler;
            for (int i = 27; i < h.slots.size(); i++) {
                ItemStack s = h.slots.get(i).getStack();
                if (!s.isEmpty() && borrowedItems.contains(s.getItem())) {
                    client.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
                }
            }
        });

        actionQueue.add(() -> actionWaitTicks = 3);
        actionQueue.add(() -> {
            client.player.closeHandledScreen();
            reset();
        });
    }

    private int findShulkerWithEmptySlot(MinecraftClient client) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = client.player.getInventory().getStack(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                ContainerComponent c = s.get(DataComponentTypes.CONTAINER);
                if (c == null || c.stream().toList().size() < 27) return i;
            }
        }
        return -1;
    }

    private void queueFreeUpInventorySpace(MinecraftClient client, int shulkerSlot, int trashSlot) {
        actionQueue.add(() -> { client.player.closeHandledScreen(); silentlyExtracting = true; currentMapper = null; });
        actionQueue.add(() -> shulkerExtractor.requestOpenShulker(shulkerSlot));
        actionQueue.add(this::waitForUi);
        actionQueue.add(() -> {
            ScreenHandler h = client.player.currentScreenHandler;
            int uiTrashSlot = (trashSlot < 9) ? trashSlot + 54 : trashSlot + 18;
            client.interactionManager.clickSlot(h.syncId, uiTrashSlot, 0, SlotActionType.QUICK_MOVE, client.player);
        });
        actionQueue.add(() -> actionWaitTicks = 2);
        actionQueue.add(() -> {
            client.player.closeHandledScreen();
            silentlyExtracting = false;
            openTargetContainer(client, currentTask.targetPos);
        });
    }

    private int findTrashSlotSafe(MinecraftClient client, ItemStack exclude, int excludeSlot) {
        for (int i = 9; i < 36; i++) {
            if (i == excludeSlot) continue;
            ItemStack s = client.player.getInventory().getStack(i);
            if (!s.isEmpty() && !ItemMatcher.isSameItem(s, exclude)) {
                boolean isNeededElsewhere = false;
                for (ItemStack req : currentTask.requiredItems.values()) {
                    if (ItemMatcher.isSameItem(s, req)) { isNeededElsewhere = true; break; }
                }
                if (!isNeededElsewhere) return i;
            }
        }
        return -1;
    }

    private boolean tryPlaceCursorItem(MinecraftClient client, ScreenHandler handler) {
        int containerSize = handler.slots.size() - 36;
        for (int i = 0; i < containerSize; i++) {
            ItemStack required = currentTask.requiredItems.getOrDefault(i, ItemStack.EMPTY);
            int uiSlot = currentMapper.getUiSlotForContainer(i);
            if (uiSlot != -1 && !required.isEmpty() && ItemMatcher.isSameItem(handler.getCursorStack(), required)) {
                ItemStack current = handler.slots.get(uiSlot).getStack();
                if (current.getCount() < required.getCount()) {
                    client.interactionManager.clickSlot(handler.syncId, uiSlot, 0, SlotActionType.PICKUP, client.player);
                    return true;
                }
            }
        }
        int empty = findEmptyPlayerSlot(client);
        if (empty != -1) {
            client.interactionManager.clickSlot(handler.syncId, currentMapper.getUiSlotForPlayer(empty), 0, SlotActionType.PICKUP, client.player);
            return true;
        }
        return false;
    }

    private void fillFromPlayerInv(MinecraftClient client, int syncId, int playerSlot, int containerSlot, int needed) {
        int uiPlayerSlot = currentMapper.getUiSlotForPlayer(playerSlot);
        ItemStack sourceStack = client.player.getInventory().getStack(playerSlot);
        int countInSlot = sourceStack.getCount();
        int amountToMove = Math.min(needed, countInSlot);

        if (amountToMove == countInSlot) {
            client.interactionManager.clickSlot(syncId, uiPlayerSlot, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(syncId, containerSlot, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(syncId, uiPlayerSlot, 0, SlotActionType.PICKUP, client.player);
        } else {
            client.interactionManager.clickSlot(syncId, uiPlayerSlot, 0, SlotActionType.PICKUP, client.player);
            for (int i = 0; i < amountToMove; i++) {
                client.interactionManager.clickSlot(syncId, containerSlot, 1, SlotActionType.PICKUP, client.player);
            }
            client.interactionManager.clickSlot(syncId, uiPlayerSlot, 0, SlotActionType.PICKUP, client.player);
        }
    }

    private void openTargetContainer(MinecraftClient client, BlockPos pos) {
        actionQueue.add(() -> {
            BlockHitResult hitResult = new BlockHitResult(new Vec3d(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5), Direction.UP, pos, false);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            currentMapper = null;
        });
        actionQueue.add(() -> actionWaitTicks = 5);
        actionQueue.add(this::waitForUi);
    }

    private void waitForUi() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.currentScreenHandler == client.player.playerScreenHandler) {
            actionQueue.addFirst(this::waitForUi);
        }
    }

    private int countEmptyPlayerSlots(MinecraftClient client) {
        int count = 0;
        for (int i = 0; i < 36; i++) { if (client.player.getInventory().getStack(i).isEmpty()) count++; }
        return count;
    }

    private int findEmptyPlayerSlot(MinecraftClient client) {
        for (int i = 9; i < 36; i++) if (client.player.getInventory().getStack(i).isEmpty()) return i;
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }

    private int findItemInPlayerInv(MinecraftClient client, ItemStack target) {
        for (int i = 0; i < 36; i++) if (ItemMatcher.isSameItem(client.player.getInventory().getStack(i), target)) return i;
        return -1;
    }

    private ShulkerSearchResult findItemInShulkers(MinecraftClient client, ItemStack target) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = client.player.getInventory().getStack(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                ContainerComponent c = s.get(DataComponentTypes.CONTAINER);
                if (c != null) {
                    List<ItemStack> inner = c.stream().toList();
                    for (int j = 0; j < inner.size(); j++) if (ItemMatcher.isSameItem(inner.get(j), target)) return new ShulkerSearchResult(i, j);
                }
            }
        }
        return null;
    }

    private record ShulkerSearchResult(int shulkerPlayerSlot, int itemSlotInShulker) {}

    private void reset() {
        currentTask = null;
        currentMapper = null;
        silentlyExtracting = false;
        actionQueue.clear();
        actionWaitTicks = 0;
        watchdogTimer = 0;
        borrowedItems.clear();
        lastOpenedShulkerSlot = -1;
    }

    public BlockPos getCurrentTaskPos() {
        return currentTask != null ? currentTask.targetPos : null;
    }
}