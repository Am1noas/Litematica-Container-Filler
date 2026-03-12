package com.mimicenzymes.schematichelper.core;

import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;

public class CompletedContainers {
    private static final Set<BlockPos> COMPLETED = new HashSet<>();

    public static void add(BlockPos pos) {
        COMPLETED.add(pos.toImmutable());
    }

    public static boolean isCompleted(BlockPos pos) {
        return COMPLETED.contains(pos);
    }

    public static void clear() {
        COMPLETED.clear();
    }
}