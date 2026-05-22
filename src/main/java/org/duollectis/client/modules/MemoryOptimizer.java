package org.duollectis.client.modules;

import static org.duollectis.client.utils.MC.mc;

import lombok.Getter;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.IntSetting;

@ModuleInfo(
	value = "memory-optimizer",
	category = Category.MISC
)
public class MemoryOptimizer extends Module {

	private static final int TICKS_PER_SECOND = 20;

	@Getter
	private final Setting<Boolean> noWeather = settings.add(BoolSetting
		.create("no-weather")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Boolean> noWeatherParticles = settings.add(BoolSetting
		.create("no-weather-particles")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Boolean> limitParticles = settings.add(BoolSetting
		.create("limit-particles")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Integer> maxParticles = settings.add(IntSetting
		.create("max-particles")
		.defaultValue(64)
		.range(0, 4096)
		.sliderRange(0, 512)
		.done());

	/**
	 * Замораживает тик анимаций спрайт-атласа (вода, лава, огонь, порталы).
	 * Экономит ~50-150 МБ heap-буферов за счёт остановки GPU-апдейтов анимированных текстур.
	 */
	@Getter
	private final Setting<Boolean> freezeAnimations = settings.add(BoolSetting
		.create("freeze-animations")
		.defaultValue(true)
		.done());

	/**
	 * Обнуляет уровень mipmap при загрузке атласа текстур.
	 * Применяется немедленно через перезагрузку ресурсов. Экономия ~100-200 МБ.
	 */
	@Getter
	private final Setting<Boolean> noMipmap = settings.add(BoolSetting
		.create("no-mipmap")
		.defaultValue(false)
		.done());

	@Getter
	private final Setting<Boolean> limitSounds = settings.add(BoolSetting
		.create("limit-sounds")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Integer> maxSounds = settings.add(IntSetting
		.create("max-sounds")
		.defaultValue(8)
		.range(1, 255)
		.sliderRange(1, 64)
		.done());

	/**
	 * Ограничивает очередь задач перестройки чанков.
	 * Каждая задача держит ~2-4 МБ нативной памяти — при большой очереди это сотни МБ.
	 */
	@Getter
	private final Setting<Boolean> limitChunkQueue = settings.add(BoolSetting
		.create("limit-chunk-queue")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Integer> maxChunkQueue = settings.add(IntSetting
		.create("max-chunk-queue")
		.defaultValue(4)
		.range(1, 64)
		.sliderRange(1, 32)
		.done());

	/**
	 * Принудительно ограничивает дистанцию рендера чанков.
	 * Самый мощный рычаг: каждый чанк = ~1-2 МБ буферов. Экономия ~200-400 МБ.
	 * При активации немедленно вызывает перепланировку чанков через scheduleTerrainUpdate.
	 */
	@Getter
	private final Setting<Boolean> limitRenderDistance = settings.add(BoolSetting
		.create("limit-render-distance")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Integer> maxRenderDistance = settings.add(IntSetting
		.create("max-render-distance")
		.defaultValue(2)
		.range(2, 32)
		.sliderRange(2, 16)
		.done());

	@Getter
	private final Setting<Boolean> autoPurge = settings.add(BoolSetting
		.create("auto-purge")
		.defaultValue(true)
		.done());

	@Getter
	private final Setting<Integer> purgeIntervalSeconds = settings.add(IntSetting
		.create("purge-interval")
		.defaultValue(15)
		.range(5, 300)
		.sliderRange(5, 120)
		.done());

	private int tickCounter;

	@Override
	protected void onActivate() {
		tickCounter = 0;
		applyRenderDistanceLimit();
		applyMipmapSetting();
		purgeHeavyResources();
	}

	@Override
	protected void onDeactivate() {
		if (mc.worldRenderer != null) {
			mc.worldRenderer.scheduleTerrainUpdate();
		}
	}

	/**
	 * Принудительно выгружает все тяжёлые ресурсы из памяти:
	 * активные частицы, буферы звуков, кэш текстур карт и запускает GC.
	 */
	public void purgeHeavyResources() {
		if (mc.particleManager != null) {
			mc.particleManager.clearParticles();
		}

		mc.getSoundManager().stopAll();
		mc.getMapTextureManager().clear();

		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		runtime.gc();
	}

	/**
	 * Немедленно применяет ограничение дистанции рендера, перепланируя все чанки.
	 * Без этого вызова уже загруженные чанки остаются в памяти до следующего тика.
	 */
	private void applyRenderDistanceLimit() {
		if (mc.worldRenderer == null || !limitRenderDistance.get()) {
			return;
		}

		mc.worldRenderer.scheduleTerrainUpdate();
	}

	/**
	 * Применяет настройку mipmap через полную перезагрузку ресурсов.
	 * Вызывается только при включении модуля, чтобы не перезагружать ресурсы лишний раз.
	 */
	private void applyMipmapSetting() {
		if (!noMipmap.get()) {
			return;
		}

		mc.reloadResources();
	}

	@Subscribe
	private void onTick(TickPreEvent event) {
		if (!autoPurge.get()) {
			return;
		}

		int intervalTicks = purgeIntervalSeconds.get() * TICKS_PER_SECOND;
		tickCounter++;

		if (tickCounter < intervalTicks) {
			return;
		}

		tickCounter = 0;
		purgeHeavyResources();
	}

}
