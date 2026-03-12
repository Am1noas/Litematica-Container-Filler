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
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import java.util.Map;

public class Callbacks implements IHotkeyCallback {
    private static final Callbacks INSTANCE = new Callbacks();
    public static Callbacks getInstance() { return INSTANCE; }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || action != KeyAction.PRESS) return false;

        // 这里的比较逻辑现在非常稳固
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
            mc.player.sendMessage(Text.literal(state ? "§a开启持续填充" : "§c关闭持续填充"), true);
            return true;
        } else if (key == Hotkeys.TOGGLE_MODE.getKeybind()) {
            boolean state = !Configs.AREA_MODE.getBooleanValue();
            Configs.AREA_MODE.setBooleanValue(state);
            mc.player.sendMessage(Text.literal(state ? "§b范围模式" : "§e单体模式"), true);
            return true;
        }
        return false;
    }

    private void executeFill(MinecraftClient mc) {
        if (Configs.AREA_MODE.getBooleanValue()) {
            AreaScanner.executeScan(mc); // <-- 就是这里
        } else if (mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            Map<Integer, ItemStack> items = SchematicContainerReader.getRequiredItems(pos, mc.world);
            if (!items.isEmpty()) AutoFillerStateMachine.getInstance().addTask(pos, items);
        }
    }

}