package com.mimicenzymes.schematichelper.dependency;

/**
 * 潜影盒提取器接口
 * 负责向服务端发送打开背包中潜影盒的请求
 */
public interface IShulkerExtractor {
    /**
     * @param playerSlotIndex 潜影盒在玩家背包中的格子序号 (0-35)
     * @return 请求是否成功发出
     */
    boolean requestOpenShulker(int playerSlotIndex);
}