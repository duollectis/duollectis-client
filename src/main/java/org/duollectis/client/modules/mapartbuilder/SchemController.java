package org.duollectis.client.modules.mapartbuilder;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.modules.mapartbuilder.SchematicLayer.BlockEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Контроллер разбора активной схемы Litematica на слои по оси X.
 * <p>
 * При вызове {@link #scan()} читает выбранное размещение схемы и группирует
 * все не-воздушные блоки по мировой X-координате от минус к плюс.
 * Внутри каждого слоя блоки разбиты на колонки по Z (змейка),
 * внутри каждой колонки — отсортированы снизу вверх по Y (индекс 0 = нижний).
 */
class SchemController extends Controller<MapArtBuilder> {

	private final List<SchematicLayer> layers = new ArrayList<>();

	@Override
	public void onDeactivate() {
		layers.clear();
	}

	/**
	 * Сканирует активную схему Litematica и строит список слоёв по X.
	 *
	 * @return количество найденных слоёв, или 0 если схема не загружена
	 */
	int scan() {
		layers.clear();

		SchematicPlacement placement = DataManager.getSchematicPlacementManager()
				.getSelectedSchematicPlacement();

		if (placement == null) {
			debugInfo("[Schem] Схема не выбрана");
			return 0;
		}

		LitematicaSchematic schematic = placement.getSchematic();
		BlockPos origin = placement.getOrigin();

		// worldX → (worldZ → блоки по Y снизу вверх)
		TreeMap<Integer, LinkedHashMap<Integer, List<BlockEntry>>> sorted = new TreeMap<>();

		for (String regionName : schematic.getAreaPositions().keySet()) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

			if (container == null) {
				continue;
			}

			Vec3i size = container.getSize();
			BlockPos regionOffset = schematic.getSubRegionPosition(regionName);

			int worldOriginX = origin.getX() + regionOffset.getX();
			int worldOriginY = origin.getY() + regionOffset.getY();
			int worldOriginZ = origin.getZ() + regionOffset.getZ();

			collectInto(sorted, container, worldOriginX, worldOriginY, worldOriginZ, size);
		}

		int id = 0;

		for (LinkedHashMap<Integer, List<BlockEntry>> columnMap : sorted.values()) {
			List<List<BlockEntry>> columns = new ArrayList<>(columnMap.values());
			layers.add(new SchematicLayer(id++, columns));
		}

		debugInfo("[Schem] Слоёв: " + layers.size());
		return layers.size();
	}

	Optional<SchematicLayer> getLayer(int id) {
		return id >= 0 && id < layers.size()
				? Optional.of(layers.get(id))
				: Optional.empty();
	}

	List<SchematicLayer> getLayers() {
		return List.copyOf(layers);
	}

	boolean isEmpty() {
		return layers.isEmpty();
	}

	int size() {
		return layers.size();
	}

	private void collectInto(
			TreeMap<Integer, LinkedHashMap<Integer, List<BlockEntry>>> target,
			LitematicaBlockStateContainer container,
			int worldOriginX,
			int worldOriginY,
			int worldOriginZ,
			Vec3i size
	) {
		int stepX = size.getX() >= 0 ? 1 : -1;
		int stepY = size.getY() >= 0 ? 1 : -1;
		int stepZ = size.getZ() >= 0 ? 1 : -1;
		int absX = Math.abs(size.getX());
		int absY = Math.abs(size.getY());
		int absZ = Math.abs(size.getZ());

		for (int ix = 0; ix < absX; ix++) {
			int localX = ix * stepX;
			int worldX = worldOriginX + localX;

			LinkedHashMap<Integer, List<BlockEntry>> columnMap = target.computeIfAbsent(
					worldX,
					k -> new LinkedHashMap<>()
			);

			for (int iz = 0; iz < absZ; iz++) {
				int localZ = iz * stepZ;
				int worldZ = worldOriginZ + localZ;

				for (int iy = 0; iy < absY; iy++) {
					int localY = iy * stepY;
					BlockState state = container.get(localX, localY, localZ);

					if (state == null || state.isOf(Blocks.AIR)) {
						continue;
					}

					if (state.getBlock().asItem() == Blocks.AIR.asItem()) {
						continue;
					}

					int worldY = worldOriginY + localY;

					columnMap
							.computeIfAbsent(worldZ, k -> new ArrayList<>())
							.add(new BlockEntry(new BlockPos(worldX, worldY, worldZ), state));
				}

				// Сортируем колонку снизу вверх после заполнения
				List<BlockEntry> column = columnMap.get(worldZ);

				if (column != null) {
					column.sort(Comparator.comparingInt(e -> e.pos().getY()));
				}
			}
		}
	}
}
