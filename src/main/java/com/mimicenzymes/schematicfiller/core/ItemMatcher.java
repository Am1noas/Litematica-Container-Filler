package com.mimicenzymes.schematicfiller.core;

import net.minecraft.item.ItemStack;

public class ItemMatcher {
    
    /**
     * 比对两个物品是否一致
     * 必须保证物品类型相同，且所有数据组件完全一致。
     */
    public static boolean isSameItem(ItemStack current, ItemStack required) {
        if (current.isEmpty() || required.isEmpty()) return false;
        
        // 比对 Item 类型和所有的 Data Components
        return ItemStack.areItemsAndComponentsEqual(current, required);
    }
}