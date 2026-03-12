package com.mimicenzymes.schematichelper.core;

import com.mimicenzymes.schematichelper.config.FeatureConfigs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11; // 直接使用最底层的臭阴OpenGL无视版本

import java.util.ArrayList;
import java.util.List;

public class ContainerHighlighter {

    private static final List<BlockPos> MISSING_LIST = new ArrayList<>();
    private static double lastX = 0, lastY = -100, lastZ = 0;
    private static int scanCooldown = 0;

    public static void onRender(WorldRenderContext context) {
        if (!FeatureConfigs.ENABLE_MOD.getBooleanValue() || !FeatureConfigs.HIGHLIGHT_CONTAINERS.getBooleanValue()) {
            return;
        }

        if (SchematicWorldHandler.getSchematicWorld() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        if (Math.abs(px - lastX) > 1.0 || Math.abs(py - lastY) > 1.0 || Math.abs(pz - lastZ) > 1.0 || scanCooldown-- <= 0) {
            rebuildCache(client, px, py, pz);
            lastX = px; lastY = py; lastZ = pz;
            scanCooldown = 5; // 0.25 秒极速更新
        }

        if (MISSING_LIST.isEmpty()) return;

        MatrixStack matrices = context.matrices();
        Vec3d camPos = client.gameRenderer.getCamera().getPos();
        Color4f color = Color4f.fromColor(FeatureConfigs.HIGHLIGHT_COLOR.getIntegerValue());
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.getLines());
        for (BlockPos pos : MISSING_LIST) {
            renderBox(matrices, buffer, pos, camPos, color);
        }
        if (context.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            immediate.draw(RenderLayer.getLines());
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    private static void rebuildCache(MinecraftClient client, double cx, double cy, double cz) {
        MISSING_LIST.clear();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int r = 10;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (!SchematicContainerReader.getRequiredItems(mutablePos, client.world).isEmpty()) {
                        MISSING_LIST.add(mutablePos.toImmutable());
                    }
                }
            }
        }
    }

    private static void renderBox(MatrixStack matrices, VertexConsumer buffer, BlockPos pos, Vec3d cam, Color4f c) {
        matrices.push();
        matrices.translate((float)(pos.getX() - cam.x), (float)(pos.getY() - cam.y), (float)(pos.getZ() - cam.z));
        Matrix4f model = matrices.peek().getPositionMatrix();
        float s = -0.002f, e = 1.002f;
        drawLines(buffer, model, s, e, c);
        matrices.pop();
    }

    private static void drawLines(VertexConsumer buffer, Matrix4f model, float s, float e, Color4f c) {
        line(buffer, model, s, s, s, e, s, s, c); line(buffer, model, e, s, s, e, s, e, c);
        line(buffer, model, e, s, e, s, s, e, c); line(buffer, model, s, s, e, s, s, s, c);
        line(buffer, model, s, e, s, e, e, s, c); line(buffer, model, e, e, s, e, e, e, c);
        line(buffer, model, e, e, e, s, e, e, c); line(buffer, model, s, e, e, s, e, s, c);
        line(buffer, model, s, s, s, s, e, s, c); line(buffer, model, e, s, s, e, e, s, c);
        line(buffer, model, e, s, e, e, e, e, c); line(buffer, model, s, s, e, s, e, e, c);
    }

    private static void line(VertexConsumer buffer, Matrix4f model, float x1, float y1, float z1, float x2, float y2, float z2, Color4f c) {
        buffer.vertex(model, x1, y1, z1).color(c.r, c.g, c.b, c.a).normal(0, 1, 0);
        buffer.vertex(model, x2, y2, z2).color(c.r, c.g, c.b, c.a).normal(0, 1, 0);
    }
}