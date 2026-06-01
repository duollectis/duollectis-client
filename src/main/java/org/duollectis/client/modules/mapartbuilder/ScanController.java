package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.PacketReceiveEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.api.tick.OpenWatchdog;
import org.duollectis.client.utils.network.PacketUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Открывает контейнер по заданной позиции без показа GUI:
 * перехватывает {@link OpenScreenS2CPacket} и {@link InventoryS2CPacket},
 * отменяет их на клиенте, читает содержимое и сразу закрывает экран на сервере.
 * <p>
 * Ватчдог: если сервер не прислал {@link OpenScreenS2CPacket} за {@code openTimeout} тиков,
 * повторяем клик. После {@code MAX_OPEN_ATTEMPTS} неудачных попыток — пропускаем хранилище.
 */
class ScanController extends Controller<MapArtBuilder> {

	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int MAX_OPEN_ATTEMPTS = 3;

	private int pendingSyncId = -1;
	private List<ItemStack> result;
	private boolean done;
	private boolean opened;

	private OpenWatchdog watchdog;

	@Nullable
	private BlockPos openPos;

	@Override
	protected void onDeactivate() {
		debugInfo("[Loot] Деактивация, сброс состояния");
		reset();
	}

	void start(BlockPos pos) {
		reset();
		openPos = pos;
		watchdog = new OpenWatchdog(MAX_OPEN_ATTEMPTS, module.openTimeout.get().intValue());

		debugInfo("[Loot] Открываем контейнер: " + pos);
		mc.interactionManager.interactBlock(
			mc.player,
			Hand.MAIN_HAND,
			new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false)
		);
		opened = true;
	}

	void reset() {
		pendingSyncId = -1;
		result = null;
		done = false;
		opened = false;
		watchdog = null;
		openPos = null;
	}

	boolean isOpened() {
		return opened;
	}

	boolean isDone() {
		return done;
	}

	List<ItemStack> getResult() {
		return result == null ? List.of() : List.copyOf(result);
	}

	@Subscribe
	private void onTick(TickPreEvent event) {
		if (done || !opened || pendingSyncId != -1 || openPos == null) {
			return;
		}

		watchdog.tick(
			() -> {
				debugInfo("[Loot] Ватчдог: шалкер не открылся, попытка "
					+ watchdog.attempts() + "/" + watchdog.maxAttempts() + " для " + openPos);
				mc.interactionManager.interactBlock(
					mc.player,
					Hand.MAIN_HAND,
					new BlockHitResult(openPos.toCenterPos(), Direction.UP, openPos, false)
				);
			},
			() -> {
				debugInfo("[Loot] Ватчдог: превышен лимит попыток ("
					+ MAX_OPEN_ATTEMPTS + ") — пропускаем хранилище " + openPos);
				result = List.of();
				done = true;
				opened = false;
			}
		);
	}

	@Subscribe
	private void onPacket(PacketReceiveEvent event) {
		if (done || !opened) {
			return;
		}

		if (event.getPacket() instanceof OpenScreenS2CPacket packet) {
			pendingSyncId = packet.getSyncId();
			debugInfo("[Loot] Перехвачен OpenScreen, syncId=" + pendingSyncId);
			event.cancel();
			return;
		}

		if (event.getPacket() instanceof InventoryS2CPacket packet) {
			if (pendingSyncId == -1 || packet.syncId() != pendingSyncId) {
				return;
			}

			debugInfo("[Loot] Перехвачен Inventory, слотов=" + packet.contents().size());
			event.cancel();
			captureAndClose(packet);
		}
	}

	/**
	 * Извлекает содержимое контейнера из пакета инвентаря и немедленно закрывает экран на сервере.
	 * Размер контейнера вычисляется как разница между общим числом слотов и {@link #PLAYER_INVENTORY_SIZE},
	 * чтобы отсечь слоты инвентаря игрока, которые сервер всегда добавляет в конец пакета.
	 */
	private void captureAndClose(InventoryS2CPacket packet) {
		List<ItemStack> all = packet.contents();
		int containerSize = all.size() - PLAYER_INVENTORY_SIZE;

		result = containerSize > 0
			? all.subList(0, containerSize).stream()
				.map(ItemStack::copy)
				.toList()
			: List.of();

		debugInfo("[Loot] Захвачено предметов: " + result.size() + " (размер контейнера: " + containerSize + ")");
		PacketUtil.sendPacket(new CloseHandledScreenC2SPacket(pendingSyncId));

		done = true;
		opened = false;
	}
}
