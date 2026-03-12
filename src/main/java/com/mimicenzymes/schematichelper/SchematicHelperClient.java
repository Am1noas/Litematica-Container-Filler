package com.mimicenzymes.schematichelper;

import com.mimicenzymes.schematichelper.config.ConfigHandler;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import com.mimicenzymes.schematichelper.input.KeyCallbacks;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.ContainerHighlighter;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class SchematicHelperClient implements ClientModInitializer
{
    public static final String MOD_ID = "schematic_container_helper";

    @Override
    public void onInitializeClient()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                AutoFillerStateMachine.getInstance().tick(client));

        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                ContainerHighlighter.onRender(context));

        InitializationHandler.getInstance()
                .registerInitializationHandler(new InitHandler());
    }

    private static class InitHandler implements IInitializationHandler
    {
        @Override
        public void registerModHandlers()
        {
            // 🚀 1. 实例化咱们刚写的管家，读盘，并注册给 MaLiLib！
            ConfigHandler configHandler = new ConfigHandler();
            configHandler.load();
            ConfigManager.getInstance().registerConfigHandler(MOD_ID, configHandler);

            // 🚀 2. 注册按键提供者
            InputEventHandler.getKeybindManager()
                    .registerKeybindProvider(new IKeybindProvider()
                    {
                        @Override
                        public void addKeysToMap(IKeybindManager manager)
                        {
                            Hotkeys.HOTKEY_LIST.forEach(h ->
                                    manager.addKeybindToMap(h.getKeybind()));
                        }

                        @Override
                        public void addHotkeys(IKeybindManager manager)
                        {
                            manager.addHotkeysForCategory(
                                    MOD_ID,
                                    MOD_ID,
                                    Hotkeys.HOTKEY_LIST);
                        }
                    });

            // 🚀 3. 初始化回调 (别忘了这个)
            KeyCallbacks.init();
        }
    }
}