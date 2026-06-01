package org.duollectis.client.mixin.modules.mapartenhancer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MapArtEnhancer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.texture.MapTextureManager$MapTexture")
public class MapTextureManagerMixin {

	@Shadow
	private NativeImageBackedTexture texture;

	/**
	 * Срабатывает строго после вызова {@code texture.upload()} внутри {@code updateTexture()},
	 * то есть только когда {@code needsUpdate} был {@code true} и пиксели реально перезаписаны.
	 * При раннем возврате (needsUpdate=false) инжект не срабатывает — накопления эффекта нет.
	 */
	@Inject(
		method = "updateTexture",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/texture/NativeImageBackedTexture;upload()V"
		)
	)
	private void duollectis$applyEnhancements(CallbackInfo ci) {
		MapArtEnhancer module = ModuleManager.getInstance().get(MapArtEnhancer.class);
		if (module == null || !module.isActive()) {
			return;
		}

		double blurValue = module.blur.get();
		int brightnessVal = module.brightness.get();
		int contrastVal = module.contrast.get();
		int saturationVal = module.saturation.get();

		boolean needsBlur = blurValue > 0.0;
		boolean needsPixelWork = brightnessVal != 0 || contrastVal != 0 || saturationVal != 0;

		if (needsBlur || needsPixelWork) {
			NativeImage source = texture.getImage();
			if (source == null) {
				return;
			}

			NativeImage result = needsBlur
				? applyFractionalBlur(source, blurValue)
				: source.applyToCopy(argb -> argb);

			if (needsPixelWork) {
				NativeImage processed = applyColorAdjustments(result, brightnessVal, contrastVal, saturationVal);
				result.close();
				result = processed;
			}

			texture.setImage(result);
		}

		// CLAMP_TO_EDGE предотвращает "оборачивание" текстуры на краях тайлов карты
		GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
		((NativeImageBackedTextureMixin) texture).setSampler(sampler);
	}

	/**
	 * Дробный blur через линейную интерполяцию между двумя целыми радиусами.
	 * При blurValue=0.3: 30% blur(radius=1) + 70% оригинала.
	 * При blurValue=1.5: 50% blur(radius=1) + 50% blur(radius=2).
	 * Это даёт плавную градацию без резких скачков при изменении слайдера.
	 */
	@Unique
	private static NativeImage applyFractionalBlur(NativeImage source, double blurValue) {
		int radiusLow = (int) Math.floor(blurValue);
		int radiusHigh = (int) Math.ceil(blurValue);
		double t = blurValue - radiusLow;

		if (radiusLow == radiusHigh || t < 0.001) {
			return radiusLow == 0
				? source.applyToCopy(argb -> argb)
				: applyGaussianBlur(source, radiusLow);
		}

		NativeImage blurLow = radiusLow == 0
			? source.applyToCopy(argb -> argb)
			: applyGaussianBlur(source, radiusLow);

		NativeImage blurHigh = applyGaussianBlur(source, radiusHigh);

		int width = source.getWidth();
		int height = source.getHeight();
		NativeImage result = new NativeImage(source.getFormat(), width, height, false);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int lo = blurLow.getColorArgb(x, y);
				int hi = blurHigh.getColorArgb(x, y);

				int a = lerp((lo >> 24) & 0xFF, (hi >> 24) & 0xFF, t);
				int r = lerp((lo >> 16) & 0xFF, (hi >> 16) & 0xFF, t);
				int g = lerp((lo >> 8) & 0xFF, (hi >> 8) & 0xFF, t);
				int b = lerp(lo & 0xFF, hi & 0xFF, t);

				result.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
			}
		}

		blurLow.close();
		blurHigh.close();

		return result;
	}

	/**
	 * Разделяемая свёртка Гаусса (separable convolution): горизонтальный проход + вертикальный.
	 * Сложность O(w·h·r) вместо O(w·h·r²) для полного 2D-ядра.
	 * Ядро вычисляется один раз и нормализуется, чтобы сумма весов = 1.
	 */
	@Unique
	private static NativeImage applyGaussianBlur(NativeImage source, int radius) {
		int width = source.getWidth();
		int height = source.getHeight();
		double[] kernel = buildGaussianKernel(radius);

		int[] pixels = source.copyPixelsArgb();
		int[] horizontal = new int[width * height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double a = 0;
				double r = 0;
				double g = 0;
				double b = 0;

				for (int k = -radius; k <= radius; k++) {
					int sx = Math.max(0, Math.min(width - 1, x + k));
					int argb = pixels[y * width + sx];
					double weight = kernel[k + radius];

					a += ((argb >> 24) & 0xFF) * weight;
					r += ((argb >> 16) & 0xFF) * weight;
					g += ((argb >> 8) & 0xFF) * weight;
					b += (argb & 0xFF) * weight;
				}

				horizontal[y * width + x] = ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
			}
		}

		NativeImage result = new NativeImage(source.getFormat(), width, height, false);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double a = 0;
				double r = 0;
				double g = 0;
				double b = 0;

				for (int k = -radius; k <= radius; k++) {
					int sy = Math.max(0, Math.min(height - 1, y + k));
					int argb = horizontal[sy * width + x];
					double weight = kernel[k + radius];

					a += ((argb >> 24) & 0xFF) * weight;
					r += ((argb >> 16) & 0xFF) * weight;
					g += ((argb >> 8) & 0xFF) * weight;
					b += (argb & 0xFF) * weight;
				}

				result.setColorArgb(x, y, ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b);
			}
		}

		return result;
	}

	/**
	 * Строит нормализованное 1D-ядро Гаусса размером (2·radius+1).
	 * Сигма выбирается как radius/2.0 + 0.5 — стандартное соотношение для визуального blur.
	 */
	@Unique
	private static double[] buildGaussianKernel(int radius) {
		int size = 2 * radius + 1;
		double[] kernel = new double[size];
		double sigma = radius / 2.0 + 0.5;
		double twoSigmaSq = 2.0 * sigma * sigma;
		double sum = 0.0;

		for (int i = 0; i < size; i++) {
			double x = i - radius;
			kernel[i] = Math.exp(-(x * x) / twoSigmaSq);
			sum += kernel[i];
		}

		for (int i = 0; i < size; i++) {
			kernel[i] /= sum;
		}

		return kernel;
	}

	@Unique
	private static NativeImage applyColorAdjustments(NativeImage source, int brightnessVal, int contrastVal, int saturationVal) {
		double contrastFactor = (100.0 + contrastVal) / 100.0;
		int brightnessOffset = brightnessVal * 255 / 100;
		double saturationFactor = (100.0 + saturationVal) / 100.0;

		return source.applyToCopy(argb -> {
			int a = (argb >> 24) & 0xFF;
			int r = (argb >> 16) & 0xFF;
			int g = (argb >> 8) & 0xFF;
			int b = argb & 0xFF;

			if (brightnessVal != 0) {
				r = clamp(r + brightnessOffset);
				g = clamp(g + brightnessOffset);
				b = clamp(b + brightnessOffset);
			}

			if (contrastVal != 0) {
				r = clamp((int) (128 + (r - 128) * contrastFactor));
				g = clamp((int) (128 + (g - 128) * contrastFactor));
				b = clamp((int) (128 + (b - 128) * contrastFactor));
			}

			if (saturationVal != 0) {
				double gray = 0.299 * r + 0.587 * g + 0.114 * b;
				r = clamp((int) (gray + (r - gray) * saturationFactor));
				g = clamp((int) (gray + (g - gray) * saturationFactor));
				b = clamp((int) (gray + (b - gray) * saturationFactor));
			}

			return (a << 24) | (r << 16) | (g << 8) | b;
		});
	}

	@Unique
	private static int lerp(int a, int b, double t) {
		return (int) (a + (b - a) * t);
	}

	@Unique
	private static int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}

}
