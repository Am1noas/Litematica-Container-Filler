package com.mimicenzymes.litematicafiller.core;

import com.mimicenzymes.litematicafiller.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContainerHighlighter {
    private static final List<BlockPos> MISSING_LIST = new ArrayList<>();
    private static double lastX = 0, lastY = -100, lastZ = 0;
    private static int timer = 0;

    public static void onRender(WorldRenderContext context) {
        if (!Configs.ENABLE_MOD.getBooleanValue() || !Configs.HIGHLIGHT_CONTAINERS.getBooleanValue()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        timer++;
        if (timer > 20 || Math.abs(px - lastX) > 2 || Math.abs(py - lastY) > 2 || Math.abs(pz - lastZ) > 2) {
            rebuildCache(client, px, py, pz);
            lastX = px; lastY = py; lastZ = pz;
            timer = 0;
        }

        if (MISSING_LIST.isEmpty()) return;

        Vec3d cam = context.gameRenderer().getCamera().getPos();
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());
        Color4f c = Configs.HIGHLIGHT_COLOR.getColor();

        for (BlockPos pos : MISSING_LIST) renderBox(matrices, buffer, pos, cam, c);

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            if (Configs.HIGHLIGHT_XRAY.getBooleanValue()) GL11.glDisable(GL11.GL_DEPTH_TEST);
            immediate.draw(RenderLayer.getLines());
            if (Configs.HIGHLIGHT_XRAY.getBooleanValue()) GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    private static void rebuildCache(MinecraftClient client, double cx, double cy, double cz) {
        MISSING_LIST.clear();
        var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return;

        int r = Configs.RENDER_RADIUS.getIntegerValue();
        BlockPos center = BlockPos.ofFloored(cx, cy, cz);
        boolean syncLayer = Configs.SYNC_LITE_LAYER.getBooleanValue();
        boolean hideCompleted = Configs.HIDE_COMPLETED_CONTAINERS.getBooleanValue();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);

                    if (syncLayer && !fi.dy.masa.litematica.data.DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;

                    BlockState state = schematicWorld.getBlockState(pos);
                    if (state.isAir() || !state.hasBlockEntity()) continue;

                    Map<Integer, ItemStack> required = LitematicaContainerReader.getRequiredItems(pos, client.world.getRegistryManager());
                    if (required == null || required.isEmpty()) continue;

                    if (hideCompleted && RealContainerCache.isSatisfied(pos, required)) continue;

                    MISSING_LIST.add(pos.toImmutable());
                }
            }
        }
    }

    private static void renderBox(MatrixStack matrices, VertexConsumer buffer, BlockPos pos, Vec3d cam, Color4f c) {
        matrices.push();
        matrices.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        Matrix4f model = matrices.peek().getPositionMatrix();
        float s = -0.005f, e = 1.005f;
        line(buffer, model, s, s, s, e, s, s, c); line(buffer, model, e, s, s, e, s, e, c);
        line(buffer, model, e, s, e, s, s, e, c); line(buffer, model, s, s, e, s, s, s, c);
        line(buffer, model, s, e, s, e, e, s, c); line(buffer, model, e, e, s, e, e, e, c);
        line(buffer, model, e, e, e, s, e, e, c); line(buffer, model, s, e, e, s, e, s, c);
        line(buffer, model, s, s, s, s, e, s, c); line(buffer, model, e, s, s, e, e, s, c);
        line(buffer, model, e, s, e, e, e, e, c); line(buffer, model, s, s, e, s, e, e, c);
        matrices.pop();
    }

    private static void line(VertexConsumer buffer, Matrix4f model, float x1, float y1, float z1, float x2, float y2, float z2, Color4f c) {
        buffer.vertex(model, x1, y1, z1).color(c.r, c.g, c.b, c.a).normal(0, 0, 0);
        buffer.vertex(model, x2, y2, z2).color(c.r, c.g, c.b, c.a).normal(0, 0, 0);
    }
}