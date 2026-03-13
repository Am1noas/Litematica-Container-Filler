package com.mimicenzymes.schematicfiller.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public class Hotkeys {
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey("schematic_container_filler.hotkey.name.openConfigGui", "L,C", "schematic_container_filler.hotkey.comment.openConfigGui");
    public static final ConfigHotkey FILL_CONTAINER = new ConfigHotkey("schematic_container_filler.hotkey.name.fillContainer", "V", "schematic_container_filler.hotkey.comment.fillContainer");
    public static final ConfigHotkey TOGGLE_CONTINUOUS = new ConfigHotkey("schematic_container_filler.hotkey.name.toggleContinuous", "", "schematic_container_filler.hotkey.comment.toggleContinuous");
    public static final ConfigHotkey TOGGLE_MODE = new ConfigHotkey("schematic_container_filler.hotkey.name.toggleMode", "", "schematic_container_filler.hotkey.comment.toggleMode");
    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_CONFIG_GUI, FILL_CONTAINER, TOGGLE_CONTINUOUS, TOGGLE_MODE
    );
}