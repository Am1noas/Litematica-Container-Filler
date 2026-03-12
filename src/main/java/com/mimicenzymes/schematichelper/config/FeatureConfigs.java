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

    private static final String PREFIX = "schematic_container_helper.config.";

    public static final ConfigBoolean ENABLE_MOD = new ConfigBoolean(PREFIX + "enableMod", true, PREFIX + "enableMod");
    public static final ConfigBoolean HIGHLIGHT_CONTAINERS = new ConfigBoolean(PREFIX + "highlightContainers", true, PREFIX + "highlightContainers");
    public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor(PREFIX + "highlightColor", "0x808B4513", PREFIX + "highlightColor");
    public static final ConfigInteger FILL_DELAY = new ConfigInteger(PREFIX + "fillDelay", 0, 0, 100, PREFIX + "fillDelay");
    public static final ConfigBoolean ENABLE_QS_EXTRACTION = new ConfigBoolean(PREFIX + "enableQsExtraction", true, PREFIX + "enableQsExtraction");

    public static final ConfigHotkey FILL_HOTKEY = new ConfigHotkey(PREFIX + "fillHotkey", "V", PREFIX + "fillHotkey");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_MOD,
            HIGHLIGHT_CONTAINERS,
            HIGHLIGHT_COLOR,
            FILL_DELAY,
            ENABLE_QS_EXTRACTION
    );

    public static final List<ConfigHotkey> HOTKEYS = ImmutableList.of(FILL_HOTKEY);

    @Override public void load() {}
    @Override public void save() {}
}