package com.mimicenzymes.schematichelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import java.util.List;

public class Configs {
    public static final ConfigBoolean ENABLE_MOD = new ConfigBoolean("enableMod", true, "启用主功能");
    public static final ConfigBoolean CONTINUOUS_FILL = new ConfigBoolean("continuousFill", false, "持续填充模式");
    public static final ConfigBoolean AREA_MODE = new ConfigBoolean("areaMode", false, "范围填充模式");
    public static final ConfigInteger FILL_RADIUS = new ConfigInteger("fillRadius", 5, 1, 32, "扫描半径");
    public static final ConfigInteger RENDER_RADIUS = new ConfigInteger("renderRadius", 15, 1, 64, "渲染半径");
    public static final ConfigBoolean ENABLE_QS_EXTRACTION = new ConfigBoolean("enableQsExtraction", true, "潜影盒提取");
    public static final ConfigInteger FILL_DELAY = new ConfigInteger("fillDelay", 0, 0, 100, "填充延迟(Tick)");

    // 渲染层与数据开关
    public static final ConfigBoolean ENABLE_DATA_SYNC = new ConfigBoolean("enableDataSync", true, "接受Servux服务端数据包同步");
    public static final ConfigBoolean SYNC_LITE_LAYER = new ConfigBoolean("syncLiteLayer", true, "高亮跟随 Litematica 渲染层");
    public static final ConfigBoolean HIDE_COMPLETED_CONTAINERS = new ConfigBoolean("hideCompletedContainers", true, "隐藏已正确填充的容器高亮");

    // 高亮颜色
    public static final ConfigBoolean HIGHLIGHT_CONTAINERS = new ConfigBoolean("highlightContainers", true, "开启缺货高亮");
    public static final ConfigBoolean HIGHLIGHT_XRAY = new ConfigBoolean("highlightXray", true, "高亮透视");
    public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor("highlightColor", "0x808B4513", "高亮颜色");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_MOD, AREA_MODE, CONTINUOUS_FILL, FILL_RADIUS, RENDER_RADIUS,
            SYNC_LITE_LAYER, HIDE_COMPLETED_CONTAINERS, ENABLE_DATA_SYNC,
            HIGHLIGHT_CONTAINERS, HIGHLIGHT_XRAY, HIGHLIGHT_COLOR,
            FILL_DELAY, ENABLE_QS_EXTRACTION
    );
}