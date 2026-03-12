package com.mimicenzymes.schematichelper;

import com.mimicenzymes.schematichelper.config.ConfigHandler;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.ContainerHighlighter;
import com.mimicenzymes.schematichelper.core.SchematicChangeListener;
import com.mimicenzymes.schematichelper.core.RealContainerCache; // 🚀 新增导入
import com.mimicenzymes.schematichelper.input.Callbacks;
import com.mimicenzymes.schematichelper.input.InputHandler;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class SchematicHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "schematic_container_helper";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                AutoFillerStateMachine.getInstance().tick(client);
                SchematicChangeListener.tick(client);
                RealContainerCache.tick(client);
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ContainerHighlighter.onRender(context);
        });

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    private static class InitHandler implements IInitializationHandler {
        @Override
        public void registerModHandlers() {
            ConfigHandler configHandler = new ConfigHandler();
            configHandler.load();
            ConfigManager.getInstance().registerConfigHandler(MOD_ID, configHandler);
            InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
            Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(Callbacks.getInstance());
            Hotkeys.FILL_CONTAINER.getKeybind().setCallback(Callbacks.getInstance());
            Hotkeys.TOGGLE_CONTINUOUS.getKeybind().setCallback(Callbacks.getInstance());
            Hotkeys.TOGGLE_MODE.getKeybind().setCallback(Callbacks.getInstance());
        }
    }
}