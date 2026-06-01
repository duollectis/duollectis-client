package org.duollectis.client.modules;

import lombok.Getter;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.DoubleSetting;

import static org.duollectis.client.utils.MC.player;

/**
 * Модуль свободной камеры (FreeCam).
 * При активации переключает вид на третье лицо (скрывает руки, показывает тело игрока),
 * отвязывает камеру и управляет ею независимо через WASD/пробел/шифт.
 * Ввод игрока блокируется миксином {@code FreeCamPlayerMixin} на уровне {@code KeyboardInput#tick}.
 * Мышь перенаправляется в {@code updateRotation} через {@code FreeCamMouseMixin}.
 * Прицел и взаимодействие с блоками работают от позиции камеры через {@code FreeCamCrosshairMixin}.
 * <p>
 * Хранит {@code prevCameraPos} для плавной интерполяции между тиками в {@code CameraMixin}.
 */
@ModuleInfo(
	value = "freecam",
	category = Category.RENDER
)
public class FreeCam extends Module {

	private final Setting<Double> speed = settings.add(DoubleSetting
		.create("speed")
		.defaultValue(0.5)
		.range(0.05, 10.0)
		.sliderRange(0.05, 5.0)
		.decimalPlaces(2)
		.done()
	);

	private final Setting<Double> sensitivity = settings.add(DoubleSetting
		.create("sensitivity")
		.defaultValue(1.0)
		.range(0.1, 5.0)
		.sliderRange(0.1, 3.0)
		.decimalPlaces(2)
		.done()
	);

	@Getter private Vec3d cameraPos = Vec3d.ZERO;
	@Getter private Vec3d prevCameraPos = Vec3d.ZERO;
	@Getter private float cameraPitch;
	@Getter private float cameraYaw;

	private Perspective previousPerspective;

	@Override
	public void onActivate() {
		if (player() == null) {
			return;
		}

		cameraPos = new Vec3d(
			player().getX(),
			player().getY() + player().getStandingEyeHeight(),
			player().getZ()
		);
		prevCameraPos = cameraPos;
		cameraPitch = player().getPitch();
		cameraYaw = player().getYaw();

		previousPerspective = mc.options.getPerspective();
		mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
	}

	@Override
	public void onDeactivate() {
		if (previousPerspective == null) {
			return;
		}

		mc.options.setPerspective(previousPerspective);
		previousPerspective = null;
	}

	@Subscribe
	private void onTick(TickPreEvent event) {
		if (player() == null || mc.world == null) {
			return;
		}

		prevCameraPos = cameraPos;

		double moveSpeed = speed.get();
		float yawRad = cameraYaw * (float) (Math.PI / 180.0);

		// Горизонтальный вектор вперёд — только по yaw, без учёта pitch.
		// W/S двигают камеру параллельно земле независимо от угла наклона.
		double forwardX = -MathHelper.sin(yawRad);
		double forwardZ = MathHelper.cos(yawRad);

		// Вектор вправо: перпендикуляр к взгляду в горизонтальной плоскости.
		// В MC yaw=0 → юг (Z+), вправо = запад (X-), поэтому знаки инвертированы.
		double rightX = -MathHelper.cos(yawRad);
		double rightZ = -MathHelper.sin(yawRad);

		double dx = 0;
		double dy = 0;
		double dz = 0;

		if (mc.options.forwardKey.isPressed()) {
			dx += forwardX * moveSpeed;
			dz += forwardZ * moveSpeed;
		}

		if (mc.options.backKey.isPressed()) {
			dx -= forwardX * moveSpeed;
			dz -= forwardZ * moveSpeed;
		}

		if (mc.options.rightKey.isPressed()) {
			dx += rightX * moveSpeed;
			dz += rightZ * moveSpeed;
		}

		if (mc.options.leftKey.isPressed()) {
			dx -= rightX * moveSpeed;
			dz -= rightZ * moveSpeed;
		}

		if (mc.options.jumpKey.isPressed()) {
			dy += moveSpeed;
		}

		if (mc.options.sneakKey.isPressed()) {
			dy -= moveSpeed;
		}

		cameraPos = cameraPos.add(dx, dy, dz);
	}

	/**
	 * Обновляет углы камеры при движении мыши с учётом настройки чувствительности.
	 * Вызывается из {@code FreeCamMouseMixin} вместо {@code changeLookDirection}.
	 *
	 * @param deltaPitch изменение угла тангажа в градусах
	 * @param deltaYaw   изменение угла рыскания в градусах
	 */
	public void updateRotation(float deltaPitch, float deltaYaw) {
		float sens = sensitivity.get().floatValue();
		cameraPitch = MathHelper.clamp(cameraPitch + deltaPitch * sens, -90.0f, 90.0f);
		cameraYaw = cameraYaw + deltaYaw * sens;
	}

	/**
	 * Возвращает вектор взгляда камеры FreeCam для использования в raycast.
	 * Вычисляется из текущих {@code cameraYaw} и {@code cameraPitch}.
	 */
	public Vec3d getCameraLookVec() {
		float yawRad = cameraYaw * (float) (Math.PI / 180.0);
		float pitchRad = cameraPitch * (float) (Math.PI / 180.0);

		return new Vec3d(
			-MathHelper.sin(yawRad) * MathHelper.cos(pitchRad),
			-MathHelper.sin(pitchRad),
			MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)
		);
	}

}
