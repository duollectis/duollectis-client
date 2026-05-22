package org.duollectis.client.utils.world;

/**
 * Функциональный интерфейс для обхода блоков в пространстве.
 * Возвращает {@code false}, чтобы прервать итерацию досрочно.
 */
public interface BlockPosConsumer {
	boolean call(int x, int y, int z);
}
