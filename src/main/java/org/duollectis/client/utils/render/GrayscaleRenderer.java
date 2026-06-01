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
 * Применяет эффект обесцвечивания к главному framebuffer.
 * При {@code strength = 1.0} — полный grayscale, при {@code 0.0} — оригинал.
 */
@Slf4j
public final class GrayscaleRenderer {

	private static final Identifier VERTEX_SHADER = Identifier.ofVanilla("core/screenquad");
	private static final Identifier SHADER_GRAYSCALE = Identifier.of("duollectis", "post/grayscale");
	private static final Identifier TARGET_MAIN = Identifier.ofVanilla("main");
	private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(TARGET_MAIN);

	private PostEffectProcessor processor;
	private ProjectionMatrix2 projectionMatrix;
	private float cachedStrength = Float.NaN;

	/**
	 * Применяет обесцвечивание к главному framebuffer.
	 *
	 * @param strength сила эффекта (0.0 — нет эффекта, 1.0 — полный grayscale)
	 */
	@SuppressWarnings("deprecation")
	public void apply(float strength) {
		if (cachedStrength != strength) {
			rebuild(strength);
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

	private void rebuild(float strength) {
		close();

		try {
			projectionMatrix = new ProjectionMatrix2("duollectis_grayscale", 0.1f, 1000.0f, false);

			List<PostEffectPipeline.Input> inputs = List.of(
				new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false)
			);

			Map<String, List<UniformValue>> uniforms = Map.of(
				"GrayscaleConfig", List.of(
					new UniformValue.FloatValue(strength)
				)
			);

			PostEffectPipeline.Pass pass = new PostEffectPipeline.Pass(
				VERTEX_SHADER, SHADER_GRAYSCALE, inputs, TARGET_MAIN, uniforms
			);

			processor = PostEffectProcessor.parseEffect(
				new PostEffectPipeline(Map.of(), List.of(pass)),
				mc.getTextureManager(),
				EXTERNAL_TARGETS,
				Identifier.of("duollectis", "grayscale"),
				projectionMatrix
			);

			cachedStrength = strength;
		} catch (ShaderLoader.LoadException exception) {
			log.error("Не удалось создать процессор grayscale", exception);
			close();
		}
	}

}
