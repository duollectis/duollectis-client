package org.duollectis.client.modules;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.utils.color.Color;
import org.duollectis.client.utils.render.RenderUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Визуализирует зоны всех размещённых схем Litematica в мире:
 * рисует рамку выбранной схемы, помечает края засечками, предупреждает
 * о биомах со снегом для каждой схемы и отображает сетку карт 128×128.
 *
 * <p>Границы карт zoom=0 вычисляются по формуле Minecraft:
 * {@code centerX = gridX * 128}, {@code minX = centerX - 64 = gridX * 128 - 64}.
 * Линии сетки проходят через {@code ..., -192, -64, 64, 192, 320, ...}
 */
@ModuleInfo(
		value = "mapart-zone-viewer",
		category = Category.MISC
)
public class MapArtZoneViewer extends Module {

	private static final int MAP_SIZE = 128;
	/** Смещение левого края карты от её центра: minX = centerX - MAP_HALF */
	private static final int MAP_HALF = 64;
	private static final int SNOW_CHECK_INTERVAL = 100;
	private static final int SHADOW_ROW = 1;
	private static final float BORDER_LINE_WIDTH = 3.0f;
	private static final float EDGE_LINE_WIDTH = 2.0f;
	private static final float SNOW_LINE_WIDTH = 2.0f;
	private static final float GRID_LINE_WIDTH = 1.5f;
	private static final float SNAP_LINE_WIDTH = 2.5f;
	private static final float NODE_BOX_WIDTH = 2.0f;
	private static final int GRID_RENDER_PADDING = 1;
	/** Радиус отрисовки узловых блоков сетки вокруг игрока (в ячейках 128×128) */
	private static final int NODE_RENDER_RADIUS = 2;

	private final Setting<Color> borderColor = settings.add(ColorSetting
			.create("border-color")
			.defaultValue(new Color(0, 255, 0, 255))
			.done());

	private final Setting<Color> edgeColor = settings.add(ColorSetting
			.create("edge-color")
			.defaultValue(new Color(255, 170, 0, 255))
			.done());

	private final Setting<Color> snowBiomeColor = settings.add(ColorSetting
			.create("snow-biome-color")
			.defaultValue(new Color(255, 68, 68, 255))
			.done());

	private final Setting<Boolean> showEdgeMarkers = settings.add(BoolSetting
			.create("show-edge-markers")
			.defaultValue(true)
			.done());

	private final Setting<Boolean> warnSnowBiomes = settings.add(BoolSetting
			.create("warn-snow-biomes")
			.defaultValue(true)
			.done());

	private final Setting<Boolean> showMapGrid = settings.add(BoolSetting
			.create("show-map-grid")
			.defaultValue(true)
			.done());

	private final Setting<Color> mapGridColor = settings.add(ColorSetting
			.create("map-grid-color")
			.defaultValue(new Color(100, 180, 255, 180))
			.done());

	/** Состояние каждого размещения, ключ — UUID размещения */
	private final Map<UUID, PlacementState> placementStates = new HashMap<>();

	/** Выбранное размещение для рендера рамки/сетки */
	@Nullable
	private ZoneBounds selectedBounds;

	private int globalSnowCheckTimer;

	@Override
	protected void onDeactivate() {
		placementStates.clear();
		selectedBounds = null;
		globalSnowCheckTimer = 0;
	}

	@Subscribe(TickPreEvent.class)
	private void onTick() {
		if (mc.world == null || mc.player == null) {
			return;
		}

		List<SchematicPlacement> allPlacements = DataManager.getSchematicPlacementManager()
				.getAllSchematicsPlacements();
		SchematicPlacement selected = DataManager.getSchematicPlacementManager()
				.getSelectedSchematicPlacement();

		syncPlacementStates(allPlacements);

		selectedBounds = selected == null ? null : resolveBounds(selected);

		globalSnowCheckTimer--;

		if (globalSnowCheckTimer <= 0) {
			globalSnowCheckTimer = SNOW_CHECK_INTERVAL;
			checkAllSnowBiomes();
		}

		if (selectedBounds != null) {
			PlacementState state = selected == null ? null : placementStates.get(selected.getHashId());

			if (state != null) {
				checkShadowRow(selected.getName(), selectedBounds, state);
				checkSnapAlignment(selected.getName(), selectedBounds, state);
			}
		}
	}

	@Subscribe(RenderEvent.class)
	private void onRender(RenderEvent event) {
		if (mc.player == null) {
			return;
		}

		double playerY = mc.player.getY();

		if (selectedBounds != null) {
			renderBorder(event, selectedBounds);

			if (showEdgeMarkers.get()) {
				renderEdgeMarkers(event, selectedBounds);
			}
		}

		if (showMapGrid.get()) {
			renderMapGrid(event, playerY, selectedBounds);
		}

		for (PlacementState state : placementStates.values()) {
			if (state.snowBiomePositions.isEmpty()) {
				continue;
			}

			renderSnowBiomeMarkers(event, state.snowBiomePositions);
		}
	}

	/**
	 * Синхронизирует карту состояний с текущим списком размещений:
	 * добавляет новые, удаляет снятые.
	 */
	private void syncPlacementStates(List<SchematicPlacement> allPlacements) {
		placementStates.keySet().retainAll(
				allPlacements.stream()
						.map(SchematicPlacement::getHashId)
						.collect(java.util.stream.Collectors.toSet())
		);

		for (SchematicPlacement placement : allPlacements) {
			ZoneBounds bounds = resolveBounds(placement);

			if (bounds == null) {
				continue;
			}

			UUID id = placement.getHashId();
			PlacementState existing = placementStates.get(id);

			if (existing == null) {
				placementStates.put(id, new PlacementState(bounds));
			} else if (!existing.bounds.equals(bounds)) {
				// Схема сдвинулась — сбрасываем состояние
				placementStates.put(id, new PlacementState(bounds));
			}
		}
	}

	/**
	 * Проверяет снежные биомы для всех размещений.
	 * Если чанки зоны только что прогрузились — сбрасывает флаг предупреждения
	 * и повторяет скан, чтобы не пропустить снег из-за незагруженных чанков.
	 */
	private void checkAllSnowBiomes() {
		if (mc.world == null) {
			return;
		}

		List<SchematicPlacement> allPlacements = DataManager.getSchematicPlacementManager()
				.getAllSchematicsPlacements();

		for (SchematicPlacement placement : allPlacements) {
			PlacementState state = placementStates.get(placement.getHashId());

			if (state == null) {
				continue;
			}

			checkSnowBiomesForPlacement(placement.getName(), state);
		}
	}

	private void checkSnowBiomesForPlacement(String name, PlacementState state) {
		if (mc.world == null) {
			return;
		}

		boolean chunksNowLoaded = areZoneChunksLoaded(state.bounds);

		// Чанки только что прогрузились — сбрасываем флаг для повторного скана
		if (chunksNowLoaded && !state.chunksWereLoaded) {
			state.snowWarningShown = false;
		}

		state.chunksWereLoaded = chunksNowLoaded;

		if (!chunksNowLoaded) {
			return;
		}

		state.snowBiomePositions.clear();
		int seaLevel = mc.world.getSeaLevel();
		int checkY = state.bounds.minY;

		for (int x = state.bounds.minX; x <= state.bounds.maxX; x += 4) {
			for (int z = state.bounds.minZ; z <= state.bounds.maxZ; z += 4) {
				BlockPos pos = new BlockPos(x, checkY, z);
				Biome biome = mc.world.getBiome(pos).value();
				Biome.Precipitation precipitation = biome.getPrecipitation(pos, seaLevel);

				if (precipitation == Biome.Precipitation.SNOW) {
					state.snowBiomePositions.add(pos);
				}
			}
		}

		if (state.snowBiomePositions.isEmpty() || !warnSnowBiomes.get()) {
			state.snowWarningShown = false;
			return;
		}

		if (state.snowWarningShown) {
			return;
		}

		state.snowWarningShown = true;
		warning("⚠ [" + name + "] Снежные биомы в зоне схемы ("
				+ state.snowBiomePositions.size() + " точек)! Снег испортит мапарт.");
	}

	/**
	 * Проверяет, загружены ли все чанки зоны схемы.
	 */
	private boolean areZoneChunksLoaded(ZoneBounds b) {
		if (mc.world == null) {
			return false;
		}

		int chunkMinX = b.minX >> 4;
		int chunkMaxX = b.maxX >> 4;
		int chunkMinZ = b.minZ >> 4;
		int chunkMaxZ = b.maxZ >> 4;

		for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
			for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
				if (!mc.world.isChunkLoaded(cx, cz)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Проверяет наличие затеняющего ряда: размер схемы по Z должен быть 128*N + 1.
	 * Без него первый ряд изображения будет отрисован с некорректной яркостью.
	 */
	private void checkShadowRow(String name, ZoneBounds b, PlacementState state) {
		if (state.shadowRowWarningShown) {
			return;
		}

		int sizeZ = b.maxZ - b.minZ;
		boolean hasShadowRow = (sizeZ % MAP_SIZE) == SHADOW_ROW;

		if (hasShadowRow) {
			return;
		}

		state.shadowRowWarningShown = true;
		warning("⚠ [" + name + "] Схема не имеет затеняющего ряда (размер Z = " + sizeZ
				+ "). Первый ряд изображения будет отрисован с некорректной яркостью!");
	}

	/**
	 * Проверяет, выровнен ли угол схемы по границе сетки карт.
	 * Граница карты zoom=0: minX = gridX * 128 - 64, т.е. ..., -192, -64, 64, 192, ...
	 * Реальный угол карты — minZ + SHADOW_ROW (первый ряд схемы затеняющий).
	 * При несовпадении однократно выводит в чат нужное смещение.
	 */
	private void checkSnapAlignment(String name, ZoneBounds b, PlacementState state) {
		int snapX = mapMinX(b.minX);
		int snapZ = mapMinZ(b.minZ + SHADOW_ROW);
		int dx = b.minX - snapX;
		int dz = (b.minZ + SHADOW_ROW) - snapZ;

		boolean aligned = dx == 0 && dz == 0;

		if (aligned) {
			state.snapWarningShown = false;
			return;
		}

		if (state.snapWarningShown) {
			return;
		}

		state.snapWarningShown = true;
		info("[" + name + "] Схема не выровнена по сетке карт. Сдвинь на: X "
				+ (-dx) + ", Z " + (-dz) + "  (целевой угол: " + snapX + ", " + snapZ + ")");
	}

	/**
	 * Рисует прямоугольный каркас по периметру зоны схемы (нижний, верхний и вертикальные рёбра).
	 */
	private void renderBorder(RenderEvent event, ZoneBounds b) {
		int color = borderColor.get().getPacked();

		double x1 = b.minX;
		double y1 = b.minY;
		double z1 = b.minZ;
		double x2 = b.maxX;
		double y2 = b.maxY;
		double z2 = b.maxZ;

		drawLine(event, x1, y1, z1, x2, y1, z1, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y1, z1, x2, y1, z2, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y1, z2, x1, y1, z2, color, BORDER_LINE_WIDTH);
		drawLine(event, x1, y1, z2, x1, y1, z1, color, BORDER_LINE_WIDTH);

		drawLine(event, x1, y2, z1, x2, y2, z1, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y2, z1, x2, y2, z2, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y2, z2, x1, y2, z2, color, BORDER_LINE_WIDTH);
		drawLine(event, x1, y2, z2, x1, y2, z1, color, BORDER_LINE_WIDTH);

		drawLine(event, x1, y1, z1, x1, y2, z1, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y1, z1, x2, y2, z1, color, BORDER_LINE_WIDTH);
		drawLine(event, x2, y1, z2, x2, y2, z2, color, BORDER_LINE_WIDTH);
		drawLine(event, x1, y1, z2, x1, y2, z2, color, BORDER_LINE_WIDTH);
	}

	/**
	 * Рисует засечки по краям зоны через каждые 16 блоков —
	 * визуально обозначает границы секций карты (16×16 чанков).
	 */
	private void renderEdgeMarkers(RenderEvent event, ZoneBounds b) {
		int color = edgeColor.get().getPacked();
		double midY = (b.minY + b.maxY) / 2.0;
		double tickLen = 2.0;

		for (int x = b.minX; x <= b.maxX; x += 16) {
			drawLine(event, x, midY, b.minZ - tickLen, x, midY, b.minZ + tickLen, color, EDGE_LINE_WIDTH);
			drawLine(event, x, midY, b.maxZ - tickLen, x, midY, b.maxZ + tickLen, color, EDGE_LINE_WIDTH);
		}

		for (int z = b.minZ; z <= b.maxZ; z += 16) {
			drawLine(event, b.minX - tickLen, midY, z, b.minX + tickLen, midY, z, color, EDGE_LINE_WIDTH);
			drawLine(event, b.maxX - tickLen, midY, z, b.maxX + tickLen, midY, z, color, EDGE_LINE_WIDTH);
		}
	}

	/**
	 * Рисует сетку карт zoom=0 на высоте игрока (±1 ячейка вокруг зоны схемы или игрока).
	 * Линии проходят через реальные границы карт: ..., -192, -64, 64, 192, 320, ...
	 * В узлах сетки рисуются блоки-маркеры на уровне игрока.
	 * Линии ближайшей выровненной позиции схемы рисуются толще.
	 */
	private void renderMapGrid(RenderEvent event, double playerY, @Nullable ZoneBounds b) {
		int color = mapGridColor.get().getPacked();
		int playerBlockY = MathHelper.floor(playerY);

		int playerX = MathHelper.floor(mc.player.getX());
		int playerZ = MathHelper.floor(mc.player.getZ());

		int snapX = b == null ? mapMinX(playerX) : mapMinX(b.minX);
		int snapZ = b == null ? mapMinZ(playerZ) : mapMinZ(b.minZ + SHADOW_ROW);

		int refMinX = b == null ? playerX : b.minX;
		int refMaxX = b == null ? playerX : b.maxX;
		int refMinZ = b == null ? playerZ : b.minZ;
		int refMaxZ = b == null ? playerZ : b.maxZ;

		int firstLineX = mapMinX(refMinX) - GRID_RENDER_PADDING * MAP_SIZE;
		int lastLineX = mapMinX(refMaxX) + (GRID_RENDER_PADDING + 1) * MAP_SIZE;
		int firstLineZ = mapMinZ(refMinZ) - GRID_RENDER_PADDING * MAP_SIZE;
		int lastLineZ = mapMinZ(refMaxZ) + (GRID_RENDER_PADDING + 1) * MAP_SIZE;

		for (int lineX = firstLineX; lineX <= lastLineX; lineX += MAP_SIZE) {
			float width = (lineX == snapX) ? SNAP_LINE_WIDTH : GRID_LINE_WIDTH;

			drawLine(event, lineX, playerY, firstLineZ, lineX, playerY, lastLineZ, color, width);
		}

		for (int lineZ = firstLineZ; lineZ <= lastLineZ; lineZ += MAP_SIZE) {
			float width = (lineZ == snapZ) ? SNAP_LINE_WIDTH : GRID_LINE_WIDTH;

			drawLine(event, firstLineX, playerY, lineZ, lastLineX, playerY, lineZ, color, width);
		}

		int nodeFirstX = mapMinX(playerX) - NODE_RENDER_RADIUS * MAP_SIZE;
		int nodeLastX = mapMinX(playerX) + (NODE_RENDER_RADIUS + 1) * MAP_SIZE;
		int nodeFirstZ = mapMinZ(playerZ) - NODE_RENDER_RADIUS * MAP_SIZE;
		int nodeLastZ = mapMinZ(playerZ) + (NODE_RENDER_RADIUS + 1) * MAP_SIZE;

		for (int nx = nodeFirstX; nx <= nodeLastX; nx += MAP_SIZE) {
			for (int nz = nodeFirstZ; nz <= nodeLastZ; nz += MAP_SIZE) {
				BlockPos nodePos = new BlockPos(nx, playerBlockY, nz);

				RenderUtils.drawLinesBox(event.getMatrices(), nodePos, color, NODE_BOX_WIDTH, true);
			}
		}

		if (b != null) {
			renderCornerMarker(event, b, playerY);
			renderSnapBox(event, b, snapX, snapZ, playerY);
		}
	}

	/**
	 * Рисует крест-маркер в реальном углу карты на высоте игрока.
	 * Реальный угол = minZ + SHADOW_ROW, т.к. первый ряд схемы — затеняющий.
	 */
	private void renderCornerMarker(RenderEvent event, ZoneBounds b, double playerY) {
		int color = mapGridColor.get().getPacked();
		double cx = b.minX;
		double cz = b.minZ + SHADOW_ROW;
		double arm = 4.0;

		drawLine(event, cx - arm, playerY, cz, cx + arm, playerY, cz, color, SNAP_LINE_WIDTH);
		drawLine(event, cx, playerY, cz - arm, cx, playerY, cz + arm, color, SNAP_LINE_WIDTH);
		drawLine(event, cx, playerY, cz, cx, playerY + arm * 2, cz, color, SNAP_LINE_WIDTH);
	}

	/**
	 * Рисует прямоугольник целевой выровненной позиции схемы на сетке карт.
	 * Показывает, куда нужно сдвинуть схему, чтобы она встала на границу карты.
	 */
	private void renderSnapBox(RenderEvent event, ZoneBounds b, int snapX, int snapZ, double playerY) {
		int dx = b.minX - snapX;
		int dz = (b.minZ + SHADOW_ROW) - snapZ;

		if (dx == 0 && dz == 0) {
			return;
		}

		int color = mapGridColor.get().getPacked();

		double sx1 = snapX;
		double sz1 = snapZ;
		double sx2 = snapX + (b.maxX - b.minX);
		double sz2 = snapZ + (b.maxZ - b.minZ);

		drawLine(event, sx1, playerY, sz1, sx2, playerY, sz1, color, SNAP_LINE_WIDTH);
		drawLine(event, sx2, playerY, sz1, sx2, playerY, sz2, color, SNAP_LINE_WIDTH);
		drawLine(event, sx2, playerY, sz2, sx1, playerY, sz2, color, SNAP_LINE_WIDTH);
		drawLine(event, sx1, playerY, sz2, sx1, playerY, sz1, color, SNAP_LINE_WIDTH);
	}

	private void renderSnowBiomeMarkers(RenderEvent event, List<BlockPos> positions) {
		int color = snowBiomeColor.get().getPacked();

		for (BlockPos pos : positions) {
			double cx = pos.getX() + 2.0;
			double cz = pos.getZ() + 2.0;
			double y1 = pos.getY();
			double y2 = pos.getY() + 8.0;

			drawLine(event, cx, y1, cz, cx, y2, cz, color, SNOW_LINE_WIDTH);
		}
	}

	/**
	 * Читает размещение схемы Litematica и вычисляет AABB зоны
	 * по всем регионам схемы в мировых координатах.
	 */
	@Nullable
	private static ZoneBounds resolveBounds(@Nullable SchematicPlacement placement) {
		if (placement == null) {
			return null;
		}

		LitematicaSchematic schematic = placement.getSchematic();
		BlockPos origin = placement.getOrigin();

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (String regionName : schematic.getAreaPositions().keySet()) {
			BlockPos regionOffset = schematic.getSubRegionPosition(regionName);
			Vec3i size = schematic.getAreaSize(regionName);

			if (size == null) {
				continue;
			}

			int wx = origin.getX() + regionOffset.getX();
			int wy = origin.getY() + regionOffset.getY();
			int wz = origin.getZ() + regionOffset.getZ();

			int rx1 = size.getX() >= 0 ? wx : wx + size.getX();
			int ry1 = size.getY() >= 0 ? wy : wy + size.getY();
			int rz1 = size.getZ() >= 0 ? wz : wz + size.getZ();
			int rx2 = size.getX() >= 0 ? wx + size.getX() : wx;
			int ry2 = size.getY() >= 0 ? wy + size.getY() : wy;
			int rz2 = size.getZ() >= 0 ? wz + size.getZ() : wz;

			minX = Math.min(minX, rx1);
			minY = Math.min(minY, ry1);
			minZ = Math.min(minZ, rz1);
			maxX = Math.max(maxX, rx2);
			maxY = Math.max(maxY, ry2);
			maxZ = Math.max(maxZ, rz2);
		}

		return minX == Integer.MAX_VALUE ? null : new ZoneBounds(minX, minY, minZ, maxX, maxY, maxZ);
	}

	/**
	 * Вычисляет левую границу карты zoom=0, содержащей координату x.
	 * Формула из MapState.of(): centerX = gridX * 128, minX = centerX - 64.
	 * Границы: ..., -192, -64, 64, 192, 320, ...
	 */
	private static int mapMinX(int x) {
		return Math.floorDiv(x + MAP_HALF, MAP_SIZE) * MAP_SIZE - MAP_HALF;
	}

	private static int mapMinZ(int z) {
		return Math.floorDiv(z + MAP_HALF, MAP_SIZE) * MAP_SIZE - MAP_HALF;
	}

	private static void drawLine(
			RenderEvent event,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			int color,
			float width
	) {
		RenderUtils.drawLine(event.getMatrices(), x1, y1, z1, x2, y2, z2, color, width, true);
	}

	private record ZoneBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

	/** Изменяемое состояние одного размещения схемы. */
	private static final class PlacementState {

		final ZoneBounds bounds;
		final List<BlockPos> snowBiomePositions = new ArrayList<>();
		boolean chunksWereLoaded;
		boolean snowWarningShown;
		boolean snapWarningShown;
		boolean shadowRowWarningShown;

		PlacementState(ZoneBounds bounds) {
			this.bounds = bounds;
		}

	}

}
