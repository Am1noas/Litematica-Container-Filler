package com.mimicenzymes.schematichelper;

import com.mimicenzymes.schematichelper.config.FeatureConfigs;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.ContainerHighlighter;
import com.mimicenzymes.schematichelper.input.KeyCallbacks;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class SchematicHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "schematic_container_helper";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoFillerStateMachine.getInstance().tick(client);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ContainerHighlighter.onRender(context);
        });

        InitializationHandler.getInstance().registerInitializationHandler(new ModInitHandler());
    }

    private static class ModInitHandler implements IInitializationHandler {
        @Override
        public void registerModHandlers() {
            ConfigManager.getInstance().registerConfigHandler(MOD_ID, new FeatureConfigs());
            KeyCallbacks.init();
            InputEventHandler.getKeybindManager().registerKeybindProvider(new IKeybindProvider() {
                @Override
                public void addKeysToMap(IKeybindManager manager) {
                    FeatureConfigs.HOTKEYS.forEach(hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
                }

                @Override
                public void addHotkeys(IKeybindManager manager) {
                    manager.addHotkeysForCategory(MOD_ID, MOD_ID, FeatureConfigs.HOTKEYS);
                }
            });
        }
    }
}