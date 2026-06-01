package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.fsm.StateMachine;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.api.tick.Cooldown;
import org.duollectis.client.utils.network.PacketUtil;
import org.duollectis.client.utils.network.ScreenHandlerUtil;
import org.duollectis.client.utils.render.RenderUtils;
import org.duollectis.client.utils.rotation.Rotations;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Контроллер постройки одного слоя схемы по зонному принципу.
 * <p>
 * Алгоритм:
 * <ol>
 *   <li>Летит к первому незавершённому блоку слоя.</li>
 *   <li>Ставит все блоки из всех колонок, попадающие в {@code horizontalRadius} вокруг текущей позиции.</li>
 *   <li>Когда в радиусе нечего ставить — ищет первый незавершённый блок за пределами радиуса и летит к нему.</li>
 *   <li>Когда незавершённых блоков не осталось — переходит в {@link Stage#DONE}.</li>
 * </ol>
 */
class BuildController extends Controller<MapArtBuilder> {

	private final FlyController flyController = new FlyController();

	private final StateMachine<Stage> fsm = StateMachine
			.start(Stage.DONE)
			.on(Stage.FLYING, this::tickFlying)
			.on(Stage.BUILD, this::tickBuild)
			.terminal(Stage.DONE)
			.build();

	@Nullable
	private SchematicLayer layer;

	private double cruiseY;

	/**
	 * Зафиксированный целевой блок для текущего цикла поворот→swap→постановка.
	 * Сбрасывается после успешной постановки или при переходе к следующей зоне.
	 * Без фиксации lowest меняется каждый тик и постановка никогда не завершается.
	 */
	@Nullable
	private SchematicLayer.BlockEntry targetEntry;

	/**
	 * Флаг: на текущем тике уже отправлен поворот к блоку, постановка — на следующем тике.
	 */
	private boolean rotatedToBlock;

	/**
	 * Флаг: в этом тике был выполнен SWAP предмета из инвентаря в хотбар.
	 * Постановка блока откладывается на следующий тик, чтобы сервер успел обновить инвентарь.
	 */
	private boolean pendingSwap;

	private final Cooldown placeCooldown = new Cooldown();

	/**
	 * Позиция блока, к которому бот пытается подобраться в текущей зоне.
	 * Если позиция не меняется {@code stuckThreshold} тиков подряд — зона заблокирована
	 * и бот принудительно переходит к следующей.
	 */
	@Nullable
	private BlockPos lastAttemptedPos;

	private int stuckTicks;

	/**
	 * Позиции блоков, которые бот пытался поставить {@code maxPlaceAttempts} раз,
	 * но блок так и не появился в мире. Такие позиции пропускаются до конца стройки слоя.
	 */
	private final Set<BlockPos> skippedPositions = new HashSet<>();

	private int placeAttempts;

	/**
	 * {@code true} если последний {@link #start} обнаружил что слой уже построен
	 * и реальная стройка не начиналась — {@link #findFirstPendingBlock()} вернул {@code null}.
	 */
	private boolean skipped;

	@Override
	protected void onDeactivate() {
		layer = null;
		cruiseY = 0;
		fsm.reset();
		targetEntry = null;
		rotatedToBlock = false;
		pendingSwap = false;
		placeCooldown.reset();
		lastAttemptedPos = null;
		stuckTicks = 0;
		skippedPositions.clear();
		placeAttempts = 0;
		skipped = false;
		flyController.reset();
	}

	@Override
	protected void onInit() {
		addController(flyController);
	}

	boolean isDone() {
		return fsm.is(Stage.DONE);
	}

	/**
	 * Возвращает {@code true} если последний вызов {@link #start} обнаружил,
	 * что слой уже полностью построен и реальная стройка не начиналась.
	 * Используется в {@link MapArtBuilder} для пропуска цикла сбора ресурсов.
	 */
	boolean wasSkipped() {
		return skipped;
	}

	void reset() {
		layer = null;
		fsm.reset();
		targetEntry = null;
		rotatedToBlock = false;
		pendingSwap = false;
		placeCooldown.reset();
		lastAttemptedPos = null;
		stuckTicks = 0;
		skippedPositions.clear();
		placeAttempts = 0;
		skipped = false;
		flyController.reset();
	}

	void start(@Nullable SchematicLayer layer) {
		if (this.layer != null || layer == null) {
			return;
		}

		this.layer = layer;
		this.cruiseY = layer.maxY() + module.buildAboveHeight.get();
		this.rotatedToBlock = false;
		this.lastAttemptedPos = null;
		this.stuckTicks = 0;
		this.skipped = false;

		SchematicLayer.BlockEntry firstPending = findFirstPendingBlock();

		if (firstPending == null) {
			debugInfo("СТРОЙКА: нет доступных блоков, пропускаем слой");
			this.layer = null;
			this.skipped = true;
			return;
		}

		debugInfo("НАЧИНАЕМ СТРОЙКУ");
		fsm.restart(Stage.FLYING);
		flyController.start(firstPending.pos(), cruiseY);
	}

	void tick() {
		if (layer == null || fsm.is(Stage.DONE)) {
			return;
		}

		placeCooldown.tick();

		if (placeCooldown.isActive()) {
			return;
		}

		fsm.tick();
	}

	private void tickFlying() {
		if (flyController.isDone()) {
			fsm.go(Stage.BUILD);
			targetEntry = null;
			rotatedToBlock = false;
			pendingSwap = false;
			placeCooldown.reset();
			lastAttemptedPos = null;
			stuckTicks = 0;
			placeAttempts = 0;
			return;
		}

		flyController.onTick();
	}

	/**
	 * Ставит все блоки в радиусе {@code horizontalRadius} вокруг текущей позиции.
	 * Если блок в радиусе есть, но вне радиуса взаимодействия — подлетает к нему (микро-перелёт).
	 * Когда в радиусе нечего ставить — ищет следующую незавершённую зону и летит к ней.
	 * Если незавершённых блоков нет вообще — завершает слой.
	 */
	private void tickBuild() {
		boolean placed = placeAllInRange();

		if (placed) {
			stuckTicks = 0;
			return;
		}

		if (mc.player != null
				&& mc.player.currentScreenHandler != null
				&& mc.player.currentScreenHandler != mc.player.playerScreenHandler
		) {
			mc.player.closeHandledScreen();
			return;
		}

		SchematicLayer.BlockEntry nextInRadius = findLowestInRadius();

		if (nextInRadius != null) {
			if (nextInRadius.pos().equals(lastAttemptedPos)) {
				stuckTicks++;
			} else {
				lastAttemptedPos = nextInRadius.pos();
				stuckTicks = 0;
			}

			if (stuckTicks >= module.stuckThreshold.get().intValue()) {
				debugInfo("[Build] ЗОНА ЗАБЛОКИРОВАНА (" + stuckTicks + " тиков) — переходим к следующей");
				lastAttemptedPos = null;
				stuckTicks = 0;
				flyToNextZone();
				return;
			}

			debugInfo("[Build] Блок в радиусе, но вне досягаемости — подлетаем: " + nextInRadius.pos());
			fsm.go(Stage.FLYING);
			targetEntry = null;
			rotatedToBlock = false;
			pendingSwap = false;
			flyController.startZoneHop(
					nextInRadius.pos(),
					calcPathClearanceY(mc.player.getBlockPos(), nextInRadius.pos())
			);
			return;
		}

		debugInfo("[Build] В радиусе нечего ставить — летим к следующей зоне"
				+ " playerY=" + String.format("%.1f", mc.player.getY())
				+ " hRadius=" + module.horizontalRadius.get()
				+ " vRadius=" + module.verticalRadius.get());
		flyToNextZone();
	}

	/**
	 * Ищет ближайший незавершённый блок за пределами текущего {@code horizontalRadius} и летит к нему
	 * через крейсерскую высоту, чтобы не врезаться в уже построенные блоки схемы.
	 * Если таких блоков нет — завершает слой.
	 */
	private void flyToNextZone() {
		SchematicLayer.BlockEntry nextTarget = findNearestPendingOutsideRadius();

		if (nextTarget == null) {
			debugInfo("ЗАКАНЧИВАЕМ СТРОЙКУ");
			layer = null;
			fsm.go(Stage.DONE);
			module.setVelocity(Vec3d.ZERO);
			return;
		}

		debugInfo("ЛЕТИМ К СЛЕДУЮЩЕЙ ЗОНЕ: " + nextTarget.pos());
		fsm.go(Stage.FLYING);
		targetEntry = null;
		rotatedToBlock = false;
		pendingSwap = false;
		lastAttemptedPos = null;
		stuckTicks = 0;
		flyController.startZoneHop(nextTarget.pos(), calcPathClearanceY(mc.player.getBlockPos(), nextTarget.pos()));
	}

	/**
	 * Фиксирует целевой блок (минимальный Y в радиусе) и проводит его через цикл:
	 * поворот → (опционально) swap предмета → задержка → постановка.
	 * Целевой блок фиксируется в {@link #targetEntry} и не меняется до завершения постановки,
	 * чтобы исключить рассинхронизацию между тиком поворота и тиком постановки.
	 *
	 * @return {@code true} если за этот тик выполнено действие (поворот, ожидание или постановка)
	 */
	private boolean placeAllInRange() {
		if (targetEntry != null) {
			BlockPos pos = targetEntry.pos();
			boolean alreadyPlaced = !mc.world.getBlockState(pos).isAir();
			boolean outOfRange = !isInBuildRadius(pos);

			if (alreadyPlaced || outOfRange) {
				debugInfo("[Build] targetEntry инвалидирован: pos=" + pos
						+ " alreadyPlaced=" + alreadyPlaced
						+ " outOfRange=" + outOfRange);
				targetEntry = null;
				rotatedToBlock = false;
				pendingSwap = false;
				placeAttempts = 0;
			}
		}

		if (targetEntry == null) {
			targetEntry = findLowestPlaceable();

			if (targetEntry == null) {
				debugInfo("[Build] findLowestPlaceable вернул null — нет блоков в радиусе");
				return false;
			}

			placeAttempts = 0;
			debugInfo("[Build] Новый targetEntry: " + targetEntry.pos()
					+ " block=" + targetEntry.state().getBlock()
					+ " hasItem=" + hasItemForBlock(targetEntry.state()));
		}

		if (rotatedToBlock) {
			if (pendingSwap) {
				debugInfo("[Build] Ожидаем swap, пропускаем тик постановки");
				pendingSwap = false;
				return true;
			}

			placeAttempts++;

			if (placeAttempts > module.maxPlaceAttempts.get().intValue()
					&& mc.world.getBlockState(targetEntry.pos()).isAir()
			) {
				debugInfo("[Build] ВАТЧДОГ: блок " + targetEntry.pos()
						+ " не появился за " + placeAttempts + " попыток — пропускаем");
				skippedPositions.add(targetEntry.pos());
				targetEntry = null;
				rotatedToBlock = false;
				placeAttempts = 0;
				return true;
			}

			debugInfo("[Build] Ставим блок: " + targetEntry.pos() + " (попытка " + placeAttempts + ")");
			placeBlock(targetEntry);
			targetEntry = null;
			rotatedToBlock = false;
			placeCooldown.set((int) module.placeDelay.get().doubleValue());
			return true;
		}

		debugInfo("[Build] Поворачиваемся к блоку: " + targetEntry.pos());
		rotateToBlock(targetEntry);
		rotatedToBlock = true;
		return true;
	}

	/**
	 * Ищет следующий блок для постановки с соблюдением порядка снизу вверх внутри каждой колонки.
	 * Для каждой колонки берётся только первый (нижний) незавершённый блок с опорой и предметом.
	 * Среди всех таких кандидатов выбирается тот, у которого минимальный Y.
	 * Позиции из {@link #skippedPositions} пропускаются.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findLowestPlaceable() {
		SchematicLayer.BlockEntry lowest = null;
		int candidateCount = 0;

		for (int i = 0; i < layer.columnCount(); i++) {
			SchematicLayer.BlockEntry candidate = findLowestReadyInColumn(i);

			if (candidate == null) {
				continue;
			}

			candidateCount++;

			if (lowest == null || candidate.pos().getY() < lowest.pos().getY()) {
				lowest = candidate;
			}
		}

		debugInfo("[Find] findLowestPlaceable: кандидатов=" + candidateCount
				+ " результат=" + (lowest == null ? "null" : lowest.pos()));

		return lowest;
	}

	/**
	 * Возвращает нижний блок в колонке, который готов к постановке:
	 * все блоки ниже него уже поставлены, он сам — воздух, предмет есть, в радиусе.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findLowestReadyInColumn(int columnIndex) {
		for (SchematicLayer.BlockEntry entry : layer.column(columnIndex)) {
			BlockPos pos = entry.pos();

			if (!mc.world.getBlockState(pos).isAir()) {
				continue;
			}

			if (skippedPositions.contains(pos)) {
				continue;
			}

			boolean inRange = isInBuildRadius(pos);
			boolean hasItem = hasItemForBlock(entry.state());

			if (!inRange || !hasItem) {
				debugInfo("[Find] ПРОПУСК " + pos
						+ " block=" + entry.state().getBlock()
						+ " inRange=" + inRange
						+ " hasItem=" + hasItem);
				return null;
			}

			return entry;
		}

		return null;
	}

	/**
	 * Ищет нижний (минимальный Y) незавершённый блок с предметом в пределах {@code horizontalRadius},
	 * независимо от {@code interactRadius}. Используется для микро-перелёта внутри зоны
	 * когда блок в радиусе постройки, но вне радиуса взаимодействия.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findLowestInRadius() {
		SchematicLayer.BlockEntry lowest = null;

		for (int i = 0; i < layer.columnCount(); i++) {
			SchematicLayer.BlockEntry candidate = findLowestPendingEntryInColumn(i);

			if (candidate == null) {
				continue;
			}

			if (!isInBuildRadius(candidate.pos())) {
				continue;
			}

			if (lowest == null || candidate.pos().getY() < lowest.pos().getY()) {
				lowest = candidate;
			}
		}

		return lowest;
	}

	/**
	 * Возвращает нижний незавершённый блок в колонке, игнорируя радиус взаимодействия.
	 * Используется как базовый кандидат для проверки радиуса и перелёта.
	 * <p>
	 * Прерывает колонку (возвращает {@code null}) если нижний незавершённый блок не имеет
	 * предмета в инвентаре — нельзя лететь к верхнему блоку (ковру) пока не поставлена опора.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findLowestPendingEntryInColumn(int columnIndex) {
		for (SchematicLayer.BlockEntry entry : layer.column(columnIndex)) {
			BlockPos pos = entry.pos();

			if (!mc.world.getBlockState(pos).isAir()) {
				continue;
			}

			if (skippedPositions.contains(pos)) {
				continue;
			}

			if (!hasItemForBlock(entry.state())) {
				return null;
			}

			return entry;
		}

		return null;
	}

	/**
	 * Ищет нижний pending блок в ближайшей к игроку колонке за пределами {@code horizontalRadius}.
	 * Колонка считается "за пределами" только если её нижний pending блок не в радиусе —
	 * это гарантирует что бот не улетит к колонке у которой нижний блок ещё в радиусе.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findNearestPendingOutsideRadius() {
		Vec3d playerPos = mc.player.getEntityPos();
		int nearestColumnIndex = -1;
		double nearestDistSq = Double.MAX_VALUE;

		for (int i = 0; i < layer.columnCount(); i++) {
			SchematicLayer.BlockEntry lowestPending = findLowestPendingEntryInColumn(i);

			if (lowestPending == null) {
				continue;
			}

			if (isInBuildRadius(lowestPending.pos())) {
				continue;
			}

			BlockPos anyPos = layer.column(i).get(0).pos();
			double dx = anyPos.getX() + 0.5 - playerPos.x;
			double dz = anyPos.getZ() + 0.5 - playerPos.z;
			double distSq = dx * dx + dz * dz;

			if (distSq < nearestDistSq) {
				nearestDistSq = distSq;
				nearestColumnIndex = i;
			}
		}

		return nearestColumnIndex < 0 ? null : findLowestPendingEntryInColumn(nearestColumnIndex);
	}

	/**
	 * Ищет нижний pending блок в ближайшей к игроку колонке по всему слою.
	 * Используется при старте для определения начальной позиции.
	 */
	@Nullable
	private SchematicLayer.BlockEntry findFirstPendingBlock() {
		Vec3d playerPos = mc.player.getEntityPos();
		int nearestColumnIndex = -1;
		double nearestDistSq = Double.MAX_VALUE;

		for (int i = 0; i < layer.columnCount(); i++) {
			// Колонка считается строимой только если нижний незавершённый блок имеет предмет.
			// Если предмета нет — колонка заблокирована (нельзя ставить верхние блоки без опоры).
			if (findLowestPendingEntryInColumn(i) == null) {
				continue;
			}

			BlockPos anyPos = layer.column(i).get(0).pos();
			double dx = anyPos.getX() + 0.5 - playerPos.x;
			double dz = anyPos.getZ() + 0.5 - playerPos.z;
			double distSq = dx * dx + dz * dz;

			if (distSq < nearestDistSq) {
				nearestDistSq = distSq;
				nearestColumnIndex = i;
			}
		}

		return nearestColumnIndex < 0 ? null : findLowestPendingEntryInColumn(nearestColumnIndex);
	}

	private boolean hasItemForBlock(BlockState state) {
		Item needed = state.getBlock().asItem();

		for (int slot = 0; slot < 36; slot++) {
			if (mc.player.getInventory().getStack(slot).isOf(needed)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Вычисляет минимальную высоту подъёма для безопасного перелёта от {@code from} до {@code to}.
	 * Сканирует все блоки слоя, чья XZ-проекция попадает в полосу шириной 1.5 блока вдоль маршрута,
	 * и возвращает максимальный Y среди них плюс {@code buildAboveHeight}.
	 * Если блоков на пути нет — возвращает текущую высоту игрока (подъём не нужен).
	 */
	private double calcPathClearanceY(BlockPos from, BlockPos to) {
		double fx = from.getX() + 0.5;
		double fz = from.getZ() + 0.5;
		double tx = to.getX() + 0.5;
		double tz = to.getZ() + 0.5;
		double segDx = tx - fx;
		double segDz = tz - fz;
		double segLenSq = segDx * segDx + segDz * segDz;

		int maxBlockY = Integer.MIN_VALUE;

		for (int i = 0; i < layer.columnCount(); i++) {
			for (SchematicLayer.BlockEntry entry : layer.column(i)) {
				if (mc.world.getBlockState(entry.pos()).isAir()) {
					continue;
				}

				BlockPos pos = entry.pos();
				double px = pos.getX() + 0.5 - fx;
				double pz = pos.getZ() + 0.5 - fz;

				double distToSegSq = segLenSq < 1e-9
				                     ? px * px + pz * pz
				                     : distToSegmentSq(px, pz, segDx, segDz, segLenSq);

				if (distToSegSq <= 2.25) {
					maxBlockY = Math.max(maxBlockY, pos.getY());
				}
			}
		}

		return maxBlockY == Integer.MIN_VALUE
		       ? mc.player.getY()
		       : maxBlockY + 1 + module.buildAboveHeight.get();
	}

	/**
	 * Квадрат расстояния от точки {@code (px, pz)} до отрезка {@code (0,0)→(segDx,segDz)}.
	 */
	private static double distToSegmentSq(double px, double pz, double segDx, double segDz, double segLenSq) {
		double t = Math.max(0, Math.min(1, (px * segDx + pz * segDz) / segLenSq));
		double closestX = t * segDx - px;
		double closestZ = t * segDz - pz;
		return closestX * closestX + closestZ * closestZ;
	}

	/**
	 * Проверяет попадание блока в цилиндрическую зону постройки.
	 * XZ-расстояние ограничено {@code horizontalRadius}, Y-расстояние вниз — {@code verticalRadius}.
	 */
	private boolean isInBuildRadius(BlockPos pos) {
		double hRadius = module.horizontalRadius.get();
		double vRadius = module.verticalRadius.get();
		Vec3d playerPos = mc.player.getEntityPos();
		double dx = pos.getX() + 0.5 - playerPos.x;
		double dz = pos.getZ() + 0.5 - playerPos.z;
		double dy = playerPos.y - pos.getY();

		return dx * dx + dz * dz <= hRadius * hRadius && dy >= 0 && dy <= vRadius;
	}

	/**
	 * Поворачивает игрока к точке клика, вычисленной с учётом оси целевого блока.
	 */
	private void rotateToBlock(SchematicLayer.BlockEntry entry) {
		BlockHitResult hit = buildHitResult(entry);
		PacketUtil.rotate(
				Rotations.getYawToPoint(hit.getPos()),
				Rotations.getPitchToPoint(hit.getPos())
		);
	}

	/**
	 * Ставит блок на позицию {@code entry.pos()} через {@code interactionManager}.
	 * Грань клика выбирается в зависимости от свойства {@code axis} целевого блока:
	 * {@code Y} → верхняя грань, {@code X} → восточная/западная, {@code Z} → южная/северная.
	 */
	private void placeBlock(SchematicLayer.BlockEntry entry) {
		if (mc.interactionManager == null) {
			return;
		}

		if (!equipItem(entry.state())) {
			return;
		}

		BlockHitResult hit = buildHitResult(entry);

		debugInfo("[Build] interactBlock: pos=" + entry.pos() + " face=" + hit.getSide() + " hitPoint=" + hit.getPos());

		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
	}

	/**
	 * Вычисляет {@link BlockHitResult} для постановки блока с учётом свойства {@code axis}.
	 * <p>
	 * Если блок имеет свойство {@code axis=X} — ищет поставленный сосед по оси X и кликает
	 * на его боковую грань; аналогично для {@code axis=Z}. Если подходящего соседа нет —
	 * кликает на нижний блок с нужной боковой грани. Для {@code axis=Y} и блоков без оси —
	 * стандартный клик на верхнюю грань нижнего блока.
	 */
	private BlockHitResult buildHitResult(SchematicLayer.BlockEntry entry) {
		BlockPos pos = entry.pos();
		BlockState state = entry.state();

		if (state.contains(Properties.AXIS)) {
			Direction.Axis axis = state.get(Properties.AXIS);

			if (axis == Direction.Axis.X) {
				return buildSideHit(pos, Direction.EAST, Direction.WEST);
			}

			if (axis == Direction.Axis.Z) {
				return buildSideHit(pos, Direction.SOUTH, Direction.NORTH);
			}
		}

		Vec3d hitPoint = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return new BlockHitResult(hitPoint, Direction.UP, pos, false);
	}

	/**
	 * Строит {@link BlockHitResult} для постановки блока с горизонтальной осью.
	 * Ищет поставленный сосед в направлении {@code primary}, затем {@code secondary}.
	 * Если ни одного нет — кликает на нижний блок с грани {@code primary}.
	 */
	private BlockHitResult buildSideHit(BlockPos pos, Direction primary, Direction secondary) {
		BlockPos primaryNeighbor = pos.offset(primary);
		BlockPos secondaryNeighbor = pos.offset(secondary);

		if (!mc.world.getBlockState(primaryNeighbor).isAir()) {
			Vec3d hitPoint = Vec3d.ofCenter(primaryNeighbor).add(
					secondary.getOffsetX() * 0.5,
					0,
					secondary.getOffsetZ() * 0.5
			);
			return new BlockHitResult(hitPoint, secondary, primaryNeighbor, false);
		}

		if (!mc.world.getBlockState(secondaryNeighbor).isAir()) {
			Vec3d hitPoint = Vec3d.ofCenter(secondaryNeighbor).add(
					primary.getOffsetX() * 0.5,
					0,
					primary.getOffsetZ() * 0.5
			);
			return new BlockHitResult(hitPoint, primary, secondaryNeighbor, false);
		}

		// Нет соседей — кликаем на нижний блок с боковой грани primary
		Vec3d hitPoint = new Vec3d(
				pos.getX() + 0.5 + primary.getOffsetX() * 0.5,
				pos.getY(),
				pos.getZ() + 0.5 + primary.getOffsetZ() * 0.5
		);
		return new BlockHitResult(hitPoint, primary, pos, false);
	}

	/**
	 * Выбирает предмет для постановки блока в активный слот хотбара.
	 * Сначала ищет в хотбаре (0–8), затем свапает из основного инвентаря (9–35).
	 *
	 * @return {@code true} если предмет найден и выбран
	 */
	private boolean equipItem(BlockState state) {
		Item needed = state.getBlock().asItem();

		for (int slot = 0; slot < 9; slot++) {
			if (mc.player.getInventory().getStack(slot).isOf(needed)) {
				mc.player.getInventory().setSelectedSlot(slot);
				return true;
			}
		}

		for (int slot = 9; slot < 36; slot++) {
			if (mc.player.getInventory().getStack(slot).isOf(needed)) {
				int hotbarSlot = mc.player.getInventory().getSelectedSlot();
				ScreenHandlerUtil.clickSlot(
						mc.player.currentScreenHandler,
						slot,
						hotbarSlot,
						SlotActionType.SWAP,
						mc.player
				);
				pendingSwap = true;
				return true;
			}
		}

		return false;
	}

	@Subscribe(RenderEvent.class)
	public void onRender(RenderEvent event) {
		if (targetEntry == null) {
			return;
		}

		RenderUtils.drawLinesBox(event.getMatrices(), targetEntry.pos(), 0xFFFFFF00, true);
	}

	private enum Stage {
		FLYING,
		BUILD,
		DONE
	}
}
