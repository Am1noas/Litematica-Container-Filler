package com.mimicenzymes.schematichelper.core;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SchematicContainerIndex {
    private static final List<BlockPos> CONTAINERS = new ArrayList<>();
    private static boolean indexed = false;

    public static void rebuildIndex(MinecraftClient mc) {
        CONTAINERS.clear();
        SchematicCache.clear();

        var schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null) {
            indexed = false;
            return;
        }

        BlockPos.Mutable pos = new BlockPos.Mutable();
        int min = -256;
        int max = 256;

        for (int x = min; x <= max; x++) {
            for (int y = -64; y <= 320; y++) {
                for (int z = min; z <= max; z++) {
                    pos.set(x, y, z);
                    BlockState state = schematicWorld.getBlockState(pos);

                    if (state.hasBlockEntity()) {
                        CONTAINERS.add(pos.toImmutable());
                    }
                }
            }
        }

        SpatialContainerIndex.rebuild(CONTAINERS);
        indexed = true;
    }

    public static List<BlockPos> getContainers() {
        return CONTAINERS;
    }
}