package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Иммутабельный слой схемы — все не-воздушные блоки на одной мировой X-координате.
 * Блоки разбиты на колонки по (X, Z). Внутри каждой колонки список отсортирован
 * снизу вверх по Y: индекс 0 — нижний блок, последний — верхний.
 *
 * @param id      порядковый номер слоя (0 = самый западный)
 * @param columns колонки блоков; каждая колонка — список блоков по Y снизу вверх
 */
record SchematicLayer(int id, List<List<BlockEntry>> columns) {

	SchematicLayer(int id, List<List<BlockEntry>> columns) {
		this.id = id;
		this.columns = columns.stream()
				.map(List::copyOf)
				.toList();
	}

	/** Возвращает колонку блоков по индексу. Индекс 0 — нижний блок, последний — верхний. */
	List<BlockEntry> column(int index) {
		return columns.get(index);
	}

	int columnCount() {
		return columns.size();
	}

	boolean isEmpty() {
		return columns.isEmpty();
	}

	/**
	 * Возвращает максимальную мировую Y-координату среди всех блоков слоя.
	 * Используется для вычисления крейсерской высоты полёта над схемой.
	 */
	int maxY() {
		return columns.stream()
			.flatMap(List::stream)
			.mapToInt(entry -> entry.pos().getY())
			.max()
			.orElse(0);
	}


	/**
	 * Подсчитывает блоки слоя, которые ещё не поставлены в мире.
	 * Блок считается уже поставленным, если {@code world.getBlockState(pos)} совпадает с целевым состоянием.
	 *
	 * @param world текущий клиентский мир для проверки реального состояния блоков
	 * @return карта «состояние блока → количество ещё не поставленных»
	 */
	Map<BlockState, Integer> countBlocks(ClientWorld world) {
		Map<BlockState, Integer> counts = new HashMap<>();

		for (List<BlockEntry> column : columns) {
			for (BlockEntry entry : column) {
				if (world.getBlockState(entry.pos()).isAir()) {
					counts.merge(entry.state(), 1, Integer::sum);
				}
			}
		}

		return counts;
	}

	record BlockEntry(BlockPos pos, BlockState state) {}
}
