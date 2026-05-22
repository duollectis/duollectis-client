package org.duollectis.client.utils.render;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import static org.duollectis.client.utils.MC.mc;

/** Утилиты для рендеринга отладочных примитивов в мировом пространстве. */
public final class RenderUtils {

	private static final float LINE_WIDTH = 8f;
	private static final float ALPHA_OPAQUE = 1.0f;

	private RenderUtils() {}

	/**
	 * Рисует каркас блока (12 рёбер) по позиции блока с учётом смещения камеры.
	 * Шейдер lines получает view/projection через RenderSystem — передаём identity-матрицу,
	 * координаты вершин уже смещены на -cameraPos.
	 *
	 * @param matrix identity-матрица из {@code RenderEvent.getModelView()}
	 * @param pos    позиция блока в мировых координатах
	 * @param color  цвет в формате ARGB (если alpha == 0, используется непрозрачный)
	 */
	public static void drawLinesBox(Matrix4f matrix, BlockPos pos, int color) {
		if (mc.player == null) {
			return;
		}

		Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
		Box blockBox = new Box(
			pos.getX(), pos.getY(), pos.getZ(),
			pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
		);
		Box renderedBox = blockBox.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		drawLinesBox(matrix, renderedBox, color);
	}

	/**
	 * Рисует каркас произвольного AABB (12 рёбер) в уже смещённых координатах.
	 *
	 * @param matrix identity-матрица из {@code RenderEvent.getModelView()}
	 * @param box    ограничивающий параллелепипед
	 * @param color  цвет в формате ARGB
	 */
	public static void drawLinesBox(Matrix4f matrix, Box box, int color) {
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(RenderLayers.lines());

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
