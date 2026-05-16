package org.duollectis.client.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MiscUtil {

	public static boolean iterateStrict(double range, BlockPosConsumer consumer) {
		return iterateStrict(range, false, consumer);
	}

	public static boolean iterateStrict(double range, boolean sortDistance, BlockPosConsumer consumer) {
		return iterateStrict(range, range, sortDistance, consumer);
	}

	public static boolean iterateStrict(double range, double yRange, boolean sortDistance, BlockPosConsumer consumer) {
		double powRange = range * range;
		double powYRange = yRange * yRange;

		int startRange = -MathHelper.floor(range);
		int endRange = MathHelper.ceil(range);

		int startRangeY = -MathHelper.floor(yRange);
		int endRangeY = MathHelper.ceil(yRange);

		List<BlockPos> positions = new ArrayList<>();

		for (int y = startRangeY; y <= endRangeY; y++) {
			int powY = y * y;

			for (int x = startRange; x <= endRange; x++) {
				int powX = x * x;

				for (int z = startRange; z <= endRange; z++) {
					int powZ = z * z;

					if (powX + powZ > powRange || powY > powYRange) {
						continue;
					}

					if (sortDistance) {
						positions.add(new BlockPos(x, y, z));
					}
					else if (consumer.call(x, y, z)) {
						return true;
					}
				}
			}
		}

		if (sortDistance) {
			positions.sort(Comparator.comparing(pos -> pos.getSquaredDistance(BlockPos.ZERO)));

			for (var pos : positions) {
				if (consumer.call(pos.getX(), pos.getY(), pos.getZ())) {
					return true;
				}
			}
		}

		return false;
	}


	private static boolean iterateInRange(int y, int range, List<BlockPos> ignorePositions, BlockPosConsumer callback) {
		for (int rang = 0; rang <= range; rang++) {
			if (rang == 0) {
				if (call(ignorePositions, 0, y, 0, callback))
					return true;
			}
			else {
				int x, z, ix, iz;
				ix = iz = 0;
				x = z = -rang;

				while (true) {
					if (ix == 0 && iz == 0)
						ix = 1;

					if (x >= rang && ix == 1) {
						ix = 0;
						iz = 1;
					}

					if (z >= rang && iz == 1) {
						ix = -1;
						iz = 0;
					}

					if (x <= -rang && ix == -1) {
						ix = 0;
						iz = -1;
					}

					if (z <= -rang && iz == -1)
						break;

					if (call(ignorePositions, x, y, z, callback))
						return true;

					x += ix;
					z += iz;
				}
			}
		}

		return false;
	}

	private static boolean call(List<BlockPos> ignorePositions, int x, int y, int z, BlockPosConsumer callback) {
		if (ignorePositions == null || !ignorePositions.contains(new BlockPos(x, y, z)))
			return callback.call(x, y, z);

		return false;
	}


}
