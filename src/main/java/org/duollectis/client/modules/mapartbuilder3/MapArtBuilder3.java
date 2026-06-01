package org.duollectis.client.modules.mapartbuilder3;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.jetbrains.annotations.NotNull;

@ModuleInfo(
		value = "mapart-builder-3"
)
public class MapArtBuilder3 extends Module {

	{
		settings.add(BoolSetting
				.create("debug")
				.defaultValue(false)
				.onChanged(v -> debug = v)
				.done());
	}

	final Setting<Double> interactRadius = settings.add(DoubleSetting
			.create("interact-radius")
			.defaultValue(4)
			.range(1, 10)
			.sliderRange(1, 10)
			.decimalPlaces(1)
			.suffix("б")
			.done());

	final Setting<Double> horizontalSpeed = settings.add(DoubleSetting
			.create("horizontal-speed")
			.defaultValue(0.5)
			.range(0.05, 5)
			.sliderRange(0.05, 5)
			.decimalPlaces(2)
			.suffix("б/т")
			.done());

	final Setting<Double> verticalSpeed = settings.add(DoubleSetting
			.create("vertical-speed")
			.defaultValue(0.3)
			.range(0.05, 3)
			.sliderRange(0.05, 3)
			.decimalPlaces(2)
			.suffix("б/т")
			.done());

	/**
	 * На сколько блоков выше целевого блока лететь перед постановкой.
	 */
	final Setting<Double> buildAboveHeight = settings.add(DoubleSetting
			.create("build-above-height")
			.defaultValue(2)
			.range(1, 8)
			.sliderRange(1, 8)
			.suffix("б")
			.done());

	/**
	 * Задержка между постановками блоков в тиках.
	 * Даёт серверу время обработать предыдущий клик перед следующим.
	 */
	final Setting<Double> placeDelay = settings.add(DoubleSetting
			.create("place-delay")
			.defaultValue(2)
			.range(0, 20)
			.sliderRange(0, 20)
			.decimalPlaces(0)
			.suffix("т")
			.done());

	/**
	 * Таймаут ожидания открытия шалкера в тиках.
	 * Если за это время сервер не прислал пакет открытия — повторяем клик.
	 */
	final Setting<Double> openTimeout = settings.add(DoubleSetting
			.create("open-timeout")
			.defaultValue(20)
			.range(5, 100)
			.sliderRange(5, 100)
			.decimalPlaces(0)
			.suffix("т")
			.done());

	/**
	 * Порог прочности элитры в процентах, при достижении которого
	 * контроллер взлёта автоматически меняет её на свежую из инвентаря.
	 */
	final Setting<Double> elytraSwapThreshold = settings.add(DoubleSetting
			.create("elytra-swap-threshold")
			.defaultValue(20)
			.range(1, 99)
			.sliderRange(1, 99)
			.decimalPlaces(0)
			.suffix("%")
			.done());

	/**
	 * Максимальное количество попыток поставить один блок.
	 * Если блок не появился за это число попыток — позиция пропускается.
	 */
	final Setting<Double> maxPlaceAttempts = settings.add(DoubleSetting
			.create("max-place-attempts")
			.defaultValue(6)
			.range(1, 20)
			.sliderRange(1, 20)
			.decimalPlaces(0)
			.done());

	/**
	 * Желаемый вектор перемещения игрока на элитрах за один игровой тик.
	 * Выставляется контроллерами (например, {@link TakeoffController}) каждый тик.
	 */
	@Getter
	@Setter
	@NotNull
	private Vec3d velocity = Vec3d.ZERO;

	@Override
	protected void onDeactivate() {
		velocity = Vec3d.ZERO;
	}

	@Override
	protected void onInit() {
		addController(new TakeoffController());
		addController(new PrintController());
	}
}
