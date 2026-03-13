package com.mimicenzymes.schematichelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import java.util.List;

public class Configs {
    public static final ConfigBoolean ENABLE_MOD = new ConfigBoolean("schematic_container_helper.config.name.enableMod", true, "schematic_container_helper.config.comment.enableMod");
    public static final ConfigBoolean CONTINUOUS_FILL = new ConfigBoolean("schematic_container_helper.config.name.continuousFill", false, "schematic_container_helper.config.comment.continuousFill");
    public static final ConfigBoolean AREA_MODE = new ConfigBoolean("schematic_container_helper.config.name.areaMode", false, "schematic_container_helper.config.comment.areaMode");
    public static final ConfigInteger FILL_RADIUS = new ConfigInteger("schematic_container_helper.config.name.fillRadius", 5, 1, 32, "schematic_container_helper.config.comment.fillRadius");
    public static final ConfigInteger RENDER_RADIUS = new ConfigInteger("schematic_container_helper.config.name.renderRadius", 15, 1, 64, "schematic_container_helper.config.comment.renderRadius");
    public static final ConfigBoolean SYNC_LITE_LAYER = new ConfigBoolean("schematic_container_helper.config.name.syncLiteLayer", true, "schematic_container_helper.config.comment.syncLiteLayer");
    public static final ConfigBoolean HIDE_COMPLETED_CONTAINERS = new ConfigBoolean("schematic_container_helper.config.name.hideCompletedContainers", true, "schematic_container_helper.config.comment.hideCompletedContainers");
    public static final ConfigBoolean ENABLE_DATA_SYNC = new ConfigBoolean("schematic_container_helper.config.name.enableDataSync", true, "schematic_container_helper.config.comment.enableDataSync");
    public static final ConfigBoolean HIGHLIGHT_CONTAINERS = new ConfigBoolean("schematic_container_helper.config.name.highlightContainers", true, "schematic_container_helper.config.comment.highlightContainers");
    public static final ConfigBoolean HIGHLIGHT_XRAY = new ConfigBoolean("schematic_container_helper.config.name.highlightXray", true, "schematic_container_helper.config.comment.highlightXray");
    public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor("schematic_container_helper.config.name.highlightColor", "0x808B4513", "schematic_container_helper.config.comment.highlightColor");
    public static final ConfigInteger FILL_DELAY = new ConfigInteger("schematic_container_helper.config.name.fillDelay", 0, 0, 100, "schematic_container_helper.config.comment.fillDelay");
    public static final ConfigBoolean ENABLE_QS_EXTRACTION = new ConfigBoolean("schematic_container_helper.config.name.enableQsExtraction", true, "schematic_container_helper.config.comment.enableQsExtraction");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_MOD, CONTINUOUS_FILL, AREA_MODE, FILL_RADIUS, RENDER_RADIUS,
            SYNC_LITE_LAYER, HIDE_COMPLETED_CONTAINERS, ENABLE_DATA_SYNC,
            HIGHLIGHT_CONTAINERS, HIGHLIGHT_XRAY,
            HIGHLIGHT_COLOR, FILL_DELAY, ENABLE_QS_EXTRACTION
    );
}