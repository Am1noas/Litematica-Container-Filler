package com.mimicenzymes.schematichelper;

import com.mimicenzymes.schematichelper.config.ConfigHandler;
import com.mimicenzymes.schematichelper.core.AutoFillerStateMachine;
import com.mimicenzymes.schematichelper.core.ContainerHighlighter;
import com.mimicenzymes.schematichelper.core.SchematicChangeListener;
import com.mimicenzymes.schematichelper.input.InputHandler;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
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
        /*
         * 每 tick 执行：
         * 1 自动填充任务
         * 2 蓝图变化监听
         */
        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            AutoFillerStateMachine.getInstance().tick(client);

            SchematicChangeListener.tick(client);
        });

        /*
         * 渲染容器高亮
         */
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
        {
            ContainerHighlighter.onRender(context);
        });

        /*
         * 注册 malilib 初始化
         */
        InitializationHandler.getInstance()
                .registerInitializationHandler(new InitHandler());
    }

    private static class InitHandler implements IInitializationHandler
    {
        @Override
        public void registerModHandlers()
        {
            /*
             * 配置系统
             */
            ConfigHandler configHandler = new ConfigHandler();
            configHandler.load();

            ConfigManager.getInstance()
                    .registerConfigHandler(MOD_ID, configHandler);

            /*
             * 注册按键
             */
            InputEventHandler.getKeybindManager()
                    .registerKeybindProvider(InputHandler.getInstance());
        }
    }
}