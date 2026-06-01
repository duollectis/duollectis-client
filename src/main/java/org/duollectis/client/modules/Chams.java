package org.duollectis.client.modules;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.settings.EnumSetting;
import org.duollectis.client.utils.color.Color;

import java.util.function.Function;

/**
 * Чамсы — два визуальных эффекта поверх живых существ:
 *
 * <p>1. Шейдер поверх модели (режим выбирается настройкой {@code shader-mode}):
 * FLAT — плоский цвет, RAINBOW — радужный перелив, PULSE — пульсирующий цвет.
 *
 * <p>2. Ванильный 2D-силуэт через {@code outlineColor} в {@code submitModel} —
 * рендерится в {@code entityOutlineFramebuffer} и обрабатывается post-processor'ом
 * {@code entity_outline}, который даёт настоящий плоский силуэт с контуром по краям.
 */
@ModuleInfo(
	value = "chams",
	category = Category.RENDER
)
public class Chams extends Module {

	private static final RenderPipeline FLAT_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(
				RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET,
				RenderPipelines.GLOBALS_SNIPPET
			)
			.withUniform("Lighting", UniformType.UNIFORM_BUFFER)
			.withVertexShader(Identifier.of("duollectis", "core/chams"))
			.withFragmentShader(Identifier.of("duollectis", "core/chams"))
			.withSampler("Sampler0")
			.withSampler("Sampler1")
			.withSampler("Sampler2")
			.withVertexFormat(
				VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS
			)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.withLocation("duollectis/chams_flat")
			.build()
	);

	private static final RenderPipeline RAINBOW_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(
				RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET,
				RenderPipelines.GLOBALS_SNIPPET
			)
			.withUniform("Lighting", UniformType.UNIFORM_BUFFER)
			.withVertexShader(Identifier.of("duollectis", "core/chams_rainbow"))
				.withFragmentShader(Identifier.of("duollectis", "core/chams_rainbow"))
			.withSampler("Sampler0")
			.withSampler("Sampler1")
			.withSampler("Sampler2")
			.withVertexFormat(
				VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS
			)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.withLocation("duollectis/chams_rainbow")
			.build()
	);

	private static final RenderPipeline PULSE_PIPELINE = RenderPipelines.register(
		RenderPipeline
			.builder(
				RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET,
				RenderPipelines.GLOBALS_SNIPPET
			)
			.withUniform("Lighting", UniformType.UNIFORM_BUFFER)
			.withVertexShader(Identifier.of("duollectis", "core/chams_pulse"))
			.withFragmentShader(Identifier.of("duollectis", "core/chams_pulse"))
			.withSampler("Sampler0")
			.withSampler("Sampler1")
			.withSampler("Sampler2")
			.withVertexFormat(
				VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS
			)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.withLocation("duollectis/chams_pulse")
			.build()
	);

	private static final Function<Identifier, RenderLayer> FLAT_LAYER_CACHE = Util.memoize(
		texture -> RenderLayer.of(
			"duollectis_chams_flat",
			RenderSetup.builder(FLAT_PIPELINE)
				.texture("Sampler0", texture)
				.useLightmap()
				.useOverlay()
				.outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				.outputTarget(OutputTarget.MAIN_TARGET)
				.build()
		)
	);

	private static final Function<Identifier, RenderLayer> RAINBOW_LAYER_CACHE = Util.memoize(
		texture -> RenderLayer.of(
			"duollectis_chams_rainbow",
			RenderSetup.builder(RAINBOW_PIPELINE)
				.texture("Sampler0", texture)
				.useLightmap()
				.useOverlay()
				.outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				.outputTarget(OutputTarget.MAIN_TARGET)
				.build()
		)
	);

	private static final Function<Identifier, RenderLayer> PULSE_LAYER_CACHE = Util.memoize(
		texture -> RenderLayer.of(
			"duollectis_chams_pulse",
			RenderSetup.builder(PULSE_PIPELINE)
				.texture("Sampler0", texture)
				.useLightmap()
				.useOverlay()
				.outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				.outputTarget(OutputTarget.MAIN_TARGET)
				.build()
		)
	);

	private static Chams instance;

	private final BoolSetting affectPlayers = settings.add(
		BoolSetting.create("affect-players")
			.defaultValue(true)
			.done()
	);

	private final BoolSetting affectMobs = settings.add(
		BoolSetting.create("affect-mobs")
			.defaultValue(true)
			.done()
	);

	private final EnumSetting<ShaderMode> shaderMode = settings.add(
		EnumSetting.<ShaderMode>create("shader-mode")
			.defaultValue(ShaderMode.FLAT)
			.done()
	);

	private final ColorSetting shaderColor = settings.add(
		ColorSetting.create("shader-color")
			.defaultValue(new Color(255, 255, 255, 80))
			.done()
	);

	private final BoolSetting outlineEnabled = settings.add(
		BoolSetting.create("outline")
			.defaultValue(true)
			.done()
	);

	private final ColorSetting outlineColor = settings.add(
		ColorSetting.create("outline-color")
			.defaultValue(new Color(255, 255, 255, 255))
			.done()
	);

	public Chams() {
		instance = this;
	}

	public static Chams getInstance() {
		return instance;
	}

	public boolean isAffectPlayers() {
		return affectPlayers.get();
	}

	public boolean isAffectMobs() {
		return affectMobs.get();
	}

	public boolean isOutlineEnabled() {
		return outlineEnabled.get();
	}

	public int getOutlineColorPacked() {
		return outlineColor.get().getPacked();
	}

	public int getShaderColorPacked() {
		return shaderColor.get().getPacked();
	}

	public ShaderMode getShaderMode() {
		return shaderMode.get();
	}

	public RenderLayer getShaderLayer(Identifier texture) {
		return switch (shaderMode.get()) {
			case FLAT -> FLAT_LAYER_CACHE.apply(texture);
			case RAINBOW -> RAINBOW_LAYER_CACHE.apply(texture);
			case PULSE -> PULSE_LAYER_CACHE.apply(texture);
			case NONE -> null;
		};
	}

	public enum ShaderMode {
		FLAT,
		RAINBOW,
		PULSE,
		NONE
	}

}
