package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.config.Configs;
import com.mimicenzymes.schematichelper.config.GuiConfigs;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import com.mimicenzymes.schematichelper.core.AreaScanner;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.RealContainerCache;
import com.mimicenzymes.schematichelper.core.SchematicContainerReader;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
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

        if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind()) {
            GuiBase.openGui(new GuiConfigs(null));
            return true;
        }

        if (mc.player == null) return false;

        if (!Configs.ENABLE_MOD.getBooleanValue()) {
            if (key == Hotkeys.FILL_CONTAINER.getKeybind() ||
                    key == Hotkeys.TOGGLE_CONTINUOUS.getKeybind() ||
                    key == Hotkeys.TOGGLE_MODE.getKeybind()) {
                mc.player.sendMessage(Text.literal("§c[投影容器填充机] 模组当前已停用，快捷键无效！"), true); // 改为 true，显示在动作栏
            }
            return false;
        }

        if (key == Hotkeys.FILL_CONTAINER.getKeybind()) {
            AutoFillerStateMachine.getInstance().clearBlacklist();
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
            AreaScanner.executeScan(mc, false);
        } else {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
                BlockPos pos = bhr.getBlockPos();
                Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());

                if (items != null && !items.isEmpty()) {
                    if (!RealContainerCache.isSatisfied(pos, items)) {
                        AutoFillerStateMachine.getInstance().addTask(pos, items);
                        mc.player.sendMessage(Text.literal("§a[投影容器填充机] 正在添加单体填充任务..."), true);
                    } else {
                        mc.player.sendMessage(Text.literal("§e[投影容器填充机] 该容器已经满足投影要求，无需填充。"), true);
                    }
                } else {
                    mc.player.sendMessage(Text.literal("§e[投影容器填充机] 准星指向的容器没有投影要求。"), true);
                }
            } else {
                mc.player.sendMessage(Text.literal("§c[投影容器填充机] 请将准星准确对准一个容器方块！"), true);
            }
        }
    }
}