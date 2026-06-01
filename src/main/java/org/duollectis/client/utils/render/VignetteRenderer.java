package org.duollectis.client.utils.render;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.gl.PostEffectPipeline;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.UniformValue;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.duollectis.client.utils.MC.mc;

/**
 * Применяет эффект виньетки к главному framebuffer.
 * Затемняет края экрана с настраиваемой силой, мягкостью и округлостью.
 */
@Slf4j
public final class VignetteRenderer {

	private static final Identifier VERTEX_SHADER = Identifier.ofVanilla("core/screenquad");
	private static final Identifier SHADER_VIGNETTE = Identifier.of("duollectis", "post/vignette");
	private static final Identifier TARGET_MAIN = Identifier.ofVanilla("main");
	private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(TARGET_MAIN);

	private PostEffectProcessor processor;
	private ProjectionMatrix2 projectionMatrix;
	private float cachedStrength = Float.NaN;
	private float cachedSoftness = Float.NaN;
	private float cachedRoundness = Float.NaN;

	/**
	 * Применяет виньетку к главному framebuffer.
	 *
	 * @param strength  сила затемнения краёв (0.5 — слабая, 2.0 — сильная)
	 * @param softness  мягкость перехода (0.1 — резкая, 0.8 — плавная)
	 * @param roundness соотношение сторон виньетки (1.0 — круглая, >1 — вытянутая по X)
	 */
	@SuppressWarnings("deprecation")
	public void apply(float strength, float softness, float roundness) {
		if (cachedStrength != strength || cachedSoftness != softness || cachedRoundness != roundness) {
			rebuild(strength, softness, roundness);
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

	private void rebuild(float strength, float softness, float roundness) {
		close();

		try {
			projectionMatrix = new ProjectionMatrix2("duollectis_vignette", 0.1f, 1000.0f, false);

			List<PostEffectPipeline.Input> inputs = List.of(
				new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false)
			);

			Map<String, List<UniformValue>> uniforms = Map.of(
				"VignetteConfig", List.of(
					new UniformValue.FloatValue(strength),
					new UniformValue.FloatValue(softness),
					new UniformValue.FloatValue(roundness)
				)
			);

			PostEffectPipeline.Pass pass = new PostEffectPipeline.Pass(
				VERTEX_SHADER, SHADER_VIGNETTE, inputs, TARGET_MAIN, uniforms
			);

			processor = PostEffectProcessor.parseEffect(
				new PostEffectPipeline(Map.of(), List.of(pass)),
				mc.getTextureManager(),
				EXTERNAL_TARGETS,
				Identifier.of("duollectis", "vignette"),
				projectionMatrix
			);

			cachedStrength = strength;
			cachedSoftness = softness;
			cachedRoundness = roundness;
		} catch (ShaderLoader.LoadException exception) {
			log.error("Не удалось создать процессор виньетки", exception);
			close();
		}
	}

}
