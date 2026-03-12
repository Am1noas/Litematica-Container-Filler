package com.mimicenzymes.schematichelper.core;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompletedContainers {
    private static final Map<BlockPos, Long> COMPLETED = new HashMap<>();
    private static final long GRACE_PERIOD = 5000; // 🚀 5秒绝对信任期

    public static void add(BlockPos pos) {
        COMPLETED.put(pos.toImmutable(), System.currentTimeMillis());
    }

    public static boolean isCompleted(BlockPos pos) {
        return COMPLETED.containsKey(pos);
    }

    // 判断是否在保护期内
    public static boolean isInGracePeriod(BlockPos pos) {
        Long time = COMPLETED.get(pos);
        return time != null && (System.currentTimeMillis() - time) < GRACE_PERIOD;
    }

    public static void remove(BlockPos pos) { COMPLETED.remove(pos); }
    public static void clear() { COMPLETED.clear(); }
    public static List<BlockPos> getCompletedList() { return new ArrayList<>(COMPLETED.keySet()); }
}