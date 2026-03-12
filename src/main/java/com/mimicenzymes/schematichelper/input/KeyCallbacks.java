package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.config.Configs;
import com.mimicenzymes.schematichelper.config.GuiConfigs;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.SchematicContainerReader;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.KeyAction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class KeyCallbacks
{
    public static void init()
    {
        Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(
                (action, key) -> action == KeyAction.PRESS && openConfig());

        Hotkeys.FILL_CONTAINER.getKeybind().setCallback(
                (action, key) -> action == KeyAction.PRESS && manualFill());

        Hotkeys.TOGGLE_CONTINUOUS.getKeybind().setCallback(
                (action, key) -> action == KeyAction.PRESS && toggleContinuous());

        Hotkeys.TOGGLE_MODE.getKeybind().setCallback(
                (action, key) -> action == KeyAction.PRESS && toggleMode());
    }

    private static boolean openConfig()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return false;

        GuiBase.openGui(new GuiConfigs(null));
        return true;
    }

    private static boolean toggleContinuous()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        boolean state = !Configs.CONTINUOUS_FILL.getBooleanValue();
        Configs.CONTINUOUS_FILL.setBooleanValue(state);

        mc.player.sendMessage(Text.literal(
                state ? "§a[容器助手] 持续填充开启" : "§c[容器助手] 持续填充关闭"), true);

        return true;
    }

    private static boolean toggleMode()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        boolean state = !Configs.AREA_MODE.getBooleanValue();
        Configs.AREA_MODE.setBooleanValue(state);

        mc.player.sendMessage(Text.literal(
                state ? "§b[容器助手] 范围扫描模式" : "§e[容器助手] 单体模式"), true);

        return true;
    }

    // 🚀 真正的填充逻辑归位！
    private static boolean manualFill()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return false;
        if (!Configs.ENABLE_MOD.getBooleanValue()) return false;

        // 范围模式（后续我们要在这里接上 3D 扫描引擎）
        if (Configs.AREA_MODE.getBooleanValue())
        {
            int radius = Configs.FILL_RADIUS.getIntegerValue();
            mc.player.sendMessage(Text.literal("§b[容器助手] 正在扫描半径 " + radius + " ..."), true);
            // TODO: 这里之后会写入范围扫描的后端代码
            return true;
        }

        // 原本的单体模式（准星对准容器）
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof BlockHitResult blockHit)
        {
            BlockPos pos = blockHit.getBlockPos();
            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world);

            if (items.isEmpty())
            {
                mc.player.sendMessage(Text.literal("§c[容器助手] 没有蓝图数据"), true);
                return true;
            }

            // 🚀 核心：把任务塞进机器里！
            AutoFillerStateMachine.getInstance().addTask(pos, items);
            mc.player.sendMessage(Text.literal("§a[容器助手] 开始填充容器"), true);
            return true;
        }

        return false;
    }
}