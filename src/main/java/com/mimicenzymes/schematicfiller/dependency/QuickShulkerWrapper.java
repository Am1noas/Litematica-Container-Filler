package com.mimicenzymes.schematicfiller.dependency;

import net.kyrptonaught.quickshulker.network.OpenShulkerPacket; 
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class QuickShulkerWrapper implements IShulkerExtractor {

    @Override
    public boolean requestOpenShulker(int playerSlotIndex) {
        try {

            ClientPlayNetworking.send(new OpenShulkerPacket(playerSlotIndex));
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}