package org.duollectis.client.utils.rotation;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import static org.duollectis.client.utils.MC.mc;

/** Утилиты для вычисления углов поворота игрока к целевому блоку. */
public class Rotations {

	private static final double BLOCK_CENTER_OFFSET = 0.5;

	/**
	 * Вычисляет yaw (горизонтальный угол) для поворота к центру блока.
	 * Использует {@link MathHelper#wrapDegrees} для минимального угла поворота от текущего yaw.
	 *
	 * @param pos целевой блок
	 * @return целевой yaw в градусах
	 */
	public static double getYaw(BlockPos pos) {
		double player = mc.player.getYaw();
		double dx = pos.getX() + BLOCK_CENTER_OFFSET - mc.player.getX();
		double dz = pos.getZ() + BLOCK_CENTER_OFFSET - mc.player.getZ();
		double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
		return player + MathHelper.wrapDegrees((float) targetYaw - (float) player);
	}

	/**
	 * Вычисляет pitch (вертикальный угол) для прицеливания в центр блока с учётом высоты глаз игрока.
	 *
	 * @param pos целевой блок
	 * @return целевой pitch в градусах
	 */
	public static double getPitch(BlockPos pos) {
		double dx = pos.getX() + BLOCK_CENTER_OFFSET - mc.player.getX();
		double dy = pos.getY() + BLOCK_CENTER_OFFSET
				- (mc.player.getY()
				+ mc.player.getEyeHeight(mc.player.getPose()));
		double dz = pos.getZ() + BLOCK_CENTER_OFFSET - mc.player.getZ();
		double dxz = Math.sqrt(dx * dx + dz * dz);
		double player = mc.player.getPitch();
		double targetPitch = -Math.toDegrees(Math.atan2(dy, dxz));
		return player + MathHelper.wrapDegrees((float) targetPitch - (float) player);
	}
}
