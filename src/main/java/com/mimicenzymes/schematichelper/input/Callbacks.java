package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.config.Configs;
import com.mimicenzymes.schematichelper.config.GuiConfigs;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import com.mimicenzymes.schematichelper.core.AreaScanner;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.SchematicContainerReader;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import java.util.Map;

public class Callbacks implements IHotkeyCallback {
    private static final Callbacks INSTANCE = new Callbacks();
    public static Callbacks getInstance() { return INSTANCE; }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != KeyAction.PRESS) return false;

        // 1. 打开菜单
        if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind()) {
            GuiBase.openGui(new GuiConfigs(null));
            return true;
        }

        if (mc.player == null) return false;

        // 2. 模组总开关拦截（如果有按键按了，但模组关了，弹出红字警告）
        if (!Configs.ENABLE_MOD.getBooleanValue()) {
            if (key == Hotkeys.FILL_CONTAINER.getKeybind() ||
                    key == Hotkeys.TOGGLE_CONTINUOUS.getKeybind() ||
                    key == Hotkeys.TOGGLE_MODE.getKeybind()) {
                mc.player.sendMessage(Text.literal("§c[容器助手] 模组当前已停用，快捷键无效！"), true);
            }
            return false;
        }

        // 3. 业务逻辑，极速内存 == 比对
        if (key == Hotkeys.FILL_CONTAINER.getKeybind()) {
            executeFill(mc);
            return true;
        } else if (key == Hotkeys.TOGGLE_CONTINUOUS.getKeybind()) {
            boolean state = !Configs.CONTINUOUS_FILL.getBooleanValue();
            Configs.CONTINUOUS_FILL.setBooleanValue(state);
            mc.player.sendMessage(Text.translatable(state ? "schematic_container_helper.message.continuous_on" : "schematic_container_helper.message.continuous_off"), true);
            return true;
        } else if (key == Hotkeys.TOGGLE_MODE.getKeybind()) {
            boolean state = !Configs.AREA_MODE.getBooleanValue();
            Configs.AREA_MODE.setBooleanValue(state);
            mc.player.sendMessage(Text.translatable(state ? "schematic_container_helper.message.mode_area" : "schematic_container_helper.message.mode_single"), true);
            return true;
        }

        return false;
    }

    private void executeFill(MinecraftClient mc) {
        if (Configs.AREA_MODE.getBooleanValue()) {
            AreaScanner.executeScan(mc);
        } else if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());
            if (items != null && !items.isEmpty()) {
                AutoFillerStateMachine.getInstance().addTask(pos, items);
            } else {
                mc.player.sendMessage(Text.literal("§e[容器助手] 准星处没有缺货的投影容器"), true);
            }
        }
    }
}