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
        if (mc.player == null || action != KeyAction.PRESS) return false;

        if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind()) {
            GuiBase.openGui(new GuiConfigs(null));
            return true;
        }

        if (!Configs.ENABLE_MOD.getBooleanValue()) return false;

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
            // 🚀 核心修复：传入 getRegistryManager() 而不是 mc.world
            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());
            if (items != null && !items.isEmpty()) {
                AutoFillerStateMachine.getInstance().addTask(pos, items);
            }
        }
    }
}