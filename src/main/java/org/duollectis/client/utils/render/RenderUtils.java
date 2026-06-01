package org.duollectis.client.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import static org.duollectis.client.utils.MC.mc;


/**
 * Центральная утилита рендеринга.
 * Объединяет примитивы мирового пространства и post-processing эффекты.
 *
 * <p><b>Post-processing (применяются в RenderEvent):</b>
 * <ul>
 *   <li>{@link #applyBloom} — свечение ярких областей</li>
 *   <li>{@link #applyChromatic} — хроматическая аберрация</li>
 *   <li>{@link #applyVignette} — затемнение краёв экрана</li>
 *   <li>{@link #applyGrayscale} — обесцвечивание</li>
 *   <li>{@link #applyColorGrade} — цветокоррекция</li>
 * </ul>
 *
 * <p><b>Примитивы мирового пространства (применяются в RenderEvent):</b>
 * <ul>
 *   <li>{@link #drawLine} — отрезок</li>
 *   <li>{@link #drawLinesBox} — каркас блока</li>
 *   <li>{@link #drawFilledBox} — заполненный AABB</li>
 * </ul>
 *
 * <p><b>Примитивы экранного пространства (применяются в HUD-рендере):</b>
 * <ul>
 *   <li>{@link #drawText(DrawContext, String, float, float, int)} — текст с тенью</li>
 *   <li>{@link #drawTextCentered} — текст по центру</li>
 *   <li>{@link #drawRect} — закрашенный прямоугольник</li>
 *   <li>{@link #drawRectOutline} — контур прямоугольника</li>
 * </ul>
 */
public final class RenderUtils {

	private static final float DEFAULT_LINE_WIDTH = 4f;

	private static final RenderPipeline BOX_LINES_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines"))
			.withLocation("duollectis/box_lines")
			.build()
	);

	private static final RenderPipeline BOX_LINES_NO_DEPTH_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withLocation("duollectis/box_lines_no_depth")
			.build()
	);

	private static final RenderPipeline BOX_LINES_RAINBOW_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines_rainbow"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines_rainbow"))
			.withLocation("duollectis/box_lines_rainbow")
			.build()
	);

	private static final RenderPipeline BOX_LINES_RAINBOW_NO_DEPTH_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines_rainbow"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines_rainbow"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withLocation("duollectis/box_lines_rainbow_no_depth")
			.build()
	);

	private static final RenderPipeline BOX_LINES_PULSE_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines_pulse"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines_pulse"))
			.withLocation("duollectis/box_lines_pulse")
			.build()
	);

	private static final RenderPipeline BOX_LINES_PULSE_NO_DEPTH_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withVertexShader(Identifier.of("duollectis", "core/box_lines_pulse"))
			.withFragmentShader(Identifier.of("duollectis", "core/box_lines_pulse"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withLocation("duollectis/box_lines_pulse_no_depth")
			.build()
	);

	private static final RenderLayer BOX_LINES_LAYER = RenderLayer.of(
		"duollectis_box_lines",
		RenderSetup.builder(BOX_LINES_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer BOX_LINES_THROUGH_WALLS_LAYER = RenderLayer.of(
		"duollectis_box_lines_through_walls",
		RenderSetup.builder(BOX_LINES_NO_DEPTH_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer BOX_LINES_RAINBOW_LAYER = RenderLayer.of(
		"duollectis_box_lines_rainbow",
		RenderSetup.builder(BOX_LINES_RAINBOW_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer BOX_LINES_RAINBOW_THROUGH_WALLS_LAYER = RenderLayer.of(
		"duollectis_box_lines_rainbow_through_walls",
		RenderSetup.builder(BOX_LINES_RAINBOW_NO_DEPTH_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer BOX_LINES_PULSE_LAYER = RenderLayer.of(
		"duollectis_box_lines_pulse",
		RenderSetup.builder(BOX_LINES_PULSE_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer BOX_LINES_PULSE_THROUGH_WALLS_LAYER = RenderLayer.of(
		"duollectis_box_lines_pulse_through_walls",
		RenderSetup.builder(BOX_LINES_PULSE_NO_DEPTH_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderPipeline FILLED_BOX_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
			.withLocation("duollectis/filled_box")
			.build()
	);

	private static final RenderPipeline FILLED_BOX_NO_DEPTH_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
			.withLocation("duollectis/filled_box_no_depth")
			.build()
	);

	private static final RenderLayer FILLED_BOX_LAYER = RenderLayer.of(
		"duollectis_filled_box",
		RenderSetup.builder(FILLED_BOX_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final RenderLayer FILLED_BOX_THROUGH_WALLS_LAYER = RenderLayer.of(
		"duollectis_filled_box_through_walls",
		RenderSetup.builder(FILLED_BOX_NO_DEPTH_PIPELINE)
			.layeringTransform(LayeringTransform.NO_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);

	private static final BloomRenderer BLOOM = new BloomRenderer();
	private static final ChromaticAberrationRenderer CHROMATIC = new ChromaticAberrationRenderer();
	private static final VignetteRenderer VIGNETTE = new VignetteRenderer();
	private static final GrayscaleRenderer GRAYSCALE = new GrayscaleRenderer();
	private static final ColorGradeRenderer COLOR_GRADE = new ColorGradeRenderer();

	private RenderUtils() {}

	// -------------------------------------------------------------------------
	// Post-processing
	// -------------------------------------------------------------------------

	/**
	 * Применяет bloom post-effect к главному framebuffer.
	 *
	 * @param threshold порог яркости (0.0–1.0)
	 * @param intensity интенсивность свечения
	 * @param radius    радиус размытия в пикселях
	 */
	public static void applyBloom(float threshold, float intensity, float radius) {
		BLOOM.apply(threshold, intensity, radius);
	}

	/** Освобождает ресурсы bloom-процессора. Вызывать при выключении модуля. */
	public static void closeBloom() {
		BLOOM.close();
	}

	/**
	 * Применяет хроматическую аберрацию к главному framebuffer.
	 *
	 * @param strength сила расщепления каналов (0.1–10.0)
	 */
	public static void applyChromatic(float strength) {
		CHROMATIC.apply(strength);
	}

	/** Освобождает ресурсы процессора хроматической аберрации. */
	public static void closeChromatic() {
		CHROMATIC.close();
	}

	/**
	 * Применяет виньетку к главному framebuffer.
	 *
	 * @param strength  сила затемнения краёв (0.5–2.0)
	 * @param softness  мягкость перехода (0.1–0.8)
	 * @param roundness округлость виньетки (1.0 — круглая, >1 — вытянутая)
	 */
	public static void applyVignette(float strength, float softness, float roundness) {
		VIGNETTE.apply(strength, softness, roundness);
	}

	/** Освобождает ресурсы процессора виньетки. */
	public static void closeVignette() {
		VIGNETTE.close();
	}

	/**
	 * Применяет обесцвечивание к главному framebuffer.
	 *
	 * @param strength сила эффекта (0.0 — нет, 1.0 — полный grayscale)
	 */
	public static void applyGrayscale(float strength) {
		GRAYSCALE.apply(strength);
	}

	/** Освобождает ресурсы процессора grayscale. */
	public static void closeGrayscale() {
		GRAYSCALE.close();
	}

	/**
	 * Применяет цветокоррекцию к главному framebuffer.
	 *
	 * @param shadows    тонировка тёмных областей (RGB смещение)
	 * @param midtones   тонировка полутонов
	 * @param highlights тонировка светлых областей
	 * @param contrast   контраст (-1.0..1.0)
	 * @param saturation насыщенность (-1.0..1.0)
	 * @param exposure   экспозиция в стопах (-2.0..2.0)
	 */
	public static void applyColorGrade(
		Vector3f shadows,
		Vector3f midtones,
		Vector3f highlights,
		float contrast,
		float saturation,
		float exposure
	) {
		COLOR_GRADE.apply(shadows, midtones, highlights, contrast, saturation, exposure);
	}

	/** Освобождает ресурсы процессора цветокоррекции. */
	public static void closeColorGrade() {
		COLOR_GRADE.close();
	}

	// -------------------------------------------------------------------------
	// Мировые примитивы
	// -------------------------------------------------------------------------

	/**
	 * Рисует произвольный отрезок между двумя точками в мировых координатах.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param x1,y1,z1     начало отрезка
	 * @param x2,y2,z2     конец отрезка
	 * @param color        цвет в формате ARGB
	 * @param width        ширина линии
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawLine(
		MatrixStack matrices,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		int color,
		float width,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		float a = (color >> 24 & 255) / 255.0f;

		if (a == 0.0f) {
			return;
		}

		float r = (color >> 16 & 255) / 255.0f;
		float g = (color >> 8 & 255) / 255.0f;
		float b = (color & 255) / 255.0f;

		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		float fx1 = (float) (x1 - cam.x);
		float fy1 = (float) (y1 - cam.y);
		float fz1 = (float) (z1 - cam.z);
		float fx2 = (float) (x2 - cam.x);
		float fy2 = (float) (y2 - cam.y);
		float fz2 = (float) (z2 - cam.z);

		RenderLayer layer = throughWalls ? BOX_LINES_THROUGH_WALLS_LAYER : BOX_LINES_LAYER;
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(layer);

		line(matrices.peek(), buffer, width, fx1, fy1, fz1, fx2, fy2, fz2, r, g, b, a);

		provider.draw(layer);
	}

	/** Рисует каркас блока с шириной по умолчанию без depth test. */
	public static void drawLinesBox(MatrixStack matrices, BlockPos pos, int color) {
		drawLinesBox(matrices, pos, color, DEFAULT_LINE_WIDTH, false);
	}

	/** Рисует каркас блока с шириной по умолчанию. */
	public static void drawLinesBox(MatrixStack matrices, BlockPos pos, int color, boolean throughWalls) {
		drawLinesBox(matrices, pos, color, DEFAULT_LINE_WIDTH, throughWalls);
	}

	/**
	 * Рисует каркас произвольного AABB в мировых координатах.
	 * Удобен для рендера bounding box энтити или любого произвольного объёма.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param box          AABB в мировых координатах
	 * @param color        цвет в формате ARGB
	 * @param width        ширина рёбер
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawLinesBoxFromBox(
		MatrixStack matrices,
		Box box,
		int color,
		float width,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		float a = (color >> 24 & 255) / 255.0f;

		if (a == 0.0f) {
			return;
		}

		float r = (color >> 16 & 255) / 255.0f;
		float g = (color >> 8 & 255) / 255.0f;
		float b = (color & 255) / 255.0f;

		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		float x1 = (float) (box.minX - cam.x);
		float y1 = (float) (box.minY - cam.y);
		float z1 = (float) (box.minZ - cam.z);
		float x2 = (float) (box.maxX - cam.x);
		float y2 = (float) (box.maxY - cam.y);
		float z2 = (float) (box.maxZ - cam.z);

		RenderLayer layer = throughWalls ? BOX_LINES_THROUGH_WALLS_LAYER : BOX_LINES_LAYER;
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(layer);
		MatrixStack.Entry entry = matrices.peek();

		line(entry, buffer, width, x1, y1, z1, x2, y1, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z1, x2, y1, z2, r, g, b, a);
		line(entry, buffer, width, x2, y1, z2, x1, y1, z2, r, g, b, a);
		line(entry, buffer, width, x1, y1, z2, x1, y1, z1, r, g, b, a);

		line(entry, buffer, width, x1, y2, z1, x2, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y2, z1, x2, y2, z2, r, g, b, a);
		line(entry, buffer, width, x2, y2, z2, x1, y2, z2, r, g, b, a);
		line(entry, buffer, width, x1, y2, z2, x1, y2, z1, r, g, b, a);

		line(entry, buffer, width, x1, y1, z1, x1, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z1, x2, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z2, x2, y2, z2, r, g, b, a);
		line(entry, buffer, width, x1, y1, z2, x1, y2, z2, r, g, b, a);

		provider.draw(layer);
	}

	/**
	 * Рисует интерполированный bounding box энтити с учётом tickDelta.
	 * Позиция плавно интерполируется между прошлым и текущим тиком,
	 * что устраняет дёрганье при рендере движущихся энтити.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param entity       целевая энтити
	 * @param tickDelta    дробная часть тика (0.0–1.0) из {@code RenderTickCounter}
	 * @param color        цвет каркаса ARGB
	 * @param width        ширина рёбер
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawEntityBox(
		MatrixStack matrices,
		Entity entity,
		float tickDelta,
		int color,
		float width,
		boolean throughWalls
	) {
		Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
		Box box = entity.getBoundingBox();

		double dx = lerpedPos.x - entity.getX();
		double dy = lerpedPos.y - entity.getY();
		double dz = lerpedPos.z - entity.getZ();

		drawLinesBoxFromBox(
			matrices,
			box.offset(dx, dy, dz),
			color,
			width,
			throughWalls
		);
	}

	/**
		* Рисует заполненный bounding box энтити с интерполяцией позиции.
		*
		* @param matrices     MatrixStack из {@code RenderEvent}
		* @param entity       целевая энтити
		* @param tickDelta    дробная часть тика (0.0–1.0)
		* @param fillColor    цвет заливки ARGB
		* @param outlineColor цвет каркаса ARGB
		* @param width        ширина рёбер каркаса
		* @param throughWalls если {@code true} — рендерится сквозь стены
		*/
	public static void drawEntityFilledBox(
		MatrixStack matrices,
		Entity entity,
		float tickDelta,
		int fillColor,
		int outlineColor,
		float width,
		boolean throughWalls
	) {
		Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
		Box box = entity.getBoundingBox();

		double dx = lerpedPos.x - entity.getX();
		double dy = lerpedPos.y - entity.getY();
		double dz = lerpedPos.z - entity.getZ();

		Box lerpedBox = box.offset(dx, dy, dz);

		drawFilledBox(
			matrices,
			lerpedBox.minX, lerpedBox.minY, lerpedBox.minZ,
			lerpedBox.maxX, lerpedBox.maxY, lerpedBox.maxZ,
			fillColor,
			outlineColor,
			width,
			throughWalls
		);
	}


	/**
	 * Рисует интерполированный bounding box энтити с радужным шейдером.
	 * Цвет анимируется по HSV-кругу в зависимости от {@code GameTime} и высоты вершины.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param entity       целевая энтити
	 * @param tickDelta    дробная часть тика (0.0–1.0)
	 * @param width        ширина рёбер
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawEntityBoxRainbow(
		MatrixStack matrices,
		Entity entity,
		float tickDelta,
		float width,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
		Box box = entity.getBoundingBox();

		double dx = lerpedPos.x - entity.getX();
		double dy = lerpedPos.y - entity.getY();
		double dz = lerpedPos.z - entity.getZ();

		Box lerpedBox = box.offset(dx, dy, dz);
		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		float x1 = (float) (lerpedBox.minX - cam.x);
		float y1 = (float) (lerpedBox.minY - cam.y);
		float z1 = (float) (lerpedBox.minZ - cam.z);
		float x2 = (float) (lerpedBox.maxX - cam.x);
		float y2 = (float) (lerpedBox.maxY - cam.y);
		float z2 = (float) (lerpedBox.maxZ - cam.z);

		RenderLayer layer = throughWalls ? BOX_LINES_RAINBOW_THROUGH_WALLS_LAYER : BOX_LINES_RAINBOW_LAYER;
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(layer);
		MatrixStack.Entry entry = matrices.peek();

		// Цвет передаётся белым — шейдер сам вычисляет радугу через GameTime и worldY
		line(entry, buffer, width, x1, y1, z1, x2, y1, z1, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y1, z1, x2, y1, z2, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y1, z2, x1, y1, z2, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x1, y1, z2, x1, y1, z1, 1f, 1f, 1f, 1f);

		line(entry, buffer, width, x1, y2, z1, x2, y2, z1, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y2, z1, x2, y2, z2, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y2, z2, x1, y2, z2, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x1, y2, z2, x1, y2, z1, 1f, 1f, 1f, 1f);

		line(entry, buffer, width, x1, y1, z1, x1, y2, z1, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y1, z1, x2, y2, z1, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x2, y1, z2, x2, y2, z2, 1f, 1f, 1f, 1f);
		line(entry, buffer, width, x1, y1, z2, x1, y2, z2, 1f, 1f, 1f, 1f);

		provider.draw(layer);
	}

	/**
	 * Рисует интерполированный bounding box энтити с пульсирующим шейдером.
	 * Альфа-канал осциллирует по синусоиде через {@code GameTime}.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param entity       целевая энтити
	 * @param tickDelta    дробная часть тика (0.0–1.0)
	 * @param color        базовый цвет ARGB (RGB используется, альфа модулируется шейдером)
	 * @param width        ширина рёбер
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawEntityBoxPulse(
		MatrixStack matrices,
		Entity entity,
		float tickDelta,
		int color,
		float width,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		float r = (color >> 16 & 255) / 255.0f;
		float g = (color >> 8 & 255) / 255.0f;
		float b = (color & 255) / 255.0f;

		Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
		Box box = entity.getBoundingBox();

		double dx = lerpedPos.x - entity.getX();
		double dy = lerpedPos.y - entity.getY();
		double dz = lerpedPos.z - entity.getZ();

		Box lerpedBox = box.offset(dx, dy, dz);
		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		float x1 = (float) (lerpedBox.minX - cam.x);
		float y1 = (float) (lerpedBox.minY - cam.y);
		float z1 = (float) (lerpedBox.minZ - cam.z);
		float x2 = (float) (lerpedBox.maxX - cam.x);
		float y2 = (float) (lerpedBox.maxY - cam.y);
		float z2 = (float) (lerpedBox.maxZ - cam.z);

		RenderLayer layer = throughWalls ? BOX_LINES_PULSE_THROUGH_WALLS_LAYER : BOX_LINES_PULSE_LAYER;
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(layer);
		MatrixStack.Entry entry = matrices.peek();

		// Альфа передаётся как 1.0 — шейдер сам модулирует её через sin(GameTime)
		line(entry, buffer, width, x1, y1, z1, x2, y1, z1, r, g, b, 1f);
		line(entry, buffer, width, x2, y1, z1, x2, y1, z2, r, g, b, 1f);
		line(entry, buffer, width, x2, y1, z2, x1, y1, z2, r, g, b, 1f);
		line(entry, buffer, width, x1, y1, z2, x1, y1, z1, r, g, b, 1f);

		line(entry, buffer, width, x1, y2, z1, x2, y2, z1, r, g, b, 1f);
		line(entry, buffer, width, x2, y2, z1, x2, y2, z2, r, g, b, 1f);
		line(entry, buffer, width, x2, y2, z2, x1, y2, z2, r, g, b, 1f);
		line(entry, buffer, width, x1, y2, z2, x1, y2, z1, r, g, b, 1f);

		line(entry, buffer, width, x1, y1, z1, x1, y2, z1, r, g, b, 1f);
		line(entry, buffer, width, x2, y1, z1, x2, y2, z1, r, g, b, 1f);
		line(entry, buffer, width, x2, y1, z2, x2, y2, z2, r, g, b, 1f);
		line(entry, buffer, width, x1, y1, z2, x1, y2, z2, r, g, b, 1f);

		provider.draw(layer);
	}

	/**
	 * Рисует каркас блока (12 рёбер) по позиции блока.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param pos          позиция блока в мировых координатах
	 * @param color        цвет в формате ARGB
	 * @param width        ширина ребра
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawLinesBox(
		MatrixStack matrices,
		BlockPos pos,
		int color,
		float width,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		float a = (color >> 24 & 255) / 255.0f;

		if (a == 0.0f) {
			return;
		}

		float r = (color >> 16 & 255) / 255.0f;
		float g = (color >> 8 & 255) / 255.0f;
		float b = (color & 255) / 255.0f;

		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		double ox = pos.getX() - cam.x;
		double oy = pos.getY() - cam.y;
		double oz = pos.getZ() - cam.z;

		RenderLayer layer = throughWalls ? BOX_LINES_THROUGH_WALLS_LAYER : BOX_LINES_LAYER;
		VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer buffer = provider.getBuffer(layer);

		float x1 = (float) ox;
		float y1 = (float) oy;
		float z1 = (float) oz;
		float x2 = (float) (ox + 1);
		float y2 = (float) (oy + 1);
		float z2 = (float) (oz + 1);

		MatrixStack.Entry entry = matrices.peek();

		line(entry, buffer, width, x1, y1, z1, x2, y1, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z1, x2, y1, z2, r, g, b, a);
		line(entry, buffer, width, x2, y1, z2, x1, y1, z2, r, g, b, a);
		line(entry, buffer, width, x1, y1, z2, x1, y1, z1, r, g, b, a);

		line(entry, buffer, width, x1, y2, z1, x2, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y2, z1, x2, y2, z2, r, g, b, a);
		line(entry, buffer, width, x2, y2, z2, x1, y2, z2, r, g, b, a);
		line(entry, buffer, width, x1, y2, z2, x1, y2, z1, r, g, b, a);

		line(entry, buffer, width, x1, y1, z1, x1, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z1, x2, y2, z1, r, g, b, a);
		line(entry, buffer, width, x2, y1, z2, x2, y2, z2, r, g, b, a);
		line(entry, buffer, width, x1, y1, z2, x1, y2, z2, r, g, b, a);

		provider.draw(layer);
	}

	/**
	 * Рисует заполненный AABB (ось-выровненный параллелепипед) в мировых координатах.
	 * Использует полупрозрачный fill + каркас поверх.
	 *
	 * @param matrices     MatrixStack из {@code RenderEvent}
	 * @param x1,y1,z1     минимальный угол
	 * @param x2,y2,z2     максимальный угол
	 * @param fillColor    цвет заливки ARGB
	 * @param outlineColor цвет каркаса ARGB
	 * @param lineWidth    ширина рёбер каркаса
	 * @param throughWalls если {@code true} — рендерится сквозь стены
	 */
	public static void drawFilledBox(
		MatrixStack matrices,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		int fillColor,
		int outlineColor,
		float lineWidth,
		boolean throughWalls
	) {
		if (mc.player == null) {
			return;
		}

		Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

		float fx1 = (float) (x1 - cam.x);
		float fy1 = (float) (y1 - cam.y);
		float fz1 = (float) (z1 - cam.z);
		float fx2 = (float) (x2 - cam.x);
		float fy2 = (float) (y2 - cam.y);
		float fz2 = (float) (z2 - cam.z);

		float fa = (fillColor >> 24 & 255) / 255.0f;
		float fr = (fillColor >> 16 & 255) / 255.0f;
		float fg = (fillColor >> 8 & 255) / 255.0f;
		float fb = (fillColor & 255) / 255.0f;

		if (fa > 0.0f) {
			RenderLayer fillLayer = throughWalls ? FILLED_BOX_THROUGH_WALLS_LAYER : FILLED_BOX_LAYER;
			VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
			VertexConsumer fill = provider.getBuffer(fillLayer);
			MatrixStack.Entry entry = matrices.peek();

			quad(entry, fill, fx1, fy1, fz1, fx2, fy1, fz1, fx2, fy1, fz2, fx1, fy1, fz2, fr, fg, fb, fa);
			quad(entry, fill, fx1, fy2, fz1, fx2, fy2, fz1, fx2, fy2, fz2, fx1, fy2, fz2, fr, fg, fb, fa);
			quad(entry, fill, fx1, fy1, fz1, fx1, fy2, fz1, fx2, fy2, fz1, fx2, fy1, fz1, fr, fg, fb, fa);
			quad(entry, fill, fx1, fy1, fz2, fx1, fy2, fz2, fx2, fy2, fz2, fx2, fy1, fz2, fr, fg, fb, fa);
			quad(entry, fill, fx1, fy1, fz1, fx1, fy2, fz1, fx1, fy2, fz2, fx1, fy1, fz2, fr, fg, fb, fa);
			quad(entry, fill, fx2, fy1, fz1, fx2, fy2, fz1, fx2, fy2, fz2, fx2, fy1, fz2, fr, fg, fb, fa);

			provider.draw(fillLayer);
		}

		float oa = (outlineColor >> 24 & 255) / 255.0f;

		if (oa > 0.0f) {
			float or_ = (outlineColor >> 16 & 255) / 255.0f;
			float og = (outlineColor >> 8 & 255) / 255.0f;
			float ob = (outlineColor & 255) / 255.0f;

			RenderLayer outlineLayer = throughWalls ? BOX_LINES_THROUGH_WALLS_LAYER : BOX_LINES_LAYER;
			VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
			VertexConsumer buffer = provider.getBuffer(outlineLayer);
			MatrixStack.Entry entry = matrices.peek();

			line(entry, buffer, lineWidth, fx1, fy1, fz1, fx2, fy1, fz1, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy1, fz1, fx2, fy1, fz2, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy1, fz2, fx1, fy1, fz2, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx1, fy1, fz2, fx1, fy1, fz1, or_, og, ob, oa);

			line(entry, buffer, lineWidth, fx1, fy2, fz1, fx2, fy2, fz1, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy2, fz1, fx2, fy2, fz2, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy2, fz2, fx1, fy2, fz2, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx1, fy2, fz2, fx1, fy2, fz1, or_, og, ob, oa);

			line(entry, buffer, lineWidth, fx1, fy1, fz1, fx1, fy2, fz1, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy1, fz1, fx2, fy2, fz1, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx2, fy1, fz2, fx2, fy2, fz2, or_, og, ob, oa);
			line(entry, buffer, lineWidth, fx1, fy1, fz2, fx1, fy2, fz2, or_, og, ob, oa);

			provider.draw(outlineLayer);
		}
	}

	// -------------------------------------------------------------------------
	// Экранные примитивы (HUD)
	// -------------------------------------------------------------------------

	/**
	 * Рисует закрашенный прямоугольник в экранных координатах.
	 *
	 * @param ctx    DrawContext из HUD-рендера (InGameHud, Screen и т.д.)
	 * @param x      левый край
	 * @param y      верхний край
	 * @param width  ширина
	 * @param height высота
	 * @param color  цвет ARGB
	 */
	public static void drawRect(DrawContext ctx, int x, int y, int width, int height, int color) {
		ctx.fill(x, y, x + width, y + height, color);
	}

	/**
	 * Рисует контур прямоугольника в экранных координатах.
	 *
	 * @param ctx         DrawContext из HUD-рендера
	 * @param x           левый край
	 * @param y           верхний край
	 * @param width       ширина
	 * @param height      высота
	 * @param borderWidth толщина рамки в пикселях
	 * @param color       цвет ARGB
	 */
	public static void drawRectOutline(
		DrawContext ctx,
		int x, int y,
		int width, int height,
		int borderWidth,
		int color
	) {
		ctx.fill(x, y, x + width, y + borderWidth, color);
		ctx.fill(x, y + height - borderWidth, x + width, y + height, color);
		ctx.fill(x, y + borderWidth, x + borderWidth, y + height - borderWidth, color);
		ctx.fill(x + width - borderWidth, y + borderWidth, x + width, y + height - borderWidth, color);
	}

	/**
	 * Рисует строку текста с тенью в экранных координатах.
	 *
	 * @param ctx   DrawContext из HUD-рендера
	 * @param text  текст для отображения
	 * @param x     позиция X
	 * @param y     позиция Y
	 * @param color цвет ARGB
	 */
	public static void drawText(DrawContext ctx, String text, float x, float y, int color) {
		ctx.drawTextWithShadow(mc.textRenderer, text, (int) x, (int) y, color);
	}

	/**
	 * Рисует строку текста по центру заданной X-позиции с тенью.
	 *
	 * @param ctx     DrawContext из HUD-рендера
	 * @param text    текст для отображения
	 * @param centerX центральная X-позиция
	 * @param y       позиция Y
	 * @param color   цвет ARGB
	 */
	public static void drawTextCentered(DrawContext ctx, String text, float centerX, float y, int color) {
		float x = centerX - mc.textRenderer.getWidth(text) / 2.0f;
		drawText(ctx, text, x, y, color);
	}

	/**
	 * Рисует {@link Text} компонент с тенью в экранных координатах.
	 *
	 * @param ctx   DrawContext из HUD-рендера
	 * @param text  компонент текста
	 * @param x     позиция X
	 * @param y     позиция Y
	 * @param color цвет ARGB
	 */
	public static void drawText(DrawContext ctx, Text text, float x, float y, int color) {
		ctx.drawTextWithShadow(mc.textRenderer, text, (int) x, (int) y, color);
	}

	// -------------------------------------------------------------------------
	// Приватные вспомогательные методы
	// -------------------------------------------------------------------------

	private static void line(
		MatrixStack.Entry entry,
		VertexConsumer buffer,
		float lineWidth,
		float x1, float y1, float z1,
		float x2, float y2, float z2,
		float r, float g, float b, float a
	) {
		Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
		buffer.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, normal).lineWidth(lineWidth);
		buffer.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, normal).lineWidth(lineWidth);
	}

	private static void quad(
		MatrixStack.Entry entry,
		VertexConsumer buffer,
		float x1, float y1, float z1,
		float x2, float y2, float z2,
		float x3, float y3, float z3,
		float x4, float y4, float z4,
		float r, float g, float b, float a
	) {
		buffer.vertex(entry, x1, y1, z1).color(r, g, b, a);
		buffer.vertex(entry, x2, y2, z2).color(r, g, b, a);
		buffer.vertex(entry, x3, y3, z3).color(r, g, b, a);
		buffer.vertex(entry, x4, y4, z4).color(r, g, b, a);
	}

}
