package com.mimicenzymes.litematicafiller.dependency;

import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class QuickShulkerWrapper implements IShulkerExtractor {

    @Override
    public boolean requestOpenShulker(int playerSlotIndex) {
        try {
            ClientPlayNetworking.send(new OpenShulkerPacket(playerSlotIndex));
            return true;

        } catch (Exception e) {
            System.err.println("[LitematicaFiller] 尝试发送 QuickShulker 数据包失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}