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
import java.util.Optional;
import java.util.Set;

import static org.duollectis.client.utils.MC.mc;

/**
 * Применяет эффект хроматической аберрации к главному framebuffer.
 * Смещает R и B каналы от центра экрана, создавая эффект призматического расщепления.
 * Пересоздаёт {@link PostEffectProcessor} только при изменении параметра {@code strength}.
 */
@Slf4j
public final class ChromaticAberrationRenderer {

	private static final Identifier VERTEX_SHADER = Identifier.ofVanilla("core/screenquad");
	private static final Identifier SHADER_CHROMATIC = Identifier.of("duollectis", "post/chromatic_aberration");

	private static final Identifier TARGET_MAIN = Identifier.ofVanilla("main");

	private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(TARGET_MAIN);

	private PostEffectProcessor processor;
	private ProjectionMatrix2 projectionMatrix;
	private float cachedStrength = Float.NaN;

	/**
	 * Применяет хроматическую аберрацию к главному framebuffer.
	 * Пересоздаёт процессор только при изменении силы эффекта.
	 *
	 * @param strength сила расщепления каналов (0.0 — нет эффекта, 5.0 — максимум)
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
			projectionMatrix = new ProjectionMatrix2("duollectis_chromatic", 0.1f, 1000.0f, false);

			List<PostEffectPipeline.Input> inputs = List.of(
				new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false)
			);

			Map<String, List<UniformValue>> uniforms = Map.of(
				"ChromaticConfig", List.of(
					new UniformValue.FloatValue(strength)
				)
			);

			PostEffectPipeline.Pass pass = new PostEffectPipeline.Pass(
				VERTEX_SHADER,
				SHADER_CHROMATIC,
				inputs,
				TARGET_MAIN,
				uniforms
			);

			PostEffectPipeline pipeline = new PostEffectPipeline(Map.of(), List.of(pass));

			processor = PostEffectProcessor.parseEffect(
				pipeline,
				mc.getTextureManager(),
				EXTERNAL_TARGETS,
				Identifier.of("duollectis", "chromatic_aberration"),
				projectionMatrix
			);

			cachedStrength = strength;
		} catch (ShaderLoader.LoadException exception) {
			log.error("Не удалось создать процессор хроматической аберрации", exception);
			close();
		}
	}

}
