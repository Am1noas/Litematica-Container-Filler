package com.mimicenzymes.schematichelper.config;

import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.util.JsonUtils;

import net.fabricmc.loader.api.FabricLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;

public class ConfigHandler implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = "schematic_container_helper.json";

    @Override
    public void load()
    {
        File dir = FabricLoader.getInstance().getConfigDir().toFile();
        File file = new File(dir, CONFIG_FILE_NAME);

        if (file.exists() && file.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(file);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                // 🚀 直接去 Configs 和 Hotkeys 里读数据
                ConfigUtils.readConfigBase(root, "Features", Configs.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            }
        }
    }

    @Override
    public void save()
    {
        File dir = FabricLoader.getInstance().getConfigDir().toFile();

        if (dir.exists() || dir.mkdirs())
        {
            JsonObject root = new JsonObject();

            // 🚀 直接去 Configs 和 Hotkeys 里存数据
            ConfigUtils.writeConfigBase(root, "Features", Configs.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }
}