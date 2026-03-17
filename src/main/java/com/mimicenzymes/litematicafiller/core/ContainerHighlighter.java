package com.mimicenzymes.litematicafiller.core;

import com.mimicenzymes.litematicafiller.render.HighlightRenderer;
import com.mimicenzymes.litematicafiller.render.HighlightScanner;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public class ContainerHighlighter {

    public static void tick(MinecraftClient client) {
        HighlightScanner.tick(client);
    }

    public static void onRender(WorldRenderContext context) {
        HighlightRenderer.getInstance().render(context);
    }
}