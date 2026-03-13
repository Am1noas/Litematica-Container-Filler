package com.mimicenzymes.schematichelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public class Hotkeys {
    public static final ConfigHotkey FILL_CONTAINER = new ConfigHotkey("fillContainer", "V", "Fill container");
    public static final ConfigHotkey TOGGLE_CONTINUOUS = new ConfigHotkey("toggleContinuous", "", "Toggle continuous");
    public static final ConfigHotkey TOGGLE_MODE = new ConfigHotkey("toggleMode", "", "Toggle mode");
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey("openConfigGui", "L,C", "Open config");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            FILL_CONTAINER, TOGGLE_CONTINUOUS, TOGGLE_MODE, OPEN_CONFIG_GUI
    );
}