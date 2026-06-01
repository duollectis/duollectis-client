package org.duollectis.client.utils.render;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.gl.PostEffectPipeline;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.UniformValue;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.duollectis.client.utils.MC.mc;

/**
 * Применяет bloom post-effect к главному framebuffer.
 * Создаёт {@link PostEffectProcessor} программно, минуя кеш {@link net.minecraft.client.gl.ShaderLoader},
 * что позволяет динамически менять параметры (threshold, intensity, radius) без перезагрузки ресурсов.
 */
@Slf4j
public final class BloomRenderer {

	private static final Identifier VERTEX_SHADER = Identifier.ofVanilla("core/screenquad");
	private static final Identifier SHADER_EXTRACT = Identifier.of("duollectis", "post/bloom_extract");
	private static final Identifier SHADER_BLUR = Identifier.of("duollectis", "post/bloom_blur");
	private static final Identifier SHADER_BLEND = Identifier.of("duollectis", "post/bloom_blend");

	private static final Identifier TARGET_MAIN = Identifier.ofVanilla("main");
	private static final Identifier TARGET_BRIGHT = Identifier.of("duollectis", "bloom_bright");
	private static final Identifier TARGET_SWAP = Identifier.of("duollectis", "bloom_swap");

	private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(TARGET_MAIN);

	private PostEffectProcessor processor;
	private ProjectionMatrix2 projectionMatrix;
	private float cachedThreshold = Float.NaN;
	private float cachedIntensity = Float.NaN;
	private float cachedRadius = Float.NaN;

	/**
	 * Применяет bloom к главному framebuffer с заданными параметрами.
	 * Пересоздаёт {@link PostEffectProcessor} только при изменении параметров.
	 *
	 * @param threshold  порог яркости для извлечения bloom (0.0–1.0)
	 * @param intensity  интенсивность bloom-свечения
	 * @param radius     радиус размытия в пикселях
	 */
	@SuppressWarnings("deprecation")
	public void apply(float threshold, float intensity, float radius) {
		if (paramsChanged(threshold, intensity, radius)) {
			rebuild(threshold, intensity, radius);
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

	private boolean paramsChanged(float threshold, float intensity, float radius) {
		return cachedThreshold != threshold
			|| cachedIntensity != intensity
			|| cachedRadius != radius;
	}

	private void rebuild(float threshold, float intensity, float radius) {
		close();

		try {
			projectionMatrix = new ProjectionMatrix2("duollectis_bloom", 0.1f, 1000.0f, false);
			PostEffectPipeline pipeline = buildPipeline(threshold, intensity, radius);
			processor = PostEffectProcessor.parseEffect(
				pipeline,
				mc.getTextureManager(),
				EXTERNAL_TARGETS,
				Identifier.of("duollectis", "bloom"),
				projectionMatrix
			);

			cachedThreshold = threshold;
			cachedIntensity = intensity;
			cachedRadius = radius;
		} catch (ShaderLoader.LoadException exception) {
			log.error("Не удалось создать bloom процессор", exception);
			close();
		}
	}

	private static PostEffectPipeline buildPipeline(float threshold, float intensity, float radius) {
		Map<Identifier, PostEffectPipeline.Targets> targets = Map.of(
			TARGET_BRIGHT, new PostEffectPipeline.Targets(Optional.empty(), Optional.empty(), false, 0),
			TARGET_SWAP, new PostEffectPipeline.Targets(Optional.empty(), Optional.empty(), false, 0)
		);

		List<PostEffectPipeline.Pass> passes = List.of(
			buildExtractPass(threshold, intensity),
			buildBlurPass(TARGET_BRIGHT, TARGET_SWAP, 1.0f, 0.0f, radius),
			buildBlurPass(TARGET_SWAP, TARGET_BRIGHT, 0.0f, 1.0f, radius),
			buildBlendPass()
		);

		return new PostEffectPipeline(targets, passes);
	}

	private static PostEffectPipeline.Pass buildExtractPass(float threshold, float intensity) {
		List<PostEffectPipeline.Input> inputs = List.of(
			new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false)
		);

		Map<String, List<UniformValue>> uniforms = Map.of(
			"BloomExtractConfig", List.of(
				new UniformValue.FloatValue(threshold),
				new UniformValue.FloatValue(intensity)
			)
		);

		return new PostEffectPipeline.Pass(VERTEX_SHADER, SHADER_EXTRACT, inputs, TARGET_BRIGHT, uniforms);
	}

	private static PostEffectPipeline.Pass buildBlurPass(
		Identifier input,
		Identifier output,
		float dirX,
		float dirY,
		float radius
	) {
		List<PostEffectPipeline.Input> inputs = List.of(
			new PostEffectPipeline.TargetSampler("In", input, false, true)
		);

		Map<String, List<UniformValue>> uniforms = Map.of(
			"BlurConfig", List.of(
				new UniformValue.Vec2fValue(new Vector2f(dirX, dirY)),
				new UniformValue.FloatValue(radius)
			)
		);

		return new PostEffectPipeline.Pass(VERTEX_SHADER, SHADER_BLUR, inputs, output, uniforms);
	}

	private static PostEffectPipeline.Pass buildBlendPass() {
		List<PostEffectPipeline.Input> inputs = List.of(
			new PostEffectPipeline.TargetSampler("In", TARGET_MAIN, false, false),
			new PostEffectPipeline.TargetSampler("Bloom", TARGET_BRIGHT, false, true)
		);

		return new PostEffectPipeline.Pass(VERTEX_SHADER, SHADER_BLEND, inputs, TARGET_MAIN, Map.of());
	}

}
