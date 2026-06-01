package org.duollectis.client.modules;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.EnumSetting;

import static org.duollectis.client.utils.MC.player;

/**
 * Модуль управления полётом на элитре.
 * <p>
 * В режиме {@link FlightMode#VANILLA} усиливает стандартную физику элитры:
 * при нажатии пробела добавляет импульс вперёд по вектору взгляда, имитируя
 * запуск фейерверка, но без урона и расхода предметов.
 * <p>
 * В режиме {@link FlightMode#CREATIVE} полностью заменяет физику элитры
 * на прямое управление скоростью (WASD + пробел/шифт), аналогично творческому
 * полёту. Флаг {@code isGliding} остаётся {@code true} — сервер видит элитру.
 * Подмена физики выполняется в {@code ElytraFlyMixin}.
 */
@ModuleInfo(
	value = "elytra-fly",
	category = Category.MOVEMENT
)
public class ElytraFly extends Module {

	private final Setting<FlightMode> mode = settings.add(EnumSetting
		.<FlightMode>create("mode")
		.defaultValue(FlightMode.CREATIVE)
		.done()
	);

	private final Setting<Double> speed = settings.add(DoubleSetting
		.create("speed")
		.defaultValue(1.0)
		.range(0.1, 10.0)
		.sliderRange(0.1, 5.0)
		.decimalPlaces(2)
		.done()
	);

	private final Setting<Double> vanillaBoost = settings.add(DoubleSetting
		.create("vanilla-boost")
		.defaultValue(1.5)
		.range(0.1, 5.0)
		.sliderRange(0.1, 3.0)
		.decimalPlaces(2)
		.visible(() -> mode.get() == FlightMode.VANILLA)
		.done()
	);

	private final Setting<Boolean> autoActivate = settings.add(BoolSetting
		.create("auto-activate")
		.defaultValue(true)
		.done()
	);

	@Subscribe
	private void onTick(TickPreEvent event) {
		if (player() == null || mc.world == null) {
			return;
		}

		if (autoActivate.get() && !player().isOnGround() && !player().isGliding()) {
			player().setFlag(7, true);
		}

		if (!player().isGliding()) {
			return;
		}

		if (mode.get() == FlightMode.CREATIVE) {
			applyCreativeFlight();
		}
	}

	/**
	 * Применяет творческий режим полёта на элитре: скорость задаётся напрямую
	 * через {@code setVelocity}, игнорируя аэродинамику элитры.
	 * Вектор движения строится из WASD-ввода в мировых координатах.
	 */
	private void applyCreativeFlight() {
		double flySpeed = speed.get();
		float yawRad = player().getYaw() * (float) (Math.PI / 180.0);

		double forwardX = -MathHelper.sin(yawRad);
		double forwardZ = MathHelper.cos(yawRad);

		double rightX = MathHelper.cos(yawRad);
		double rightZ = MathHelper.sin(yawRad);

		double dx = 0;
		double dy = 0;
		double dz = 0;

		if (mc.options.forwardKey.isPressed()) {
			dx += forwardX * flySpeed;
			dz += forwardZ * flySpeed;
		}

		if (mc.options.backKey.isPressed()) {
			dx -= forwardX * flySpeed;
			dz -= forwardZ * flySpeed;
		}

		if (mc.options.rightKey.isPressed()) {
			dx -= rightX * flySpeed;
			dz -= rightZ * flySpeed;
		}

		if (mc.options.leftKey.isPressed()) {
			dx += rightX * flySpeed;
			dz += rightZ * flySpeed;
		}

		if (mc.options.jumpKey.isPressed()) {
			dy += flySpeed;
		}

		if (mc.options.sneakKey.isPressed()) {
			dy -= flySpeed;
		}

		player().setVelocity(new Vec3d(dx, dy, dz));
	}

	/** Возвращает текущий режим полёта. */
	public FlightMode getMode() {
		return mode.get();
	}

	/** Возвращает множитель буста для режима VANILLA. */
	public double getVanillaBoost() {
		return vanillaBoost.get();
	}

	public enum FlightMode {
		VANILLA,
		CREATIVE
	}

}
