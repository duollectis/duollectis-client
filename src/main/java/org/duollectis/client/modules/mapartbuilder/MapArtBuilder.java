package org.duollectis.client.modules.mapartbuilder;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ModuleInfo(
		value = "mapart-builder",
		category = Category.MISC
)
public class MapArtBuilder extends Module {

	{
		settings.add(BoolSetting
				.create("debug")
				.defaultValue(false)
				.onChanged(v -> debug = v)
				.done());
	}

	final Setting<Boolean> clearCacheOnDisable = settings.add(BoolSetting
			.create("clear-cache-on-disable")
			.defaultValue(false)
			.done());

	final Setting<Double> scanRadius = settings.add(DoubleSetting
			.create("scan-radius")
			.defaultValue(32)
			.range(1, 128)
			.sliderRange(1, 128)
			.decimalPlaces(0)
			.suffix("б")
			.done());

	final Setting<Double> interactRadius = settings.add(DoubleSetting
			.create("interact-radius")
			.defaultValue(4)
			.range(1, 10)
			.sliderRange(1, 10)
			.decimalPlaces(1)
			.suffix("б")
			.done());

	final Setting<Double> horizontalSpeed = settings.add(DoubleSetting
			.create("horizontal-speed")
			.defaultValue(0.5)
			.range(0.05, 5)
			.sliderRange(0.05, 5)
			.decimalPlaces(2)
			.suffix("б/т")
			.done());

	final Setting<Double> verticalSpeed = settings.add(DoubleSetting
			.create("vertical-speed")
			.defaultValue(0.3)
			.range(0.05, 3)
			.sliderRange(0.05, 3)
			.decimalPlaces(2)
			.suffix("б/т")
			.done());

	final Setting<Double> hoverHeight = settings.add(DoubleSetting
			.create("hover-height")
			.defaultValue(2)
			.range(1, 10)
			.sliderRange(1, 10)
			.decimalPlaces(1)
			.suffix("б")
			.done());

	/**
	 * На сколько блоков выше целевого блока лететь перед постановкой.
	 */
	final Setting<Double> buildAboveHeight = settings.add(DoubleSetting
			.create("build-above-height")
			.defaultValue(2)
			.range(1, 8)
			.sliderRange(1, 8)
			.suffix("б")
			.done());

	/**
	 * Горизонтальный радиус зоны постройки (XZ) вокруг текущей позиции бота.
	 * Бот ставит все блоки в этом радиусе, затем летит к следующей незавершённой зоне.
	 */
	final Setting<Double> horizontalRadius = settings.add(DoubleSetting
			.create("horizontal-radius")
			.defaultValue(4)
			.range(1, 16)
			.sliderRange(1, 16)
			.decimalPlaces(1)
			.suffix("б")
			.done());

	/**
	 * Вертикальный радиус зоны постройки (Y) вниз от позиции бота.
	 * Блоки ниже этого порога не считаются достижимыми из текущей позиции.
	 */
	final Setting<Double> verticalRadius = settings.add(DoubleSetting
			.create("vertical-radius")
			.defaultValue(8)
			.range(1, 32)
			.sliderRange(1, 32)
			.decimalPlaces(1)
			.suffix("б")
			.done());

	/**
	 * Задержка между постановками блоков в тиках.
	 * Даёт серверу время обработать предыдущий клик перед следующим.
	 */
	final Setting<Double> placeDelay = settings.add(DoubleSetting
			.create("place-delay")
			.defaultValue(2)
			.range(0, 20)
			.sliderRange(0, 20)
			.decimalPlaces(0)
			.suffix("т")
			.done());

	/**
	 * Таймаут ожидания открытия шалкера в тиках.
	 * Если за это время сервер не прислал пакет открытия — повторяем клик.
	 */
	final Setting<Double> openTimeout = settings.add(DoubleSetting
			.create("open-timeout")
			.defaultValue(20)
			.range(5, 100)
			.sliderRange(5, 100)
			.decimalPlaces(0)
			.suffix("т")
			.done());

	/**
	 * Порог прочности элитры в процентах, при достижении которого
	 * контроллер взлёта автоматически меняет её на свежую из инвентаря.
	 */
	final Setting<Double> elytraSwapThreshold = settings.add(DoubleSetting
			.create("elytra-swap-threshold")
			.defaultValue(20)
			.range(1, 99)
			.sliderRange(1, 99)
			.decimalPlaces(0)
			.suffix("%")
			.done());

	/**
	 * Количество тиков без прогресса, после которого зона считается заблокированной
	 * и бот принудительно переходит к следующей.
	 */
	final Setting<Double> stuckThreshold = settings.add(DoubleSetting
			.create("stuck-threshold")
			.defaultValue(10)
			.range(1, 60)
			.sliderRange(1, 60)
			.decimalPlaces(0)
			.suffix("т")
			.done());

	/**
	 * Максимальное количество попыток поставить один блок.
	 * Если блок не появился за это число попыток — позиция пропускается.
	 */
	final Setting<Double> maxPlaceAttempts = settings.add(DoubleSetting
			.create("max-place-attempts")
			.defaultValue(6)
			.range(1, 20)
			.sliderRange(1, 20)
			.decimalPlaces(0)
			.done());

	/**
	 * Желаемый вектор перемещения игрока на элитрах за один игровой тик.
	 * <p>
	 * Контроллеры модуля (например, {@link TakeoffController}) выставляют это поле
	 * каждый тик, задавая направление и скорость полёта к следующей целевой точке.
	 * {@link org.duollectis.client.mixin.modules.mapart.ClientPlayerEntityMixin} перехватывает
	 * ванильный {@code LivingEntity#travel()} и применяет этот вектор напрямую через
	 * {@code move(SELF, velocity)}, полностью подавляя ванильную аэродинамику элитр и гравитацию.
	 * После применения velocity игрока обнуляется, чтобы исключить накопление инерции.
	 */
	@Getter
	@Setter
	@NotNull
	private Vec3d velocity = Vec3d.ZERO;

	private Stage stage = Stage.INIT_LAYERS;

	private int layerIndex = 0;
	private boolean buildStarted = false;

	/**
	 * Флаг: текущий RECHECK запущен после DEPOSIT — лишние блоки уже сданы,
	 * повторная проверка на излишки не нужна, сразу переходим в REFILL.
	 */
	private boolean recheckAfterDeposit = false;

	private final StoragesController storagesController = new StoragesController();
	private final SchemController schemController = new SchemController();
	private final BuildController buildController = new BuildController();
	private final RecheckController recheckController = new RecheckController();

	@Override
	protected void onDeactivate() {
		stage = Stage.INIT_LAYERS;
		layerIndex = 0;
		buildStarted = false;
		recheckAfterDeposit = false;
		velocity = Vec3d.ZERO;
	}

	@Override
	protected void onInit() {
		addController(new TakeoffController());
		addController(storagesController);
		addController(schemController);
		addController(buildController);
		addController(recheckController);
	}

	@Subscribe(TickPreEvent.class)
	private void onTick() {
		switch (stage) {
			case INIT_LAYERS -> {
				int count = schemController.scan();

				if (count == 0) {
					errorDisable("Не удалось собрать слои схемы.");
					return;
				}

				if (count == 128) {
					debugInfo("Слои успешно собраны.");
					stage = Stage.SCAN_STORAGES;
					return;
				}

				errorDisable("Неверное количество слоёв для мапарта: %s != 128".formatted(count));
			}
			case SCAN_STORAGES -> {
				if (storagesController.tick()) {
					stage = Stage.RECHECK;
					recheckController.start(schemController.getLayers());
				}
			}
			case DEPOSIT -> {
				if (storagesController.tick()) {
					recheckAfterDeposit = true;
					stage = Stage.RECHECK;
					recheckController.start(schemController.getLayers());
				}
			}
			case RECHECK -> {
				recheckController.tick(schemController.getLayers());

				if (recheckController.isDone()) {
					if (recheckAfterDeposit) {
						recheckAfterDeposit = false;
						stage = Stage.REFILL;
						return;
					}

					Map<Item, Integer> toKeep = buildExcessItems();

					if (toKeep.isEmpty()) {
						stage = Stage.REFILL;
					} else {
						debugInfo("[RECHECK] Сдаём лишние блоки, типов: " + toKeep.size() + ".");
						storagesController.startDeposit(toKeep);
						stage = Stage.DEPOSIT;
					}
				}
			}
			case REFILL -> startRefill();
			case COLLECTING -> {
				if (storagesController.tick()) {
					stage = Stage.BUILDING;
				}
			}
			case BUILDING -> {
				if (!buildStarted) {
					buildController.reset();
					buildController.start(schemController.getLayer(layerIndex).orElse(null));
					buildStarted = true;
				}

				buildController.tick();

				if (buildController.isDone()) {
					buildStarted = false;
					storagesController.resetCollect();

					debugInfo("[BUILDING] Слой " + layerIndex
							+ " skipped=" + buildController.wasSkipped());

					stage = Stage.RECHECK;
					recheckController.start(schemController.getLayers());
				}
			}
		}
	}

	/**
	 * Обрабатывает переход из стадии {@code REFILL}: ищет первый строимый слой,
	 * определяет нужные предметы и переходит в {@code COLLECTING} или {@code BUILDING}.
	 * Использует только реальные данные из {@link RecheckController} — никаких предположений
	 * о состоянии незагруженных чанков.
	 */
	private void startRefill() {
		layerIndex = findNextBuildableLayer();

		if (layerIndex >= schemController.size()) {
			infoDisable("Все слои построены!");
			return;
		}

		Map<Item, Integer> needed = calculateItems(layerIndex);

		if (needed.isEmpty()) {
			debugInfo("[REFILL] Предметы уже в инвентаре для слоя " + layerIndex + " — переход к BUILDING.");
			stage = Stage.BUILDING;
			return;
		}

		debugInfo("[REFILL] Нужно собрать " + needed.size() + " типов предметов для слоя " + layerIndex + ".");
		storagesController.startCollect(needed);
		stage = Stage.COLLECTING;
	}

	/**
	 * Сканирует слои и возвращает индекс первого, у которого есть незавершённые блоки
	 * И хотя бы один нужный предмет доступен (в инвентаре или в кэше хранилищ).
	 * Использует только данные {@link RecheckController} — гарантированно актуальные.
	 *
	 * @return индекс первого строимого слоя, или {@code schemController.size()} если таких нет
	 */
	private int findNextBuildableLayer() {
		Map<Item, Integer> playerInventory = buildPlayerInventory();
		Map<Item, Integer> storageInventory = buildStorageInventory();

		for (int i = 0; i < schemController.size(); i++) {
			Map<BlockState, Integer> pending = recheckController.getPending(i);

			if (pending.isEmpty()) {
				debugInfo("[SCAN] Слой " + i + " уже построен — пропускаем.");
				continue;
			}

			boolean hasAnyResource = false;

			for (Map.Entry<BlockState, Integer> entry : pending.entrySet()) {
				Item item = entry.getKey().getBlock().asItem();

				if (playerInventory.getOrDefault(item, 0) > 0
						|| storageInventory.getOrDefault(item, 0) > 0
				) {
					hasAnyResource = true;
					break;
				}
			}

			if (hasAnyResource) {
				return i;
			}

			debugInfo("[SCAN] Слой " + i + " — ресурсов нет нигде, пропускаем.");
		}

		return schemController.size();
	}

	/**
	 * Вычисляет крейсерскую высоту для перелёта над схемой.
	 * Равна максимальному Y среди всех блоков всех слоёв плюс {@code buildAboveHeight},
	 * чтобы бот гарантированно пролетал над уже построенными блоками.
	 *
	 * @return крейсерская высота в мировых координатах
	 */
	double getSchematicCruiseY() {
		int maxY = schemController.getLayers()
		                          .stream()
		                          .mapToInt(SchematicLayer::maxY)
		                          .max()
		                          .orElse(64);

		return maxY + buildAboveHeight.get();
	}

	private Map<Item, Integer> calculateItems(int index) {
		Map<BlockState, Integer> pending = recheckController.getPending(index);
		return toFilteredItemMap(pending, buildPlayerInventory(), buildStorageInventory());
	}

	/**
	 * Считает количество каждого предмета в инвентаре игрока (36 слотов).
	 * Результат: «предмет → суммарное количество у игрока».
	 */
	private static Map<Item, Integer> buildPlayerInventory() {
		Map<Item, Integer> inventory = new HashMap<>();

		for (int i = 0; i < 36; i++) {
			var stack = mc.player.getInventory().getStack(i);

			if (stack.isEmpty()) {
				continue;
			}

			inventory.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}

		return inventory;
	}

	/**
	 * Суммирует количество предметов каждого типа по всем хранилищам из кэша.
	 * Результат: «предмет → суммарное количество в хранилищах».
	 */
	private Map<Item, Integer> buildStorageInventory() {
		Map<Item, Integer> inventory = new HashMap<>();

		storagesController.cache
				.values()
				.stream()
				.flatMap(List::stream)
				.forEach(cached -> inventory.merge(cached.block.asItem(), cached.count, Integer::sum));

		return inventory;
	}

	/**
	 * Вычисляет целевое количество каждого предмета в инвентаре игрока для постройки слоя
	 * с учётом ограничения вместимости инвентаря (36 слотов).
	 *
	 * @param pending          карта «состояние блока → количество незавершённых»
	 * @param playerInventory  карта «предмет → количество у игрока»
	 * @param storageInventory карта «предмет → количество в хранилищах»
	 * @return карта «предмет → целевое количество в инвентаре после сбора»
	 */
	private static Map<Item, Integer> toFilteredItemMap(
			Map<BlockState, Integer> pending,
			Map<Item, Integer> playerInventory,
			Map<Item, Integer> storageInventory
	) {
		Map<Item, Integer> totalRequired = new LinkedHashMap<>();

		for (Map.Entry<BlockState, Integer> entry : pending.entrySet()) {
			totalRequired.merge(entry.getKey().getBlock().asItem(), entry.getValue(), Integer::sum);
		}

		int freeSlots = countFreeInventorySlots();
		Map<Item, Integer> needed = new HashMap<>();

		for (Map.Entry<Item, Integer> entry : totalRequired.entrySet()) {
			if (freeSlots <= 0) {
				break;
			}

			Item item = entry.getKey();
			int required = entry.getValue();
			int inInventory = playerInventory.getOrDefault(item, 0);
			int deficit = required - inInventory;

			if (deficit <= 0) {
				continue;
			}

			int inStorages = storageInventory.getOrDefault(item, 0);

			if (inStorages <= 0) {
				continue;
			}

			int maxStack = item.getMaxCount();
			int slotsNeeded = (int) Math.ceil((double) deficit / maxStack);
			int slotsToUse = Math.min(slotsNeeded, freeSlots);
			int canCollect = slotsToUse * maxStack;
			int toCollect = Math.min(deficit, Math.min(inStorages, canCollect));

			needed.put(item, inInventory + toCollect);
			freeSlots -= slotsToUse;
		}

		return needed;
	}

	private static int countFreeInventorySlots() {
		int usedSlots = 0;

		for (int i = 0; i < 36; i++) {
			if (!mc.player.getInventory().getStack(i).isEmpty()) {
				usedSlots++;
			}
		}

		return 36 - usedSlots;
	}

	/**
	 * Вычисляет карту «предмет → сколько оставить в инвентаре» для сдачи лишних блоков.
	 * Суммирует потребности по всем незавершённым слоям из данных {@link RecheckController}.
	 * Возвращает только те блочные предметы, которых в инвентаре больше, чем суммарно нужно.
	 */
	Map<Item, Integer> buildExcessItems() {
		Map<Item, Integer> totalNeeds = new HashMap<>();

		for (int i = 0; i < schemController.size(); i++) {
			Map<BlockState, Integer> pending = recheckController.getPending(i);

			for (Map.Entry<BlockState, Integer> entry : pending.entrySet()) {
				Item item = entry.getKey().getBlock().asItem();
				totalNeeds.merge(item, entry.getValue(), Integer::sum);
			}
		}

		debugInfo("[Excess] totalNeeds типов=" + totalNeeds.size());

		Map<Item, Integer> playerInventory = buildPlayerInventory();
		Map<Item, Integer> toKeep = new HashMap<>();

		for (Map.Entry<Item, Integer> entry : playerInventory.entrySet()) {
			Item item = entry.getKey();

			if (!(item instanceof BlockItem)) {
				continue;
			}

			int inInventory = entry.getValue();
			int keepCount = totalNeeds.getOrDefault(item, 0);

			if (inInventory > keepCount) {
				debugInfo("[Excess] Лишний предмет: " + item + " инв=" + inInventory + " нужно=" + keepCount);
				toKeep.put(item, keepCount);
			}
		}

		return toKeep;
	}

	private enum Stage {
		INIT_LAYERS,
		SCAN_STORAGES,
		RECHECK,
		REFILL,
		COLLECTING,
		BUILDING,
		DEPOSIT
	}
}
