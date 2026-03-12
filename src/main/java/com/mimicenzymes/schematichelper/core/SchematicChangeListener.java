package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.MinecraftClient;

public class SchematicChangeListener {
    private static Object lastSchematic = null;

    public static void tick(MinecraftClient mc) {
        Object current = SchematicWorldHandler.getSchematicWorld();

        if (current != lastSchematic) {
            lastSchematic = current;
            SchematicContainerIndex.rebuildIndex(mc);
        }
    }
}