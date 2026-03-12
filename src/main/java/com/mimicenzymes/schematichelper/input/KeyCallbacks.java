package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.config.FeatureConfigs;
import com.mimicenzymes.schematichelper.config.GuiConfigs;
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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class KeyCallbacks {

    public static void init() {
        FeatureConfigs.FILL_HOTKEY.getKeybind().setCallback(new FillContainerCallback());

        FeatureConfigs.OPEN_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                GuiBase.openGui(new GuiConfigs(null));
                return true;
            }
            return false;
        });
    }

    private static class FillContainerCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null || client.world == null || client.currentScreen != null) {
                return false;
            }

            if (!FeatureConfigs.ENABLE_MOD.getBooleanValue()) {
                client.player.sendMessage(Text.translatable("schematic_container_helper.message.disabled_hint"), true);
                return false;
            }

            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                BlockPos targetPos = blockHitResult.getBlockPos();

                Map<Integer, ItemStack> requiredItems = SchematicContainerReader.getRequiredItems(targetPos, client.world);

                if (requiredItems.isEmpty()) {
                    client.player.sendMessage(Text.translatable("schematic_container_helper.message.no_data"), true);
                } else {
                    AutoFillerStateMachine.getInstance().addTask(targetPos, requiredItems);
                    client.player.sendMessage(Text.translatable("schematic_container_helper.message.start_fill"), true);
                }
                return true;
            }

            return false;
        }
    }
}