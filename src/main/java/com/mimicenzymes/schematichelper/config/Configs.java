package com.mimicenzymes.schematichelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import java.util.List;

public class Configs {
    public static final ConfigBoolean ENABLE_MOD = new ConfigBoolean("enableMod", true, "Enable mod");
    public static final ConfigBoolean CONTINUOUS_FILL = new ConfigBoolean("continuousFill", false, "Continuous fill");
    public static final ConfigBoolean AREA_MODE = new ConfigBoolean("areaMode", false, "Area mode");
    public static final ConfigInteger FILL_RADIUS = new ConfigInteger("fillRadius", 5, 1, 32, "Scan radius");
    public static final ConfigBoolean SYNC_LITE_LAYER = new ConfigBoolean("syncLiteLayer", true, "Sync layer");
    public static final ConfigBoolean HIGHLIGHT_CONTAINERS = new ConfigBoolean("highlightContainers", true, "Highlight");
    public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor("highlightColor", "0x808B4513", "Color");
    public static final ConfigInteger FILL_DELAY = new ConfigInteger("fillDelay", 0, 0, 100, "Delay");
    public static final ConfigBoolean ENABLE_QS_EXTRACTION = new ConfigBoolean("enableQsExtraction", true, "QS Extraction");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_MOD, CONTINUOUS_FILL, AREA_MODE, FILL_RADIUS,
            SYNC_LITE_LAYER, HIGHLIGHT_CONTAINERS, HIGHLIGHT_COLOR, FILL_DELAY, ENABLE_QS_EXTRACTION
    );
}