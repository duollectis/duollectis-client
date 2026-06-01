package org.duollectis.client.modules.mapartbuilder3;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.api.tick.Cooldown;
import org.duollectis.client.utils.network.PacketUtil;
import org.duollectis.client.utils.rotation.Rotations;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Тупой принтер схемы.
 * Каждый тик ищет ближайший незавершённый блок в радиусе взаимодействия,
 * поворачивается к нему, экипирует нужный предмет и ставит блок.
 * Соблюдает ориентацию по свойству {@code axis} (бёрёзовые брёвна, кварц и т.д.).
 */
class PrintController extends Controller<MapArtBuilder3> {

	private static final int INVENTORY_SIZE = 36;

	private final Cooldown placeCooldown = new Cooldown();

	/**
	 * Зафиксированная цель текущего цикла поворот → постановка.
	 * Без фиксации цель меняется каждый тик и постановка никогда не завершается.
	 */
	@Nullable
	private BlockPos target;

	/**
	 * Флаг: в этом тике уже отправлен поворот, постановка — на следующем тике.
	 */
	private boolean rotated;

	/**
	 * Флаг: в этом тике был выполнен swap предмета из инвентаря в хотбар.
	 * Постановка откладывается ещё на один тик, чтобы сервер успел обновить инвентарь.
	 */
	private boolean pendingSwap;

	/**
	 * Позиции, которые бот пытался поставить {@code maxPlaceAttempts} раз,
	 * но блок так и не появился — пропускаются до следующего включения.
	 */
	private final Set<BlockPos> skipped = new HashSet<>();

	private int placeAttempts;

	@Override
	protected void onDeactivate() {
		placeCooldown.reset();
		target = null;
		rotated = false;
		pendingSwap = false;
		skipped.clear();
		placeAttempts = 0;
	}

	@Subscribe(TickPreEvent.class)
	private void onTick() {
		if (mc.player == null || mc.world == null || mc.interactionManager == null) {
			return;
		}

		placeCooldown.tick();

		if (placeCooldown.isActive()) {
			return;
		}

		tickPlace();
	}

	/**
	 * Один цикл постановки: фиксируем цель → поворот → (swap) → клик.
	 * Если цель уже поставлена или вышла из радиуса — сбрасываем и ищем новую.
	 */
	private void tickPlace() {
		if (target != null) {
			boolean placed = !mc.world.getBlockState(target).isAir();
			boolean outOfRange = !isInInteractRadius(target);

			if (placed || outOfRange) {
				resetTarget();
			}
		}

		if (target == null) {
			target = findNextTarget();
			placeAttempts = 0;

			if (target == null) {
				return;
			}
		}

		if (rotated) {
			if (pendingSwap) {
				pendingSwap = false;
				return;
			}

			placeAttempts++;

			if (placeAttempts > module.maxPlaceAttempts.get().intValue()
					&& mc.world.getBlockState(target).isAir()
			) {
				debugInfo("[Print] Ватчдог: блок " + target + " не появился за " + placeAttempts + " попыток — пропускаем");
				skipped.add(target);
				resetTarget();
				return;
			}

			BlockState state = getSchematicState(target);

			if (state == null) {
				resetTarget();
				return;
			}

			if (!equipItem(state)) {
				resetTarget();
				return;
			}

			BlockHitResult hit = buildHitResult(target, state);
			mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
			placeCooldown.set(module.placeDelay.get().intValue());
			resetTarget();
			return;
		}

		BlockState state = getSchematicState(target);

		if (state == null) {
			resetTarget();
			return;
		}

		BlockHitResult hit = buildHitResult(target, state);
		PacketUtil.rotate(
				Rotations.getYawToPoint(hit.getPos()),
				Rotations.getPitchToPoint(hit.getPos())
		);
		rotated = true;
	}

	private void resetTarget() {
		target = null;
		rotated = false;
		pendingSwap = false;
	}

	/**
	 * Ищет ближайший незавершённый блок схемы в радиусе взаимодействия,
	 * для которого есть предмет в инвентаре.
	 * Блоки снизу вверх по Y — сначала опора, потом ковёр.
	 */
	@Nullable
	private BlockPos findNextTarget() {
		SchematicPlacement placement = DataManager.getSchematicPlacementManager()
				.getSelectedSchematicPlacement();

		if (placement == null) {
			return null;
		}

		LitematicaSchematic schematic = placement.getSchematic();
		BlockPos origin = placement.getOrigin();
		Vec3d playerPos = mc.player.getEntityPos();
		double radius = module.interactRadius.get();

		List<BlockPos> candidates = new ArrayList<>();

		for (String regionName : schematic.getAreaPositions().keySet()) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

			if (container == null) {
				continue;
			}

			Vec3i size = container.getSize();
			BlockPos regionOffset = schematic.getSubRegionPosition(regionName);

			int originX = origin.getX() + regionOffset.getX();
			int originY = origin.getY() + regionOffset.getY();
			int originZ = origin.getZ() + regionOffset.getZ();

			int stepX = size.getX() >= 0 ? 1 : -1;
			int stepY = size.getY() >= 0 ? 1 : -1;
			int stepZ = size.getZ() >= 0 ? 1 : -1;
			int absX = Math.abs(size.getX());
			int absY = Math.abs(size.getY());
			int absZ = Math.abs(size.getZ());

			for (int ix = 0; ix < absX; ix++) {
				for (int iz = 0; iz < absZ; iz++) {
					for (int iy = 0; iy < absY; iy++) {
						int lx = ix * stepX;
						int ly = iy * stepY;
						int lz = iz * stepZ;

						BlockState state = container.get(lx, ly, lz);

						if (state == null || state.isOf(Blocks.AIR) || state.getBlock().asItem() == Blocks.AIR.asItem()) {
							continue;
						}

						BlockPos pos = new BlockPos(originX + lx, originY + ly, originZ + lz);

						if (!mc.world.getBlockState(pos).isAir()) {
							continue;
						}

						if (skipped.contains(pos)) {
							continue;
						}

						double dx = pos.getX() + 0.5 - playerPos.x;
						double dy = pos.getY() + 0.5 - playerPos.y;
						double dz = pos.getZ() + 0.5 - playerPos.z;

						if (dx * dx + dy * dy + dz * dz > radius * radius) {
							continue;
						}

						if (!hasItemForBlock(state)) {
							continue;
						}

						candidates.add(pos);
					}
				}
			}
		}

		if (candidates.isEmpty()) {
			return null;
		}

		// Сначала нижние блоки (опора), потом верхние (ковёр)
		candidates.sort(Comparator.comparingInt(BlockPos::getY));
		return candidates.get(0);
	}

	/**
	 * Строит {@link BlockHitResult} с учётом свойства {@code axis} целевого блока.
	 * <ul>
	 *   <li>{@code axis=Y} или нет свойства — клик на верхнюю грань нижнего блока.</li>
	 *   <li>{@code axis=X} — ищет поставленного соседа по оси X, кликает на его боковую грань.</li>
	 *   <li>{@code axis=Z} — аналогично по оси Z.</li>
	 * </ul>
	 */
	private BlockHitResult buildHitResult(BlockPos pos, BlockState state) {
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
	 * Строит {@link BlockHitResult} для блока с горизонтальной осью.
	 * Ищет поставленного соседа в направлении {@code primary}, затем {@code secondary}.
	 * Если соседей нет — кликает на нижний блок с грани {@code primary}.
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

		for (int slot = 9; slot < INVENTORY_SIZE; slot++) {
			if (mc.player.getInventory().getStack(slot).isOf(needed)) {
				int hotbarSlot = mc.player.getInventory().getSelectedSlot();
				mc.interactionManager.clickSlot(
						mc.player.currentScreenHandler.syncId,
						slot,
						hotbarSlot,
						net.minecraft.screen.slot.SlotActionType.SWAP,
						mc.player
				);
				pendingSwap = true;
				return true;
			}
		}

		return false;
	}

	private boolean hasItemForBlock(BlockState state) {
		Item needed = state.getBlock().asItem();

		for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
			if (mc.player.getInventory().getStack(slot).isOf(needed)) {
				return true;
			}
		}

		return false;
	}

	private boolean isInInteractRadius(BlockPos pos) {
		double radius = module.interactRadius.get();
		Vec3d playerPos = mc.player.getEntityPos();
		double dx = pos.getX() + 0.5 - playerPos.x;
		double dy = pos.getY() + 0.5 - playerPos.y;
		double dz = pos.getZ() + 0.5 - playerPos.z;
		return dx * dx + dy * dy + dz * dz <= radius * radius;
	}

	/**
	 * Возвращает целевое состояние блока из схемы для указанной мировой позиции,
	 * или {@code null} если позиция не входит ни в один регион схемы.
	 */
	@Nullable
	private BlockState getSchematicState(BlockPos worldPos) {
		SchematicPlacement placement = DataManager
				.getSchematicPlacementManager()
				.getSelectedSchematicPlacement();

		if (placement == null) {
			return null;
		}

		LitematicaSchematic schematic = placement.getSchematic();
		BlockPos origin = placement.getOrigin();

		for (String regionName : schematic.getAreaPositions().keySet()) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

			if (container == null) {
				continue;
			}

			BlockPos regionOffset = schematic.getSubRegionPosition(regionName);
			int lx = worldPos.getX() - origin.getX() - regionOffset.getX();
			int ly = worldPos.getY() - origin.getY() - regionOffset.getY();
			int lz = worldPos.getZ() - origin.getZ() - regionOffset.getZ();

			Vec3i size = container.getSize();
			int absX = Math.abs(size.getX());
			int absY = Math.abs(size.getY());
			int absZ = Math.abs(size.getZ());

			if (lx < 0 || lx >= absX || ly < 0 || ly >= absY || lz < 0 || lz >= absZ) {
				continue;
			}

			return container.get(lx, ly, lz);
		}

		return null;
	}
}
