package org.duollectis.client.utils.render;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.gl.PostEffectPipeline;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.UniformValue;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.duollectis.client.utils.MC.mc;

/**
 * Применяет цветокоррекцию к главному framebuffer.
 * Поддерживает раздельную тонировку теней/полутонов/светов,
 * контраст, насыщенность и экспозицию.
 */
@Slf4j
public final class ColorGradeRenderer {

	private static final Identifier VERTEX_SHADER = Identifier.ofVanilla("core/screenquad");
	private static final Identifier SHADER_COLOR_GRADE = Identifier.of("duollectis", "post/color_grade");
	private static final Identifier TARGET_MAIN = Identifier.ofVanilla("main");
	private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(TARGET_MAIN);

	private PostEffectProcessor processor;
	private ProjectionMatrix2 projectionMatrix;

	private Vector3f cachedShadows;
	private Vector3f cachedMidtones;
	private Vector3f cachedHighlights;
	private float cachedContrast = Float.NaN;
	private float cachedSaturation = Float.NaN;
	private float cachedExposure = Float.NaN;

	/**
	 * Применяет цветокоррекцию к главному framebuffer.
	 *
	 * @param shadows    тонировка тёмных областей (RGB смещение, обычно -0.1..0.1)
	 * @param midtones   тонировка полутонов
	 * @param highlights тонировка светлых областей
	 * @param contrast   контраст (-1.0..1.0, 0 — нейтрально)
	 * @param saturation насыщенность (-1.0..1.0, 0 — нейтрально)
	 * @param exposure   экспозиция в стопах (-2.0..2.0, 0 — нейтрально)
	 */
	@SuppressWarnings("deprecation")
	public void apply(
		Vector3f shadows,
		Vector3f midtones,
		Vector3f highlights,
		float contrast,
		float saturation,
		float exposure
	) {
		if (paramsChanged(shadows, midtones, highlights, contrast, saturation, exposure)) {
			rebuild(shadows, midtones, highlights, contrast, saturation, exposure);
		}

		if (processor == null) {
			return;
		}

		processor.render(mc.getFramebuffer(), ObjectAllocator.TRIVIAL);
	}

	public void close() {
		if (processor != null) {
			processor.close();
			processor = null;
		}

		if (projectionMatrix != null) {
			projectionMatrix.close();
			projectionMatrix = null;
		}
	}

	private boolean paramsChanged(
		Vector3f shadows,
		Vector3f midtones,
		Vector3f highlights,
		float contrast,
		float saturation,
		float exposure
	) {
		return cachedContrast != contrast
			|| cachedSaturation != saturation
			|| cachedExposure != exposure
			|| !shadows.equals(cachedShadows)
			|| !midtones.equals(cachedMidtones)
			|| !highlights.equals(cachedHighlights);
	}

	private void rebuild(
		Vector3f shadows,
		Vector3f midtones,
		Vector3f highlights,
		float contrast,
		float saturation,
		float exposure
	) {
		close();

		try {
			projectionMatrix = new ProjectionMatrix2("duollectis_color_grade", 0.1f, 1000.0f, false);

			List<PostEffectPipeline.Input> inputs = List.of(
				new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false)
			);

			Map<String, List<UniformValue>> uniforms = Map.of(
				"ColorGradeConfig", List.of(
					new UniformValue.Vec3fValue(shadows),
					new UniformValue.Vec3fValue(midtones),
					new UniformValue.Vec3fValue(highlights),
					new UniformValue.FloatValue(contrast),
					new UniformValue.FloatValue(saturation),
					new UniformValue.FloatValue(exposure)
				)
			);

			PostEffectPipeline.Pass pass = new PostEffectPipeline.Pass(
				VERTEX_SHADER, SHADER_COLOR_GRADE, inputs, TARGET_MAIN, uniforms
			);

			processor = PostEffectProcessor.parseEffect(
				new PostEffectPipeline(Map.of(), List.of(pass)),
				mc.getTextureManager(),
				EXTERNAL_TARGETS,
				Identifier.of("duollectis", "color_grade"),
				projectionMatrix
			);

			cachedShadows = new Vector3f(shadows);
			cachedMidtones = new Vector3f(midtones);
			cachedHighlights = new Vector3f(highlights);
			cachedContrast = contrast;
			cachedSaturation = saturation;
			cachedExposure = exposure;
		} catch (ShaderLoader.LoadException exception) {
			log.error("Не удалось создать процессор цветокоррекции", exception);
			close();
		}
	}

}
