package com.mimicenzymes.schematichelper;

import com.mimicenzymes.schematichelper.config.ConfigHandler;
import com.mimicenzymes.schematichelper.config.Configs;
import com.mimicenzymes.schematichelper.config.GuiConfigs;
import com.mimicenzymes.schematichelper.core.AreaScanner;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.ContainerHighlighter;
import com.mimicenzymes.schematichelper.core.SchematicChangeListener;
import com.mimicenzymes.schematichelper.core.RealContainerCache;
import com.mimicenzymes.schematichelper.input.InputHandler;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class SchematicHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "schematic_container_helper";

    private static boolean isGuiAutoRegistered = false;
    private static int printerTickTimer = 0;
    //防鬼畜冷却器
    private static final Map<BlockPos, Long> CROSSHAIR_COOLDOWNS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isGuiAutoRegistered) {
                boolean isTitleScreen = client.currentScreen != null && client.currentScreen.getClass().getSimpleName().equals("TitleScreen");
                boolean isInWorld = client.player != null;
                if (isTitleScreen || isInWorld) {
                    try { new GuiConfigs(null); } catch (Exception e) {}
                    isGuiAutoRegistered = true;
                }
            }

            if (!com.mimicenzymes.schematichelper.config.Configs.ENABLE_MOD.getBooleanValue()) {
                return;
            }

            if (client.world != null) {
                AutoFillerStateMachine.getInstance().tick(client);
                SchematicChangeListener.tick(client);
                RealContainerCache.tick(client);

                if (Configs.CONTINUOUS_FILL.getBooleanValue() && AutoFillerStateMachine.getInstance().isIdle()) {
                    printerTickTimer++;
                    if (printerTickTimer >= 10) { // 每 0.5 秒判定一次
                        printerTickTimer = 0;

                        if (Configs.AREA_MODE.getBooleanValue()) {
                            AreaScanner.executeScan(client, true);
                        } else {
                            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                                BlockHitResult bhr = (BlockHitResult) client.crosshairTarget;
                                BlockPos pos = bhr.getBlockPos();
                                long now = System.currentTimeMillis();

                                if (!CROSSHAIR_COOLDOWNS.containsKey(pos) || now - CROSSHAIR_COOLDOWNS.get(pos) >= 5000) {
                                    Map<Integer, ItemStack> required = com.mimicenzymes.schematichelper.core.SchematicContainerReader.getRequiredItems(pos, client.world.getRegistryManager());
                                    if (required != null && !required.isEmpty() && !RealContainerCache.isSatisfied(pos, required)) {
                                        AutoFillerStateMachine.getInstance().addTask(pos, required);
                                        CROSSHAIR_COOLDOWNS.put(pos, now);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (com.mimicenzymes.schematichelper.config.Configs.ENABLE_MOD.getBooleanValue()) {
                ContainerHighlighter.onRender(context);
            }
        });
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    private static class InitHandler implements IInitializationHandler {
        @Override
        public void registerModHandlers() {
            ConfigHandler configHandler = new ConfigHandler();
            configHandler.load();
            ConfigManager.getInstance().registerConfigHandler(SchematicHelperClient.MOD_ID, configHandler);
            InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());

        }
    }
}