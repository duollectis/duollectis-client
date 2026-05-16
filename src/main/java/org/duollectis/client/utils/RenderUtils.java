package org.duollectis.client.utils;

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

	// Новый метод, принимающий BlockPos
	public static void drawLinesBox(MatrixStack matrices, BlockPos pos, int color) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;

		// 1. Создаем коробку 1x1x1 на координатах этого блока
		Box blockBox = new Box(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
		);

		// 2. Получаем позицию камеры, чтобы убрать "тряску" и привязать к миру
		Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

		// 3. Сдвигаем коробку относительно камеры
		Box renderedBox = blockBox.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		// 4. Вызываем твой старый рабочий метод, который рисует линии по Box
		drawLinesBox(matrices, renderedBox, color);
	}

	public static void drawLinesBox(MatrixStack matrices, Box box, int color) {
		MinecraftClient client = MinecraftClient.getInstance();
		VertexConsumerProvider.Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();

		// Используем RenderLayer.getLines(), чтобы рисовать сетку
		VertexConsumer buffer = provider.getBuffer(RenderLayers.lines());
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		// Разбиваем HEX цвет на каналы (ARGB)
		float a = (float) (color >> 24 & 255) / 255.0F;
		float r = (float) (color >> 16 & 255) / 255.0F;
		float g = (float) (color >> 8 & 255) / 255.0F;
		float b = (float) (color & 255) / 255.0F;
		if (a == 0.0F)
			a = 1.0F;

		// Приводим координаты бокса к float для удобства рендера
		float x1 = (float) box.minX;
		float y1 = (float) box.minY;
		float z1 = (float) box.minZ;
		float x2 = (float) box.maxX;
		float y2 = (float) box.maxY;
		float z2 = (float) box.maxZ;

		// Рисуем 12 ребер куба вручную (для линий важен шаг попарно от точки к точке)
		// Нижний квадрат
		line(matrix, buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
		line(matrix, buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
		line(matrix, buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
		line(matrix, buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);

		// Верхний квадрат
		line(matrix, buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
		line(matrix, buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
		line(matrix, buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
		line(matrix, buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);

		// Вертикальные стойки
		line(matrix, buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
		line(matrix, buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
		line(matrix, buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
		line(matrix, buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);

		// Отправляем буфер на отрисовку видеокарте
		provider.draw(RenderLayers.lines());
	}

	// Вспомогательный мини-метод для создания одной линии (двух вершин)
	private static void line(
			Matrix4f matrix,
			VertexConsumer buffer,
			float x1,
			float y1,
			float z1,
			float x2,
			float y2,
			float z2,
			float r,
			float g,
			float b,
			float a
	) {
		buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0).lineWidth(LINE_WIDTH);
		buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0).lineWidth(LINE_WIDTH);
	}
}