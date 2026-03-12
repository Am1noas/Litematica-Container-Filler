package com.mimicenzymes.schematichelper.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // 当玩家在ModMenu里点击设置时，打开GuiConfigs界面
        return GuiConfigs::new;
    }
}