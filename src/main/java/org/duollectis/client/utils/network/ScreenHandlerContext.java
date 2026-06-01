package org.duollectis.client.utils.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

import static org.duollectis.client.utils.MC.mc;

/**
 * Контекст открытого экрана-обработчика на стороне клиента.
 * Хранит актуальный {@link ScreenHandler}, применяет входящие серверные пакеты
 * и выполняет клики по слотам с корректной сетевой синхронизацией.
 */
@Getter
public class ScreenHandlerContext {

	private ScreenHandler screenHandler;

	/**
	 * Диспетчер входящих пакетов: определяет тип пакета и вызывает соответствующий обработчик.
	 *
	 * @param packet входящий пакет от сервера
	 * @return {@code true} если пакет относится к этому контексту и был обработан
	 */
	public boolean handlePacket(Packet<?> packet) {
		if (packet instanceof OpenScreenS2CPacket openScreen) {
			onOpenScreen(openScreen);
			return true;
		}

		if (packet instanceof InventoryS2CPacket inventory) {
			if (screenHandler == null || screenHandler.syncId != inventory.syncId()) {
				return false;
			}

			onInventory(inventory);
			return true;
		}

		if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate) {
			if (screenHandler == null || screenHandler.syncId != slotUpdate.getSyncId()) {
				return false;
			}

			onSlotUpdate(slotUpdate);
			return true;
		}

		return false;
	}

	public boolean isOpen() {
		return screenHandler != null;
	}

	/**
	 * Обрабатывает пакет открытия экрана от сервера.
	 * Создаёт новый {@link ScreenHandler} через фабрику типа и регистрирует его
	 * как текущий обработчик игрока, чтобы последующие пакеты слотов применялись корректно.
	 *
	 * @param packet пакет открытия экрана с syncId и типом хендлера
	 */
	public void onOpenScreen(OpenScreenS2CPacket packet) {
		screenHandler = packet.getScreenHandlerType().create(
			packet.getSyncId(),
			mc.player.getInventory()
		);
	}

	/**
	 * Применяет полное обновление содержимого инвентаря из пакета сервера.
	 * Игнорирует пакет, если он относится к другому syncId или хендлер ещё не открыт.
	 *
	 * @param packet пакет с полным списком стеков и стеком на курсоре
	 */
	public void onInventory(InventoryS2CPacket packet) {
		if (screenHandler == null) {
			return;
		}

		if (screenHandler.syncId != packet.syncId()) {
			return;
		}

		screenHandler.updateSlotStacks(
			packet.revision(),
			packet.contents(),
			packet.cursorStack()
		);
	}

	/**
	 * Применяет обновление одного слота из пакета сервера.
	 * Игнорирует пакет, если он относится к другому syncId или хендлер ещё не открыт.
	 *
	 * @param packet пакет с индексом слота и новым стеком
	 */
	public void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
		if (screenHandler == null) {
			return;
		}

		if (screenHandler.syncId != packet.getSyncId()) {
			return;
		}

		screenHandler.setStackInSlot(
			packet.getSlot(),
			packet.getRevision(),
			packet.getStack()
		);
	}

	/**
	 * Выполняет клик по слоту и отправляет {@link ClickSlotC2SPacket} с diff изменённых стеков.
	 * Вычисляет хэши только изменившихся слотов для минимизации трафика.
	 *
	 * @param slotId     идентификатор слота
	 * @param button     кнопка мыши (0 = ЛКМ, 1 = ПКМ)
	 * @param actionType тип действия (PICKUP, QUICK_MOVE и т.д.)
	 */
	public void clickSlot(int slotId, int button, SlotActionType actionType) {
		if (mc.player == null || screenHandler == null) {
			return;
		}

		DefaultedList<Slot> slots = screenHandler.slots;
		int slotCount = slots.size();
		List<ItemStack> before = Lists.newArrayListWithCapacity(slotCount);

		for (Slot slot : slots) {
			before.add(slot.getStack().copy());
		}

		screenHandler.onSlotClick(slotId, button, actionType, mc.player);

		Int2ObjectMap<ItemStackHash> changedSlots = new Int2ObjectOpenHashMap<>();
		ComponentChangesHash.ComponentHasher hasher = mc.player
			.networkHandler
			.getComponentHasher();

		for (int i = 0; i < slotCount; i++) {
			ItemStack afterStack = slots.get(i).getStack();

			if (!ItemStack.areEqual(before.get(i), afterStack)) {
				changedSlots.put(i, ItemStackHash.fromItemStack(afterStack, hasher));
			}
		}

		PacketUtil.sendPacket(new ClickSlotC2SPacket(
			screenHandler.syncId,
			screenHandler.getRevision(),
			Shorts.checkedCast(slotId),
			SignedBytes.checkedCast(button),
			actionType,
			changedSlots,
			ItemStackHash.fromItemStack(screenHandler.getCursorStack(), hasher)
		));
	}

	public void close() {
		if (screenHandler != null) {
			PacketUtil.sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
		}

		screenHandler = null;
	}
}
