package com.mimicenzymes.schematichelper.input;

import com.mimicenzymes.schematichelper.SchematicHelperClient;
import com.mimicenzymes.schematichelper.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();
    public static InputHandler getInstance() { return INSTANCE; }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
            hotkey.getKeybind().setCallback(Callbacks.getInstance());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(SchematicHelperClient.MOD_ID, SchematicHelperClient.MOD_ID, Hotkeys.HOTKEY_LIST);
    }
}