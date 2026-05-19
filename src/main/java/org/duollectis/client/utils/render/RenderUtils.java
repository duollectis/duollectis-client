package org.duollectis.client.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class RenderUtils {

    private static final float LINE_WIDTH = 8f;
    private static final float ALPHA_OPAQUE = 1.0f;

    public static void drawLinesBox(MatrixStack matrices, BlockPos pos, int color) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        Box blockBox = new Box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );
        Box renderedBox = blockBox.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        drawLinesBox(matrices, renderedBox, color);
    }

    public static void drawLinesBox(MatrixStack matrices, Box box, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = provider.getBuffer(RenderLayers.lines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float a = (float) (color >> 24 & 255) / 255.0f;
        float r = (float) (color >> 16 & 255) / 255.0f;
        float g = (float) (color >> 8 & 255) / 255.0f;
        float b = (float) (color & 255) / 255.0f;

        if (a == 0.0f) {
            a = ALPHA_OPAQUE;
        }

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        line(matrix, buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(matrix, buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(matrix, buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(matrix, buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);

        line(matrix, buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(matrix, buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(matrix, buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(matrix, buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);

        line(matrix, buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(matrix, buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(matrix, buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(matrix, buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);

        provider.draw(RenderLayers.lines());
    }

    private static void line(
            Matrix4f matrix,
            VertexConsumer buffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0).lineWidth(LINE_WIDTH);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0).lineWidth(LINE_WIDTH);
    }
}
