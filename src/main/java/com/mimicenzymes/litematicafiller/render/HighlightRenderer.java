package com.mimicenzymes.litematicafiller.render;

import com.mimicenzymes.litematicafiller.config.Configs;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Map;

public class HighlightRenderer {
    private static final HighlightRenderer INSTANCE = new HighlightRenderer();
    public static HighlightRenderer getInstance() { return INSTANCE; }

    public void render(Object context) {
        if (!Configs.ENABLE_MOD.getBooleanValue() || !Configs.HIGHLIGHT_CONTAINERS.getBooleanValue()) return;

        Map<BlockPos, HighlightState> highlights = HighlightScanner.getHighlights();
        if (highlights.isEmpty()) return;

        MatrixStack matrices = null;
        if (context instanceof MatrixStack) {
            matrices = (MatrixStack) context;
        } else {
            try {
                for (java.lang.reflect.Method m : context.getClass().getMethods()) {
                    if (m.getReturnType() == MatrixStack.class) {
                        matrices = (MatrixStack) m.invoke(context);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (matrices == null) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            Vec3d cam = client.gameRenderer.getCamera().getPos();

            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

            RenderLayer lineLayer = RenderLayer.getLines();
            VertexConsumer buffer = immediate.getBuffer(lineLayer);

            for (Map.Entry<BlockPos, HighlightState> entry : highlights.entrySet()) {
                Color4f c = getColor(entry.getValue());
                drawBox(matrices, buffer, entry.getKey(), cam, c);
            }

            immediate.draw(lineLayer);

        } catch (Throwable e) {
        }
    }

    private void drawBox(MatrixStack matrices, VertexConsumer buffer, BlockPos pos, Vec3d cam, Color4f c) {
        matrices.push();
        matrices.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        Matrix4f model = matrices.peek().getPositionMatrix();

        float s = -0.005f;
        float e = 1.005f;

        int r = Math.max(0, Math.min(255, (int) (c.r * 255.0f)));
        int g = Math.max(0, Math.min(255, (int) (c.g * 255.0f)));
        int b = Math.max(0, Math.min(255, (int) (c.b * 255.0f)));
        int a = Math.max(0, Math.min(255, (int) (c.a * 255.0f)));

        line(buffer, model, s, s, s, e, s, s, r, g, b, a);
        line(buffer, model, e, s, s, e, s, e, r, g, b, a);
        line(buffer, model, e, s, e, s, s, e, r, g, b, a);
        line(buffer, model, s, s, e, s, s, s, r, g, b, a);

        line(buffer, model, s, e, s, e, e, s, r, g, b, a);
        line(buffer, model, e, e, s, e, e, e, r, g, b, a);
        line(buffer, model, e, e, e, s, e, e, r, g, b, a);
        line(buffer, model, s, e, e, s, e, s, r, g, b, a);

        line(buffer, model, s, s, s, s, e, s, r, g, b, a);
        line(buffer, model, e, s, s, e, e, s, r, g, b, a);
        line(buffer, model, e, s, e, e, e, e, r, g, b, a);
        line(buffer, model, s, s, e, s, e, e, r, g, b, a);

        matrices.pop();
    }

    private void line(VertexConsumer buffer, Matrix4f model, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        try {
            buffer.vertex(model, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
            buffer.vertex(model, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        } catch (Throwable t) {
            buffer.vertex(model, x1, y1, z1).color(r, g, b, a);
            buffer.vertex(model, x2, y2, z2).color(r, g, b, a);
        }
    }

    private Color4f getColor(HighlightState type) {
        return switch (type) {
            case UNFILLED -> Configs.HIGHLIGHT_COLOR_UNFILLED.getColor();
            case PARTIAL -> Configs.HIGHLIGHT_COLOR_PARTIAL.getColor();
            case OVERFILLED -> Configs.HIGHLIGHT_COLOR_OVERFILLED.getColor();
            case WRONG_ITEM -> Configs.HIGHLIGHT_COLOR_WRONG.getColor();
            case SATISFIED -> Configs.HIGHLIGHT_COLOR_SATISFIED.getColor();
            default -> Configs.HIGHLIGHT_COLOR_UNKNOWN.getColor();
        };
    }
}