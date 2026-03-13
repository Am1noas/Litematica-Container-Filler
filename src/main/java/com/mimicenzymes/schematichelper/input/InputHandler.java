package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();
    public static InputHandler getInstance() { return INSTANCE; }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }

        Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(Callbacks.getInstance());
        Hotkeys.FILL_CONTAINER.getKeybind().setCallback(Callbacks.getInstance());
        Hotkeys.TOGGLE_CONTINUOUS.getKeybind().setCallback(Callbacks.getInstance());
        Hotkeys.TOGGLE_MODE.getKeybind().setCallback(Callbacks.getInstance());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
    }
}