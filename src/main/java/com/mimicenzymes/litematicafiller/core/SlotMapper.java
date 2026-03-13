package com.mimicenzymes.litematicafiller.core;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.HashMap;
import java.util.Map;

public class SlotMapper {

    private final Map<Integer, Integer> playerToUiMap = new HashMap<>();
    private final Map<Integer, Integer> containerToUiMap = new HashMap<>();

    /**
     * 在容器界面刚刚打开时实例化此映射器
     * @param handler   当前打开的界面的 ScreenHandler
     * @param playerInv 玩家自己的背包实例 (用于区分敌我)
     */
    public SlotMapper(ScreenHandler handler, PlayerInventory playerInv) {
        //遍历当前界面里的每一个格子
        for (int uiSlotId = 0; uiSlotId < handler.slots.size(); uiSlotId++) {
            Slot slot = handler.slots.get(uiSlotId);

            if (slot.inventory == null) continue;

            if (slot.inventory == playerInv) {
                //如果这个格子属于玩家自己
                // lot.getIndex()会返0~35(主背包0~26+快捷栏27~35)
                playerToUiMap.put(slot.getIndex(), uiSlotId);
            } else {
                //如果这个格子不属于玩家，那它必然属于我们正在查看的目标容器
                //原版大箱子在底层是一个DoubleInventory，它的getIndex()会完美地返回0~53
                containerToUiMap.put(slot.getIndex(), uiSlotId);
            }
        }
    }

    /**
     * 查询：我想点击玩家背包的第x格，我该发哪个UI Slot ID？
     * @return UI Slot ID，如果UI越界或找不到则返回-1
     */
    public int getUiSlotForPlayer(int playerSlotIndex) {
        return playerToUiMap.getOrDefault(playerSlotIndex, -1);
    }

    /**
     * 查询：我想点击目标容器的第y格，我该发哪个UI Slot ID？
     * @return UI Slot ID，如果UI越界或找不到则返回-1
     */
    public int getUiSlotForContainer(int containerSlotIndex) {
        return containerToUiMap.getOrDefault(containerSlotIndex, -1);
    }
}