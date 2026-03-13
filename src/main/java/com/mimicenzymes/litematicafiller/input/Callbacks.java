package com.mimicenzymes.litematicafiller.input;

import com.mimicenzymes.litematicafiller.config.Configs;
import com.mimicenzymes.litematicafiller.config.GuiConfigs;
import com.mimicenzymes.litematicafiller.config.Hotkeys;
import com.mimicenzymes.litematicafiller.core.AreaScanner;
import com.mimicenzymes.litematicafiller.core.AutoFillerStateMachine;
import com.mimicenzymes.litematicafiller.core.RealContainerCache;
import com.mimicenzymes.litematicafiller.core.LitematicaContainerReader;
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

        if (!Configs.ENABLE_MOD.getBooleanValue()) {
            return false;
        }

        if (mc.player == null) return false;

        if (!Configs.ENABLE_MOD.getBooleanValue()) {
            if (key == Hotkeys.FILL_CONTAINER.getKeybind() ||
                    key == Hotkeys.TOGGLE_CONTINUOUS.getKeybind() ||
                    key == Hotkeys.TOGGLE_MODE.getKeybind()) {
                mc.player.sendMessage(Text.translatable("schematic_container_filler.message.mod_disabled"), true);
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
            mc.player.sendMessage(Text.translatable(state ? "schematic_container_filler.message.continuous_on" : "schematic_container_filler.message.continuous_off"), true);
            return true;
        } else if (key == Hotkeys.TOGGLE_MODE.getKeybind()) {
            boolean state = !Configs.AREA_MODE.getBooleanValue();
            Configs.AREA_MODE.setBooleanValue(state);
            mc.player.sendMessage(Text.translatable(state ? "schematic_container_filler.message.mode_area" : "schematic_container_filler.message.mode_single"), true);
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
                Map<Integer, ItemStack> items = LitematicaContainerReader.getRequiredItems(pos, mc.world.getRegistryManager());

                if (items != null && !items.isEmpty()) {
                    if (!RealContainerCache.isSatisfied(pos, items)) {
                        AutoFillerStateMachine.getInstance().addTask(pos, items);
                        mc.player.sendMessage(Text.translatable("schematic_container_filler.message.task_dispatched"), true);
                    } else {
                        mc.player.sendMessage(Text.translatable("schematic_container_filler.message.already_satisfied"), true);
                    }
                } else {
                    mc.player.sendMessage(Text.translatable("schematic_container_filler.message.no_requirements"), true);
                }
            } else {
                mc.player.sendMessage(Text.translatable("schematic_container_filler.message.target_invalid"), true);
            }
        }
    }
}