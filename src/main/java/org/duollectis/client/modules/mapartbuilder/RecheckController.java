package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.duollectis.client.api.fsm.StateMachine;
import org.duollectis.client.api.modules.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;

/**
 * Контроллер проверки реального состояния слоёв схемы в мире.
 * <p>
 * Алгоритм:
 * <ol>
 *   <li>Группирует слои по чанкам (X >> 4) — один перелёт покрывает до 16 слоёв.</li>
 *   <li>Если чанк уже загружен — проверяет его немедленно без перелёта.</li>
 *   <li>Если чанк не загружен — летит к его центру и ждёт загрузки (опрос каждый тик).</li>
 *   <li>Вызывает {@link SchematicLayer#countBlocks} для всех слоёв чанка и сохраняет результат.</li>
 * </ol>
 * После завершения {@link #isDone()} возвращает {@code true}, и вызывающий код
 * может получить реальные незавершённые блоки через {@link #getPending(int)}.
 */
class RecheckController extends Controller<MapArtBuilder> {

	private final FlyController flyController = new FlyController();

	private final StateMachine<Stage> fsm = StateMachine
			.start(Stage.DONE)
			.on(Stage.FLYING, this::tickFlying)
			.on(Stage.WAITING, this::tickWaiting)
			.terminal(Stage.DONE)
			.build();

	/**
	 * Очередь чанков для проверки: каждый элемент — список индексов слоёв в этом чанке.
	 */
	private final List<List<Integer>> chunkQueue = new ArrayList<>();

	/**
	 * Реальные незавершённые блоки по каждому слою: индекс слоя → карта блоков.
	 * Заполняется по мере проверки чанков. Слои с пустой картой — уже построены.
	 */
	private final Map<Integer, Map<BlockState, Integer>> pending = new HashMap<>();

	private int currentChunkIndex = 0;

	/**
	 * Текущий список слоёв схемы — передаётся при старте и используется в tick-методах.
	 * Хранится как поле чтобы не пробрасывать через лямбды в FSM.
	 */
	private List<SchematicLayer> layers;

	@Override
	protected void onInit() {
		addController(flyController);
	}

	@Override
	protected void onDeactivate() {
		reset();
	}

	boolean isDone() {
		return fsm.is(Stage.DONE);
	}

	/**
	 * Возвращает реальные незавершённые блоки для слоя по его индексу.
	 * Пустая карта означает что слой полностью построен.
	 */
	Map<BlockState, Integer> getPending(int layerIndex) {
		return pending.getOrDefault(layerIndex, Map.of());
	}

	/**
	 * Запускает проверку всех слоёв схемы.
	 * Чанки которые уже загружены — проверяются немедленно без перелёта.
	 *
	 * @param layers список всех слоёв схемы
	 */
	void start(List<SchematicLayer> layers) {
		pending.clear();
		chunkQueue.clear();
		currentChunkIndex = 0;
		this.layers = layers;

		buildChunkQueue(layers);

		if (chunkQueue.isEmpty()) {
			fsm.reset();
			debugInfo("[Recheck] Нет слоёв для проверки");
			return;
		}

		debugInfo("[Recheck] Запуск, чанков для проверки: " + chunkQueue.size());
		advanceToNextUnprocessed();
	}

	void tick(List<SchematicLayer> layers) {
		this.layers = layers;
		fsm.tick();
	}

	void reset() {
		pending.clear();
		chunkQueue.clear();
		currentChunkIndex = 0;
		layers = null;
		fsm.reset();
		flyController.reset();
	}

	/**
	 * Группирует индексы слоёв по чанкам (X >> 4).
	 * Порядок чанков — по возрастанию X (слои уже отсортированы по X в SchemController).
	 */
	private void buildChunkQueue(List<SchematicLayer> layers) {
		SequencedSet<Integer> chunkXSet = new LinkedHashSet<>();

		for (SchematicLayer layer : layers) {
			if (layer.columnCount() == 0) {
				continue;
			}

			int worldX = layer.column(0).get(0).pos().getX();
			chunkXSet.add(worldX >> 4);
		}

		for (int chunkX : chunkXSet) {
			List<Integer> layerIndices = new ArrayList<>();

			for (int i = 0; i < layers.size(); i++) {
				SchematicLayer layer = layers.get(i);

				if (layer.columnCount() == 0) {
					continue;
				}

				int worldX = layer.column(0).get(0).pos().getX();

				if ((worldX >> 4) == chunkX) {
					layerIndices.add(i);
				}
			}

			if (!layerIndices.isEmpty()) {
				chunkQueue.add(layerIndices);
			}
		}
	}

	/**
	 * Продвигается вперёд по очереди чанков, пропуская уже загруженные (проверяет их сразу).
	 * Останавливается на первом незагруженном чанке и начинает перелёт к нему,
	 * либо завершает работу если все чанки обработаны.
	 */
	private void advanceToNextUnprocessed() {
		while (currentChunkIndex < chunkQueue.size()) {
			if (isCurrentChunkLoaded()) {
				checkCurrentChunk();
				currentChunkIndex++;
			} else {
				fsm.restart(Stage.FLYING);
				flyToCurrentChunk();
				return;
			}
		}

		finishRecheck();
	}

	private void tickFlying() {
		if (flyController.isDone()) {
			fsm.go(Stage.WAITING);
			return;
		}

		flyController.onTick();
	}

	/**
	 * Ждёт загрузки чанка — опрашивает каждый тик.
	 * Как только чанк загружен — проверяет и переходит к следующему.
	 * Это быстрее фиксированного таймера: на хорошем соединении чанк грузится за 1-2 тика.
	 */
	private void tickWaiting() {
		if (!isCurrentChunkLoaded()) {
			return;
		}

		checkCurrentChunk();
		currentChunkIndex++;
		advanceToNextUnprocessed();
	}

	/**
	 * Проверяет все слои текущего чанка и записывает результаты в {@link #pending}.
	 * Вызывается только когда чанки гарантированно загружены.
	 */
	private void checkCurrentChunk() {
		List<Integer> indices = chunkQueue.get(currentChunkIndex);

		for (int layerIndex : indices) {
			SchematicLayer layer = layers.get(layerIndex);
			Map<BlockState, Integer> blocks = layer.countBlocks(mc.world);
			pending.put(layerIndex, blocks);

			debugInfo("[Recheck] Слой " + layerIndex
					+ " незавершённых блоков типов=" + blocks.size());
		}
	}

	/**
	 * Проверяет загружен ли чанк текущего элемента очереди.
	 * Достаточно проверить один слой из группы — все они в одном чанке по X.
	 */
	private boolean isCurrentChunkLoaded() {
		List<Integer> indices = chunkQueue.get(currentChunkIndex);
		SchematicLayer layer = layers.get(indices.get(0));
		BlockPos pos = layer.column(0).get(0).pos();
		return mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
	}

	/**
	 * Летит к центру текущего чанка по X на крейсерской высоте схемы.
	 * Целевая позиция — центр чанка по X, первый слой чанка по Z, крейсерская высота по Y.
	 */
	private void flyToCurrentChunk() {
		List<Integer> indices = chunkQueue.get(currentChunkIndex);
		SchematicLayer firstLayer = layers.get(indices.get(0));

		int worldX = firstLayer.column(0).get(0).pos().getX();
		int chunkX = worldX >> 4;
		int centerX = (chunkX << 4) + 8;

		int worldZ = firstLayer.column(0).get(0).pos().getZ();
		double cruiseY = module.getSchematicCruiseY();

		BlockPos target = new BlockPos(centerX, (int) cruiseY, worldZ);
		flyController.start(target, cruiseY);
	}

	private void finishRecheck() {
		fsm.reset();
		flyController.reset();
		debugInfo("[Recheck] Все чанки проверены, слоёв с незавершёнными блоками: "
				+ pending.values().stream().filter(m -> !m.isEmpty()).count());
	}

	private enum Stage {
		FLYING,
		WAITING,
		DONE
	}
}
