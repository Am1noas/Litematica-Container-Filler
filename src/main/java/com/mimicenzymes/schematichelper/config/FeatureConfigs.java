package com.mimicenzymes.schematichelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import java.util.List;

public class FeatureConfigs implements IConfigHandler {

    // 🚀 第一个参数必须短，第三个参数是完整的 comment 翻译键
    public static final ConfigBoolean ENABLE_MOD = new ConfigBoolean("enableMod", true, "schematic_container_helper.config.comment.enableMod");
    public static final ConfigBoolean HIGHLIGHT_CONTAINERS = new ConfigBoolean("highlightContainers", true, "schematic_container_helper.config.comment.highlightContainers");
    public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor("highlightColor", "0x808B4513", "schematic_container_helper.config.comment.highlightColor");
    public static final ConfigInteger FILL_DELAY = new ConfigInteger("fillDelay", 0, 0, 100, "schematic_container_helper.config.comment.fillDelay");
    public static final ConfigBoolean ENABLE_QS_EXTRACTION = new ConfigBoolean("enableQsExtraction", true, "schematic_container_helper.config.comment.enableQsExtraction");

    public static final ConfigHotkey FILL_HOTKEY = new ConfigHotkey("fillHotkey", "V", "schematic_container_helper.config.comment.fillHotkey");
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey("openConfigGui", "L,C", "schematic_container_helper.config.comment.openConfigGui");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_MOD, HIGHLIGHT_CONTAINERS, HIGHLIGHT_COLOR, FILL_DELAY, ENABLE_QS_EXTRACTION
    );

    public static final List<ConfigHotkey> HOTKEYS = ImmutableList.of(
            FILL_HOTKEY, OPEN_CONFIG_GUI
    );

    @Override public void load() {}
    @Override public void save() {}
}