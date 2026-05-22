package org.duollectis.client.utils.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Утилиты для итерации по блокам в сферической области вокруг игрока. */
public final class MiscUtil {

	private MiscUtil() {}

	/**
	 * Итерирует блоки в сфере радиуса {@code range} вокруг игрока без сортировки.
	 *
	 * @param range    радиус в блоках
	 * @param consumer обработчик блока; возврат {@code true} прерывает итерацию
	 * @return {@code true} если итерация была прервана обработчиком
	 */
	public static boolean iterateStrict(double range, BlockPosConsumer consumer) {
		return iterateStrict(range, false, consumer);
	}

	public static boolean iterateStrict(double range, boolean sortDistance, BlockPosConsumer consumer) {
		return iterateStrict(range, range, sortDistance, consumer);
	}

	/**
	 * Итерирует блоки в эллипсоиде с горизонтальным радиусом {@code range} и вертикальным {@code yRange}.
	 * При {@code sortDistance=true} блоки обходятся от ближайшего к дальнему.
	 *
	 * @param range        горизонтальный радиус в блоках
	 * @param yRange       вертикальный радиус в блоках
	 * @param sortDistance сортировать ли блоки по расстоянию перед обходом
	 * @param consumer     обработчик блока; возврат {@code true} прерывает итерацию
	 * @return {@code true} если итерация была прервана обработчиком
	 */
	public static boolean iterateStrict(double range, double yRange, boolean sortDistance, BlockPosConsumer consumer) {
		double powRange = range * range;
		double powYRange = yRange * yRange;

		int startX = -MathHelper.floor(range);
		int endX = MathHelper.ceil(range);
		int startY = -MathHelper.floor(yRange);
		int endY = MathHelper.ceil(yRange);

		List<BlockPos> positions = new ArrayList<>();

		for (int y = startY; y <= endY; y++) {
			int powY = y * y;

			for (int x = startX; x <= endX; x++) {
				int powX = x * x;

				for (int z = startX; z <= endX; z++) {
					int powZ = z * z;

					if (powX + powZ > powRange || powY > powYRange) {
						continue;
					}

					if (sortDistance) {
						positions.add(new BlockPos(x, y, z));
					} else if (consumer.call(x, y, z)) {
						return true;
					}
				}
			}
		}

		if (!sortDistance) {
			return false;
		}

		positions.sort(Comparator.comparing(pos -> pos.getSquaredDistance(BlockPos.ZERO)));

		for (BlockPos pos : positions) {
			if (consumer.call(pos.getX(), pos.getY(), pos.getZ())) {
				return true;
			}
		}

		return false;
	}
}
