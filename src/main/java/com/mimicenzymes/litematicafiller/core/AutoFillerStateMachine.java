//核心出装
package com.mimicenzymes.litematicafiller.core;

import com.mimicenzymes.litematicafiller.config.Configs;
import com.mimicenzymes.litematicafiller.dependency.DependencyChecker;
import com.mimicenzymes.litematicafiller.dependency.DummyExtractor;
import com.mimicenzymes.litematicafiller.dependency.IShulkerExtractor;
import com.mimicenzymes.litematicafiller.dependency.QuickShulkerWrapper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoFillerStateMachine {

    public static class FillTask {
        public final BlockPos targetPos;
        public final Map<Integer, ItemStack> requiredItems;
        public final Set<Item> missingInAction = new LinkedHashSet<>(); //记录开箱后发现缺少的材料

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
    private final Map<Integer, Long> pendingCrafterToggles = new HashMap<>();

    //记录缺失的物品ban掉
    private final Map<BlockPos, Set<Item>> failedContainers = new ConcurrentHashMap<>();
    private boolean lastContinuousState = false;
    private int tickCounter = 0;

    private AutoFillerStateMachine() {
        this.shulkerExtractor = DependencyChecker.HAS_QUICK_SHULKER ? new QuickShulkerWrapper() : new DummyExtractor();
    }

    public void addTask(BlockPos pos, Map<Integer, ItemStack> requiredItems) {
        if (failedContainers.containsKey(pos)) return;

        if (currentTask != null && currentTask.targetPos.equals(pos)) return;
        for (FillTask t : taskQueue) {
            if (t.targetPos.equals(pos)) return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            boolean hasAtLeastOneMaterial = false;
            Set<Item> missingItems = new LinkedHashSet<>();

            for (ItemStack req : requiredItems.values()) {
                if (hasItemAnywhere(client, req.getItem())) {
                    hasAtLeastOneMaterial = true;
                } else {
                    missingItems.add(req.getItem());
                }
            }

            if (!hasAtLeastOneMaterial && !missingItems.isEmpty()) {
                failedContainers.put(pos, missingItems);

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Item item : missingItems) {
                    if (count > 0) sb.append(", ");
                    sb.append(item.getName().getString());
                    count++;
                    if (count >= 3 && missingItems.size() > 3) {
                        sb.append(" 等");
                        break;
                    }
                }
                sendFeedback(client, Text.translatable("litematica_container_filler.message.material_shortage", sb.toString()).getString(), true);
                return;
            }
        }

        taskQueue.add(new FillTask(pos, requiredItems));
    }

    public boolean isSilentlyExtracting() { return silentlyExtracting; }

    public void clearBlacklist() {
        failedContainers.clear();
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) { reset(); return; }

        boolean currentContinuousState = Configs.CONTINUOUS_FILL.getBooleanValue();
        if (currentContinuousState != lastContinuousState) {
            clearBlacklist();
            if (!currentContinuousState) {
                taskQueue.clear();
            }
            lastContinuousState = currentContinuousState;
        }

        tickCounter++;
        //智能解封
        if (!failedContainers.isEmpty() && tickCounter % 10 == 0) {
            failedContainers.entrySet().removeIf(entry -> {
                for (Item item : entry.getValue()) {
                    if (hasItemAnywhere(client, item)) return true;
                }
                return false;
            });
        }

        if (currentTask != null) {
            watchdogTimer++;
            if (watchdogTimer > 100) {
                sendFeedback(client, Text.translatable("litematica_container_filler.message.timeout_reset").getString(), true);
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
                sendFeedback(client, Text.translatable("litematica_container_filler.message.cursor_stuck").getString(), true);
                reset(); return;
            }
            if (delay > 0) { actionWaitTicks = delay; return; }
        }

        if (handler instanceof CrafterScreenHandler crafterHandler && client.currentScreen instanceof HandledScreen<?> handledScreen) {
            Set<Integer> targetDisabled = LitematicaContainerReader.getDisabledSlots(currentTask.targetPos);
            boolean toggledInThisTick = false;

            for (int i = 0; i < 9; i++) {
                boolean shouldBeDisabled = targetDisabled != null && targetDisabled.contains(i);
                boolean isCurrentlyDisabled = crafterHandler.isSlotDisabled(i);

                if (shouldBeDisabled != isCurrentlyDisabled) {
                    if (crafterHandler.getSlot(i).hasStack()) {
                        simulateSlotClick(handledScreen, crafterHandler.getSlot(i), i, 0, SlotActionType.QUICK_MOVE);
                    } else {
                        simulateSlotClick(handledScreen, crafterHandler.getSlot(i), i, 0, SlotActionType.PICKUP);
                    }
                    toggledInThisTick = true;
                    if (delay > 0) break;
                }
            }
            if (toggledInThisTick) {
                actionWaitTicks = Math.max(delay, 1);
                if (delay > 0) return;
            }
        }

        boolean allMatched = true;
        int containerSize = handler.slots.size() - 36;

        if (handler instanceof net.minecraft.screen.CrafterScreenHandler) {
            containerSize = 9;
        }

        if (containerSize <= 0) { reset(); return; }

        for (int containerSlot = 0; containerSlot < containerSize; containerSlot++) {

            if (handler instanceof CrafterScreenHandler ch && ch.isSlotDisabled(containerSlot)) {
                continue;
            }

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

                    failedContainers.computeIfAbsent(currentTask.targetPos, k -> new LinkedHashSet<>()).add(required.getItem());
                    currentTask.missingInAction.add(required.getItem());

                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (Item item : currentTask.missingInAction) {
                        if (count > 0) sb.append(", ");
                        sb.append(item.getName().getString());
                        count++;
                        if (count >= 3 && currentTask.missingInAction.size() > 3) {
                            sb.append(" 等");
                            break;
                        }
                    }
                    sendFeedback(client, Text.translatable("litematica_container_filler.message.fill_success").getString(), true);

                    if (current.isEmpty()) currentTask.requiredItems.remove(containerSlot);
                    else currentTask.requiredItems.put(containerSlot, current.copy());
                }
            }
        }

        if (allMatched) {
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

    private void sendFeedback(MinecraftClient client, String text, boolean isActionBar) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(text), isActionBar);
        }
    }

    private boolean hasItemAnywhere(MinecraftClient client, Item targetItem) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = client.player.getInventory().getStack(i);
            if (s.isOf(targetItem)) return true;
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                ContainerComponent c = s.get(DataComponentTypes.CONTAINER);
                if (c != null) {
                    for (ItemStack inner : c.stream().toList()) {
                        if (inner.isOf(targetItem)) return true;
                    }
                }
            }
        }
        return false;
    }

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
    public boolean isIdle() {
        return this.currentTask == null && this.actionQueue.isEmpty();
    }

    private void simulateSlotClick(HandledScreen<?> screen, Slot slot, int slotId, int button, SlotActionType actionType) {
        try {
            java.lang.reflect.Method targetMethod = null;
            Class<?> currClass = screen.getClass();
            while (currClass != null && targetMethod == null) {
                for (java.lang.reflect.Method m : currClass.getDeclaredMethods()) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 4
                            && params[0] == Slot.class
                            && params[1] == int.class
                            && params[2] == int.class
                            && params[3] == SlotActionType.class) {
                        targetMethod = m;
                        break;
                    }
                }
                currClass = currClass.getSuperclass();
            }

            if (targetMethod != null) {
                targetMethod.setAccessible(true);
                //强制触发GUI上的鼠标点击
                targetMethod.invoke(screen, slot, slotId, button, actionType);
            } else {
                //如果找不到，兜底使用发包模式
                MinecraftClient.getInstance().interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, button, actionType, MinecraftClient.getInstance().player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}