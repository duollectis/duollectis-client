package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.fsm.StateMachine;
import org.duollectis.client.api.modules.Controller;

/**
 * Контроллер полёта к целевой позиции по двум режимам.
 * <p>
 * Режим {@link #start} — первый перелёт от контейнеров:
 * <ol>
 *   <li>{@link Stage#ASCEND} — подъём по Y до крейсерской высоты {@code cruiseY}.</li>
 *   <li>{@link Stage#FLY_XZ} — горизонтальный перелёт к XZ цели.</li>
 *   <li>{@link Stage#DESCEND} — спуск по Y до {@code target.Y + hoverHeight}.</li>
 * </ol>
 * Режим {@link #startStaircase} — переход между колонками во время стройки.
 * Если следующий блок выше текущей позиции игрока: сначала подъём по Y, затем XZ.
 * Если следующий блок ниже или на том же уровне: сначала XZ, затем спуск по Y.
 * Диагональное движение запрещено — оси всегда обрабатываются строго последовательно.
 * <ol>
 *   <li>{@link Stage#ASCEND_STAIRCASE} — подъём по Y (только если цель выше).</li>
 *   <li>{@link Stage#FLY_XZ_STAIRCASE} — горизонтальный перелёт к XZ цели.</li>
 *   <li>{@link Stage#ADJUST_Y} — спуск по Y до {@code target.Y + hoverHeight} (только если цель ниже).</li>
 * </ol>
 */
class FlyController extends Controller<MapArtBuilder> {

	private static final double ARRIVAL_THRESHOLD = 0.1;

	private final StateMachine<Stage> fsm = StateMachine
			.start(Stage.DONE)
			.on(Stage.ASCEND, this::tickAscend)
			.on(Stage.FLY_XZ, this::tickFlyXZ)
			.on(Stage.DESCEND, this::tickDescend)
			.on(Stage.ASCEND_STAIRCASE, this::tickAscendStaircase)
			.on(Stage.FLY_XZ_STAIRCASE, this::tickFlyXZStaircase)
			.on(Stage.ADJUST_Y, this::tickAdjustY)
			.terminal(Stage.DONE)
			.build();

	private BlockPos target;
	private double cruiseY;
	private double hoverY;

	@Override
	protected void onDeactivate() {
		reset();
	}

	boolean isDone() {
		return fsm.is(Stage.DONE);
	}

	/**
	 * Запускает полёт первого типа: подъём до потолка схемы → XZ → спуск до hoverHeight.
	 *
	 * @param target  позиция целевого блока
	 * @param cruiseY крейсерская высота (потолок схемы + buildAboveHeight)
	 */
	void start(BlockPos target, double cruiseY) {
		debugInfo("ПОЛЁТ (потолок) к " + target + ", cruiseY=" + cruiseY);
		this.target = target;
		this.cruiseY = cruiseY;
		this.hoverY = target.getY() + module.buildAboveHeight.get();
		fsm.restart(Stage.ASCEND);
	}

	/**
	 * Запускает полёт лесенкой между колонками без диагонального движения.
	 * Если цель выше текущей позиции — сначала подъём по Y, затем XZ.
	 * Если цель ниже или на том же уровне — сначала XZ, затем спуск по Y.
	 *
	 * @param target позиция следующего целевого блока
	 */
	void startStaircase(BlockPos target) {
		debugInfo("ПОЛЁТ (лесенка) к " + target);
		this.target = target;
		this.hoverY = target.getY() + module.buildAboveHeight.get();

		boolean targetIsHigher = hoverY > mc.player.getY() + ARRIVAL_THRESHOLD;
		fsm.restart(targetIsHigher ? Stage.ASCEND_STAIRCASE : Stage.FLY_XZ_STAIRCASE);
	}

	/**
	 * Запускает умный перелёт между зонами схемы с минимально необходимым подъёмом.
	 * Если {@code clearanceY} выше текущей позиции игрока — сначала подъём до {@code clearanceY},
	 * затем XZ, затем спуск до hoverHeight цели.
	 * Если игрок уже выше {@code clearanceY} — сразу XZ, затем спуск.
	 *
	 * @param target     позиция следующего целевого блока
	 * @param clearanceY минимальная высота для безопасного перелёта над блоками на пути
	 */
	void startZoneHop(BlockPos target, double clearanceY) {
		this.target = target;
		this.hoverY = target.getY() + module.buildAboveHeight.get();
		// Крейсерская высота — максимум из clearanceY и hoverY цели,
		// чтобы не лететь XZ ниже точки назначения и не врезаться в блоки снизу.
		this.cruiseY = Math.max(clearanceY, hoverY);

		debugInfo("ПОЛЁТ (зона) к " + target + ", clearanceY=" + clearanceY + ", cruiseY=" + cruiseY);

		boolean needsAscent = cruiseY > mc.player.getY() + ARRIVAL_THRESHOLD;
		fsm.restart(needsAscent ? Stage.ASCEND : Stage.FLY_XZ);
	}

	void reset() {
		debugInfo("СБРОС ПОЛЁТА");
		target = null;
		fsm.reset();
		module.setVelocity(Vec3d.ZERO);
	}

	void onTick() {
		fsm.tick();
	}

	private void tickAscend() {
		double currentY = mc.player.getY();
		double speed = module.verticalSpeed.get();

		if (currentY >= cruiseY - ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);
			fsm.go(Stage.FLY_XZ);
			return;
		}

		double step = Math.min(speed, cruiseY - currentY);
		module.setVelocity(new Vec3d(0, step, 0));
	}

	private void tickFlyXZ() {
		double currentX = mc.player.getX();
		double currentZ = mc.player.getZ();
		double targetX = target.getX() + 0.5;
		double targetZ = target.getZ() + 0.5;
		double speed = module.horizontalSpeed.get();

		double dx = targetX - currentX;
		double dz = targetZ - currentZ;
		double dist = Math.sqrt(dx * dx + dz * dz);

		if (dist <= ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);
			fsm.go(Stage.DESCEND);
			return;
		}

		double scale = Math.min(speed, dist) / dist;
		module.setVelocity(new Vec3d(dx * scale, 0, dz * scale));
	}

	private void tickDescend() {
		double currentY = mc.player.getY();
		double speed = module.verticalSpeed.get();

		if (currentY <= hoverY + ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);
			fsm.go(Stage.DONE);
			return;
		}

		double step = Math.min(speed, currentY - hoverY);
		module.setVelocity(new Vec3d(0, -step, 0));
	}

	/**
	 * Первый шаг лесенки (цель выше): подъём по Y до hoverHeight следующего блока.
	 * После подъёма переходит к горизонтальному перелёту.
	 */
	private void tickAscendStaircase() {
		double currentY = mc.player.getY();
		double speed = module.verticalSpeed.get();

		if (currentY >= hoverY - ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);
			fsm.go(Stage.FLY_XZ_STAIRCASE);
			return;
		}

		double step = Math.min(speed, hoverY - currentY);
		module.setVelocity(new Vec3d(0, step, 0));
	}

	/**
	 * Горизонтальный перелёт к XZ следующей колонки.
	 * Если цель была выше — это второй шаг (после подъёма), переход в DONE.
	 * Если цель ниже — это первый шаг, после которого следует спуск по Y.
	 */
	private void tickFlyXZStaircase() {
		double currentX = mc.player.getX();
		double currentZ = mc.player.getZ();
		double targetX = target.getX() + 0.5;
		double targetZ = target.getZ() + 0.5;
		double speed = module.horizontalSpeed.get();

		double dx = targetX - currentX;
		double dz = targetZ - currentZ;
		double dist = Math.sqrt(dx * dx + dz * dz);

		if (dist <= ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);

			boolean alreadyAtHeight = Math.abs(mc.player.getY() - hoverY) <= ARRIVAL_THRESHOLD;
			fsm.go(alreadyAtHeight ? Stage.DONE : Stage.ADJUST_Y);
			return;
		}

		double scale = Math.min(speed, dist) / dist;
		module.setVelocity(new Vec3d(dx * scale, 0, dz * scale));
	}

	/**
	 * Второй шаг лесенки: выравнивание по Y до hoverHeight следующего блока.
	 */
	private void tickAdjustY() {
		double currentY = mc.player.getY();
		double speed = module.verticalSpeed.get();

		if (Math.abs(currentY - hoverY) <= ARRIVAL_THRESHOLD) {
			module.setVelocity(Vec3d.ZERO);
			fsm.go(Stage.DONE);
			return;
		}

		double step = Math.min(speed, Math.abs(hoverY - currentY));
		double direction = Math.signum(hoverY - currentY);
		module.setVelocity(new Vec3d(0, direction * step, 0));
	}

	private enum Stage {
		ASCEND,
		FLY_XZ,
		DESCEND,
		ASCEND_STAIRCASE,
		FLY_XZ_STAIRCASE,
		ADJUST_Y,
		DONE
	}
}
