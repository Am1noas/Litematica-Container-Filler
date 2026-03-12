package com.mimicenzymes.schematichelper.config;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

import java.util.ArrayList;
import java.util.List;

public class GuiConfigs extends GuiConfigsBase {

    private ConfigGuiTab tab = ConfigGuiTab.FEATURE;

    public GuiConfigs(Screen parent) {
        super(10, 50, "schematic_container_helper", parent, "schematic_container_helper.gui.title.configs");
    }

    public void setTab(ConfigGuiTab tab) {
        if (this.tab != tab) {
            this.tab = tab;
            this.initGui();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();
        int x = 10;
        int y = 26;
        x += this.createButton(x, y, -1, ConfigGuiTab.FEATURE) + 2;
        x += this.createButton(x, y, -1, ConfigGuiTab.HOTKEYS) + 2;
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(this.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth();
    }

    @Override
    protected int getConfigWidth() {
        return 280;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<ConfigOptionWrapper> list = new ArrayList<>();
        if (this.tab == ConfigGuiTab.FEATURE) {
            FeatureConfigs.OPTIONS.forEach(config -> list.add(new ConfigOptionWrapper(config)));
        } else if (this.tab == ConfigGuiTab.HOTKEYS) {
            FeatureConfigs.HOTKEYS.forEach(hotkey -> list.add(new ConfigOptionWrapper(hotkey)));
        }
        return list;
    }

    public enum ConfigGuiTab {
        FEATURE("schematic_container_helper.gui.button.feature"),
        HOTKEYS("schematic_container_helper.gui.button.hotkeys");

        private final String translationKey;
        ConfigGuiTab(String translationKey) { this.translationKey = translationKey; }
        public String getDisplayName() { return I18n.translate(this.translationKey); }
    }

    private static class ButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final GuiConfigs parent;
        public ButtonListener(ConfigGuiTab tab, GuiConfigs parent) { this.tab = tab; this.parent = parent; }
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) { this.parent.setTab(this.tab); }
    }
}