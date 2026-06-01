package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.fsm.StateMachine;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.utils.inventory.InvUtility;
import org.duollectis.client.utils.inventory.ItemStackUtil;
import org.duollectis.client.utils.world.MiscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Контроллер сканирования, обхода и сбора предметов из хранилищ (сундуки, шалкеры) в радиусе вокруг игрока.
 * <p>
 * Жизненный цикл: {@code SCAN} → {@code CHECK} (кэширование содержимого) → {@code DONE}.
 * После заполнения кэша можно запустить режим {@code COLLECT} через {@link #startCollect(Map)},
 * который обходит хранилища из кэша и пакетно забирает предметы без открытия GUI.
 */
public class StoragesController extends Controller<MapArtBuilder> {

	private static final double ARRIVAL_THRESHOLD = 0.3;
	private static final int SHULKER_SLOTS = 27;

	private final ScanController scanController = new ScanController();
	private final CollectController collectController = new CollectController();
	private final DepositController depositController = new DepositController();
	private final FlyController flyController = new FlyController();

	private final StateMachine<Stage> fsm = StateMachine
			.start(Stage.SCAN)
			.on(Stage.SCAN, this::scanStorages)
			.on(Stage.CHECK, this::checkStorages)
			.on(Stage.COLLECT, this::tickCollect)
			.on(Stage.DEPOSIT, this::tickDeposit)
			.terminal(Stage.DONE)
			.build();

	private final List<BlockPos> unchecked = new ArrayList<>();

	final Map<BlockPos, @NotNull List<CachedBlock>> cache = new HashMap<>();

	@Nullable
	private BlockPos checking;
	@Nullable
	private BlockPos flyingTo;

	private Map<Item, Integer> needed;
	private List<BlockPos> collectQueue;
	@Nullable
	private BlockPos collecting;
	private boolean flyingFromSchema;

	private Map<Item, Integer> toKeep;
	private List<BlockPos> depositQueue;
	@Nullable
	private BlockPos depositing;

	@Override
	protected void onInit() {
		addController(scanController);
		addController(collectController);
		addController(depositController);
		addController(flyController);
	}

	@Override
	public void onDeactivate() {
		debugInfo("[Storages] Деактивация, сброс состояния (кэш: " + cache.size() + " записей)");
		reset();

		if (module.clearCacheOnDisable.get()) {
			cache.clear();
		}
	}

	void reset() {
		checking = null;
		flyingTo = null;
		flyController.reset();
		unchecked.clear();
		needed = null;
		collectQueue = null;
		collecting = null;
		collectController.reset();
		toKeep = null;
		depositQueue = null;
		depositing = null;
		depositController.reset();
		fsm.restart(Stage.SCAN);
	}

	/**
	 * Запускает режим сбора предметов из уже закэшированных хранилищ.
	 * Обходит только те хранилища, в кэше которых есть хотя бы один нужный предмет.
	 * Сбор выполняется пакетно без открытия GUI через {@link CollectController}.
	 *
	 * @param itemsNeeded карта «предмет → нужное количество стаков»
	 */
	void startCollect(Map<Item, Integer> itemsNeeded) {
		if (fsm.is(Stage.COLLECT)) {
			return;
		}

		needed = new HashMap<>(itemsNeeded);
		collectQueue = buildCollectQueue();
		collecting = null;
		flyingFromSchema = true;
		fsm.restart(Stage.COLLECT);

		debugInfo("[Storages] Запуск сбора, хранилищ с нужными предметами: " + collectQueue.size());
	}

	boolean tick() {
		fsm.tick();
		return fsm.is(Stage.DONE);
	}

	private void checkStorages() {
		if (scanController.isOpened()) {
			return;
		}

		if (scanController.isDone()) {
			List<CachedBlock> blocks = ItemStackUtil.toBlocks(scanController.getResult());
			debugInfo("[Storages] Результат для " + checking + ": " + blocks.size() + " блоков");
			cache.put(checking, blocks);
			scanController.reset();
			checking = null;
		}

		if (checking == null) {
			unchecked.sort(distanceComparator());
			checking = unchecked.isEmpty() ? null : unchecked.removeFirst();
		}

		if (checking == null) {
			fsm.go(Stage.DONE);
			debugInfo("[Storages] Все хранилища проверены, кэш: " + cache.size() + " записей");
			return;
		}

		if (approachDirect(checking)) {
			debugInfo("[Storages] Прилетели к хранилищу: " + checking);
			scanController.start(checking);
		}
	}

	private void tickCollect() {
		if (collectController.isOpened()) {
			return;
		}

		if (collectController.isDone()) {
			cache.put(collecting, collectController.getRemainingBlocks());
			debugInfo("[Storages] Хранилище " + collecting + " обработано, кэш обновлён");
			collectController.reset();
			collecting = null;
			flyingTo = null;
			pruneCollectedItems();
			pruneCollectQueue();

			if (isAllCollected()) {
				fsm.go(Stage.DONE);
				debugInfo("[Storages] Все нужные предметы собраны, досрочное завершение");
				return;
			}
		}

		if (collecting == null) {
			collecting = nextCollectTarget();
		}

		if (collecting == null) {
			fsm.go(Stage.DONE);
			debugInfo("[Storages] Сбор завершён");
			return;
		}

		boolean arrived = flyingFromSchema
		                  ? approachViaTop(collecting)
		                  : approachViaAscent(collecting);

		if (arrived) {
			flyingFromSchema = false;
			collectController.start(collecting, needed, 1, 5);
		}
	}

	/**
	 * Удаляет из {@code needed} предметы, которых уже достаточно в инвентаре игрока.
	 * Вызывается после каждого обработанного хранилища, чтобы не открывать следующие
	 * контейнеры ради предметов, которые уже собраны.
	 */
	private void pruneCollectedItems() {
		needed.entrySet().removeIf(entry -> InvUtility.countItem(entry.getKey()) >= entry.getValue());
	}

	/**
	 * Извлекает следующую цель сбора из очереди, пропуская хранилища,
	 * в кэше которых нет ни одного предмета из актуального {@code needed}.
	 * Защищает от открытия шалкеров, ставших ненужными после частичного сбора.
	 */
	@Nullable
	private BlockPos nextCollectTarget() {
		while (!collectQueue.isEmpty()) {
			BlockPos candidate = collectQueue.removeFirst();
			List<CachedBlock> cached = cache.get(candidate);

			if (cached == null) {
				continue;
			}

			boolean hasNeeded = cached.stream().anyMatch(block -> needed.containsKey(block.block.asItem()));

			if (hasNeeded) {
				return candidate;
			}

			debugInfo("[Storages] Пропускаем " + candidate + " — нет нужных предметов в кэше");
		}

		return null;
	}

	/**
	 * Удаляет из {@code collectQueue} хранилища, в кэше которых нет ни одного предмета
	 * из актуального {@code needed}. Вызывается после {@link #pruneCollectedItems()},
	 * чтобы не летать к шалкерам с уже ненужными предметами.
	 */
	private void pruneCollectQueue() {
		collectQueue.removeIf(pos -> {
			List<CachedBlock> cached = cache.get(pos);

			if (cached == null) {
				return true;
			}

			return cached.stream().noneMatch(block -> needed.containsKey(block.block.asItem()));
		});
	}

	/**
	 * Прямой подлёт к хранилищу (без обхода через верх) — используется при первичном сканировании.
	 * Горизонтальная и вертикальная составляющие ограничиваются независимо.
	 */
	private boolean approachDirect(BlockPos target) {
		Vec3d approachPoint = new Vec3d(
				target.getX() + 0.5,
				target.getY() + 1 + module.hoverHeight.get(),
				target.getZ() + 0.5
		);

		boolean inInteractRange =
				target.getSquaredDistance(mc.player.getEntityPos()) <= Math.pow(module.interactRadius.get(), 2);

		if (mc.player.getEntityPos().distanceTo(approachPoint) < ARRIVAL_THRESHOLD || inInteractRange) {
			module.setVelocity(Vec3d.ZERO);
			return true;
		}

		module.setVelocity(buildApproachVelocity(
				approachPoint,
				module.horizontalSpeed.get(),
				module.verticalSpeed.get()
		));
		return false;
	}

	/**
	 * Летит к хранилищу через верх схемы (подъём → XZ → спуск) и возвращает {@code true},
	 * когда {@link FlyController} завершил маршрут и игрок находится над целью.
	 * Используется при сборе ресурсов, когда бот летит от схемы к хранилищам.
	 */
	private boolean approachViaTop(BlockPos target) {
		boolean inInteractRange =
				target.getSquaredDistance(mc.player.getEntityPos()) <= Math.pow(module.interactRadius.get(), 2);

		if (inInteractRange) {
			flyController.reset();
			flyingTo = null;
			return true;
		}

		if (flyController.isDone() && !target.equals(flyingTo)) {
			flyingTo = target;
			flyController.start(target, module.getSchematicCruiseY());
		}

		flyController.onTick();

		return flyController.isDone();
	}

	/**
	 * Летит к хранилищу по схеме: подъём до {@code target.Y + hoverHeight + 5} → XZ → спуск.
	 * Используется при переходе между хранилищами во время сбора и сдачи,
	 * чтобы бот не летел диагонально сквозь постройку.
	 */
	private boolean approachViaAscent(BlockPos target) {
		boolean inInteractRange =
				target.getSquaredDistance(mc.player.getEntityPos()) <= Math.pow(module.interactRadius.get(), 2);

		if (inInteractRange) {
			flyController.reset();
			flyingTo = null;
			return true;
		}

		double cruiseY = target.getY() + 1 + module.hoverHeight.get() + 5;

		if (flyController.isDone() && !target.equals(flyingTo)) {
			flyingTo = target;
			flyController.start(target, cruiseY);
		}

		flyController.onTick();

		return flyController.isDone();
	}

	/**
	 * Строит вектор скорости прямого подлёта к цели.
	 * Горизонтальная и вертикальная составляющие ограничиваются независимо,
	 * чтобы игрок не "нырял" по диагонали при большой разнице высот.
	 */
	private static Vec3d buildApproachVelocity(Vec3d target, double maxH, double maxV) {
		Vec3d playerPos = mc.player.getEntityPos();

		double dx = target.x - playerPos.x;
		double dy = target.y - playerPos.y;
		double dz = target.z - playerPos.z;

		double horizontalDist = Math.sqrt(dx * dx + dz * dz);

		double velX = horizontalDist > maxH
		              ? dx / horizontalDist * maxH
		              : dx;

		double velZ = horizontalDist > maxH
		              ? dz / horizontalDist * maxH
		              : dz;

		double velY = Math.abs(dy) > maxV
		              ? Math.signum(dy) * maxV
		              : dy;

		return new Vec3d(velX, velY, velZ);
	}

	private List<BlockPos> buildCollectQueue() {
		List<BlockPos> result = new ArrayList<>();

		for (Map.Entry<BlockPos, List<CachedBlock>> entry : cache.entrySet()) {
			boolean hasNeeded = entry.getValue()
			                         .stream()
			                         .anyMatch(cached -> needed.containsKey(cached.block.asItem()));

			if (hasNeeded) {
				result.add(entry.getKey());
			}
		}

		result.sort(distanceComparator());
		return result;
	}

	private boolean isAllCollected() {
		for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
			if (InvUtility.countItem(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}

		return true;
	}

	void resetCollect() {
		needed = null;
		collectQueue = null;
		collecting = null;
		collectController.reset();
		fsm.restart(Stage.DONE);
	}

	/**
	 * Запускает режим возврата лишних блоков в хранилища.
	 * Обходит только те хранилища из кэша, в которых есть место для сдачи
	 * (т.е. хранилище уже содержало хотя бы один из предметов из {@code toKeep}).
	 *
	 * @param toKeep карта «предмет → сколько единиц оставить в инвентаре»
	 */
	void startDeposit(Map<Item, Integer> toKeep) {
		if (fsm.is(Stage.DEPOSIT)) {
			return;
		}

		this.toKeep = new HashMap<>(toKeep);
		depositQueue = buildDepositQueue(toKeep);
		depositing = null;
		fsm.restart(Stage.DEPOSIT);

		debugInfo("[Storages] Запуск сдачи, хранилищ для возврата: " + depositQueue.size());
	}

	private void tickDeposit() {
		if (depositController.isOpened()) {
			return;
		}

		if (depositController.isDone()) {
			debugInfo("[Storages] Хранилище " + depositing + " обработано (сдача)");
			depositController.reset();
			depositing = null;
			flyingTo = null;
		}

		// Проверяем излишек до выбора следующего шалкера — если уже нечего сдавать, завершаем
		if (hasNoExcessToDeposit()) {
			fsm.go(Stage.DONE);
			debugInfo("[Storages] Нечего сдавать — сдача завершена досрочно");
			return;
		}

		if (depositing == null) {
			depositing = depositQueue.isEmpty() ? null : depositQueue.removeFirst();
		}

		if (depositing == null) {
			fsm.go(Stage.DONE);
			debugInfo("[Storages] Сдача завершена");
			return;
		}

		// approachViaAscent возвращает true только один раз при прилёте —
		// после этого depositController.isOpened() станет true и мы уйдём в return выше.
		// Повторного вызова start() не будет.
		if (approachViaAscent(depositing)) {
			debugInfo("[Storages] Прилетели к хранилищу для сдачи: " + depositing);
			depositController.start(depositing, buildShulkerToKeep(depositing));
		}
	}

	private boolean hasNoExcessToDeposit() {
		for (Map.Entry<Item, Integer> entry : toKeep.entrySet()) {
			int inInventory = countItemInPlayerInventory(entry.getKey());

			if (inInventory > entry.getValue()) {
				return false;
			}
		}

		return true;
	}

	private static int countItemInPlayerInventory(Item item) {
		int count = 0;

		for (int i = 0; i < mc.player.getInventory().size(); i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);

			if (stack.isOf(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	/**
	 * Строит карту «предмет → сколько оставить» для конкретного шалкера:
	 * берёт только те предметы из {@code toKeep}, которые есть в кэше этого шалкера.
	 * Это гарантирует что каждый предмет возвращается именно туда, откуда был взят.
	 */
	private Map<Item, Integer> buildShulkerToKeep(BlockPos pos) {
		List<CachedBlock> contents = cache.getOrDefault(pos, List.of());
		Map<Item, Integer> shulkerToKeep = new HashMap<>();

		for (CachedBlock cached : contents) {
			Item item = cached.block.asItem();
			Integer keepCount = toKeep.get(item);

			if (keepCount != null) {
				shulkerToKeep.put(item, keepCount);
			}
		}

		return shulkerToKeep;
	}

	/**
	 * Строит очередь хранилищ для сдачи.
	 * Включает только те шалкеры, у которых по кэшу есть свободное место
	 * и которые содержат хотя бы один предмет из {@code toKeep}.
	 * Сортирует по расстоянию от игрока.
	 */
	private List<BlockPos> buildDepositQueue(Map<Item, Integer> toKeep) {
		return cache.entrySet()
		            .stream()
		            .filter(entry -> hasFreeSlots(entry.getValue()))
		            .filter(entry -> entry.getValue()
		                                  .stream()
		                                  .anyMatch(cached -> toKeep.containsKey(cached.block.asItem()))
		            )
		            .map(Map.Entry::getKey)
		            .sorted(distanceComparator())
		            .collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Определяет по кэшу, есть ли в шалкере свободные слоты.
	 * Занятых слотов = сумма ceil(count / maxStackSize) для каждого типа блока.
	 */
	private static boolean hasFreeSlots(List<CachedBlock> contents) {
		int usedSlots = 0;

		for (CachedBlock cached : contents) {
			int maxStack = cached.block.asItem().getMaxCount();
			usedSlots += (cached.count + maxStack - 1) / maxStack;
		}

		return usedSlots < SHULKER_SLOTS;
	}

	private Comparator<BlockPos> distanceComparator() {
		Vec3d playerPos = mc.player.getEntityPos();

		return Comparator.comparingDouble(pos -> playerPos.squaredDistanceTo(
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5
		));
	}

	private void scanStorages() {
		MiscUtil.iterateStrict(
				module.scanRadius.get(), (x, y, z) -> {
					BlockPos pos = mc.player.getBlockPos().add(x, y, z);
					BlockState state = mc.world.getBlockState(pos);
					Block block = state.getBlock();

					if (isStorage(block)) {
						unchecked.add(pos);
					}

					return false;
				}
		);

		debugInfo("[Storages] Сканирование завершено, найдено хранилищ: " + unchecked.size());
		fsm.go(Stage.CHECK);
	}

	private static boolean isStorage(Block block) {
		return block instanceof AbstractChestBlock<?>
				|| block instanceof ShulkerBoxBlock;
	}

	private enum Stage {
		SCAN,
		CHECK,
		COLLECT,
		DEPOSIT,
		DONE
	}
}
