package org.duollectis.client.modules;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.settings.ItemListSetting;

import java.util.List;

/**
 * Автоматически раскладывает предметы по слотам хотбара согласно настроенным приоритетам.
 * Для каждого слота задаётся список предметов: первый найденный в инвентаре занимает слот.
 * Если нужный предмет уже в хотбаре — он свапается на целевую позицию.
 * Если предмет уже стоит на правильном месте — слот пропускается.
 */
@ModuleInfo("hotbar-sorter")
public class HotbarSorter extends Module {

	private static final int HOTBAR_SIZE = 9;
	private static final int HOTBAR_END = 8;
	private static final int MAIN_INV_START = 9;
	private static final int MAIN_INV_END = 35;

	private final Setting<Integer> swapDelay = settings.add(IntSetting
		.create("swap-delay")
		.defaultValue(1)
		.min(0)
		.sliderRange(0, 20)
		.done()
	);

	private final Setting<List<Item>> slot1 = settings.add(ItemListSetting
		.create("slot-1")
		.group("Slot 1")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot2 = settings.add(ItemListSetting
		.create("slot-2")
		.group("Slot 2")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot3 = settings.add(ItemListSetting
		.create("slot-3")
		.group("Slot 3")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot4 = settings.add(ItemListSetting
		.create("slot-4")
		.group("Slot 4")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot5 = settings.add(ItemListSetting
		.create("slot-5")
		.group("Slot 5")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot6 = settings.add(ItemListSetting
		.create("slot-6")
		.group("Slot 6")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot7 = settings.add(ItemListSetting
		.create("slot-7")
		.group("Slot 7")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot8 = settings.add(ItemListSetting
		.create("slot-8")
		.group("Slot 8")
		.bypassFilter()
		.done()
	);

	private final Setting<List<Item>> slot9 = settings.add(ItemListSetting
		.create("slot-9")
		.group("Slot 9")
		.bypassFilter()
		.done()
	);

	@SuppressWarnings("unchecked")
	private final Setting<List<Item>>[] slotSettings = new Setting[]{
		slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9
	};

	private int timer;

	@Override
	public void onDeactivate() {
		timer = 0;
	}

	@Subscribe(TickPreEvent.class)
	private void onTickPre() {
		if (mc.player == null) {
			return;
		}

		if (timer > 0) {
			timer--;
			return;
		}

		for (int hotbarSlot = 0; hotbarSlot < HOTBAR_SIZE; hotbarSlot++) {
			List<Item> priorities = slotSettings[hotbarSlot].get();

			if (priorities.isEmpty()) {
				continue;
			}

			Item desired = findFirstAvailable(priorities);

			if (desired == null) {
				continue;
			}

			ItemStack currentStack = mc.player.getInventory().getStack(hotbarSlot);

			if (currentStack.getItem() == desired) {
				continue;
			}

			int sourceSlot = findInInventory(desired, hotbarSlot);

			if (sourceSlot == -1) {
				continue;
			}

			swapWithHotbar(sourceSlot, hotbarSlot);
			timer = swapDelay.get();
			return;
		}
	}

	/**
	 * Возвращает первый предмет из списка приоритетов, который есть в инвентаре игрока
	 * (хотбар + основной инвентарь).
	 *
	 * @param priorities упорядоченный список предметов по убыванию приоритета
	 * @return первый найденный предмет или {@code null} если ни одного нет
	 */
	private Item findFirstAvailable(List<Item> priorities) {
		for (Item candidate : priorities) {
			if (existsAnywhere(candidate)) {
				return candidate;
			}
		}

		return null;
	}

	private boolean existsAnywhere(Item item) {
		for (int i = 0; i <= MAIN_INV_END; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == item) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Ищет предмет в инвентаре, исключая целевой хотбар-слот.
	 * Сначала проверяет основной инвентарь (9–35), затем остальные слоты хотбара.
	 * Это минимизирует лишние свапы внутри хотбара.
	 *
	 * @param item        искомый предмет
	 * @param excludeSlot хотбар-слот, который нельзя использовать как источник
	 * @return индекс слота в инвентаре игрока или {@code -1} если не найден
	 */
	private int findInInventory(Item item, int excludeSlot) {
		for (int i = MAIN_INV_START; i <= MAIN_INV_END; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == item) {
				return i;
			}
		}

		for (int i = 0; i <= HOTBAR_END; i++) {
			if (i == excludeSlot) {
				continue;
			}

			if (mc.player.getInventory().getStack(i).getItem() == item) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Выполняет свап между произвольным слотом инвентаря и слотом хотбара через протокол.
	 * Использует {@link SlotActionType#SWAP} с data = hotbarSlot, что эквивалентно
	 * нажатию цифровой клавиши хотбара на слоте в открытом инвентаре.
	 *
	 * @param sourceSlot  индекс слота-источника в инвентаре игрока [0..35]
	 * @param hotbarSlot  целевой слот хотбара [0..8]
	 */
	private void swapWithHotbar(int sourceSlot, int hotbarSlot) {
		int networkSlotId = resolveNetworkSlotId(sourceSlot);

		if (networkSlotId == -1) {
			return;
		}

		mc.interactionManager.clickSlot(
			mc.player.currentScreenHandler.syncId,
			networkSlotId,
			hotbarSlot,
			SlotActionType.SWAP,
			mc.player
		);
	}

	/**
	 * Конвертирует логический индекс слота инвентаря в сетевой ID для PlayerScreenHandler.
	 * Хотбар [0..8] → сетевые ID [36..44], основной инвентарь [9..35] → ID [9..35].
	 *
	 * @param slotIndex логический индекс слота
	 * @return сетевой ID слота или {@code -1} если индекс вне допустимого диапазона
	 */
	private int resolveNetworkSlotId(int slotIndex) {
		if (slotIndex >= 0 && slotIndex <= HOTBAR_END) {
			return 36 + slotIndex;
		}

		if (slotIndex >= MAIN_INV_START && slotIndex <= MAIN_INV_END) {
			return slotIndex;
		}

		return -1;
	}

}
