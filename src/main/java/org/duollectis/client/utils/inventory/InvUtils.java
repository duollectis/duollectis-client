package org.duollectis.client.utils.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

import static org.duollectis.client.utils.MC.mc;

/** Утилиты для поиска предметов в инвентаре и выполнения действий со слотами. */
public final class InvUtils {

	private static final int OFFHAND_SLOT_ID = 45;

	private static final Action ACTION = new Action();

	private static int previousSlot = -1;

	private InvUtils() {}

	public static boolean testInMainHand(Predicate<ItemStack> predicate) {
		return predicate.test(mc.player.getMainHandStack());
	}

	public static boolean testInOffHand(Predicate<ItemStack> predicate) {
		return predicate.test(mc.player.getOffHandStack());
	}

	public static FindItemResult findInHotbar(Item... items) {
		return findInHotbar(stack -> {
			for (Item item : items) {
				if (stack.getItem() == item) {
					return true;
				}
			}

			return false;
		});
	}

	/**
	 * Ищет предмет в хотбаре и оффхенде. Приоритет: оффхенд → текущий слот → остальной хотбар.
	 *
	 * @param isGood предикат для проверки стека
	 * @return результат поиска с индексом слота и суммарным количеством
	 */
	public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
		if (testInOffHand(isGood)) {
			return new FindItemResult(OFFHAND_SLOT_ID, mc.player.getOffHandStack().getCount());
		}

		if (testInMainHand(isGood)) {
			return new FindItemResult(
				mc.player.getInventory().getSelectedSlot(),
				mc.player.getMainHandStack().getCount()
			);
		}

		return find(isGood, 0, 8);
	}

	public static FindItemResult find(Predicate<ItemStack> isGood) {
		if (mc.player == null) {
			return new FindItemResult(0, 0);
		}

		return find(isGood, 0, mc.player.getInventory().size());
	}

	/**
	 * Ищет предмет в диапазоне слотов инвентаря, суммируя количество во всех найденных стеках.
	 *
	 * @param isGood предикат для проверки стека
	 * @param start  начальный индекс слота (включительно)
	 * @param end    конечный индекс слота (включительно)
	 * @return результат с индексом первого найденного слота и суммарным количеством
	 */
	public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
		if (mc.player == null) {
			return new FindItemResult(0, 0);
		}

		int foundSlot = -1;
		int totalCount = 0;

		for (int i = start; i <= end; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);

			if (!isGood.test(stack)) {
				continue;
			}

			if (foundSlot == -1) {
				foundSlot = i;
			}

			totalCount += stack.getCount();
		}

		return new FindItemResult(foundSlot, totalCount);
	}

	/**
	 * Переключает активный слот хотбара. При {@code swapBack=true} запоминает текущий слот
	 * для последующего восстановления через {@link #swapBack()}.
	 *
	 * @param slot     целевой слот хотбара [0..8]
	 * @param swapBack запомнить ли текущий слот для возврата
	 * @return {@code true} если переключение выполнено
	 */
	public static boolean swap(int slot, boolean swapBack) {
		if (slot == OFFHAND_SLOT_ID) {
			return true;
		}

		if (slot < 0 || slot > 8) {
			return false;
		}

		if (swapBack) {
			if (previousSlot == -1) {
				previousSlot = mc.player.getInventory().getSelectedSlot();
			}
		} else {
			previousSlot = -1;
		}

		mc.player.getInventory().setSelectedSlot(slot);
		mc.interactionManager.syncSelectedSlot();
		return true;
	}

	public static boolean swapBack() {
		if (previousSlot == -1) {
			return false;
		}

		boolean result = swap(previousSlot, false);
		previousSlot = -1;
		return result;
	}

	public static Action move() {
		ACTION.type = SlotActionType.PICKUP;
		ACTION.two = true;
		return ACTION;
	}

	public static Action click() {
		ACTION.type = SlotActionType.PICKUP;
		return ACTION;
	}

	/**
	 * Меняет предмет в указанном слоте инвентаря с хотбар-слотом через {@link SlotActionType#SWAP}.
	 * Эквивалентно нажатию клавиши F (или цифры хотбара) на слоте — без захвата курсором.
	 * <p>
	 * Использование: {@code InvUtils.swap().from(inventorySlot).toHotbar(hotbarSlot)}
	 */
	public static Action swap() {
		ACTION.type = SlotActionType.SWAP;
		return ACTION;
	}

	public static class Action {

		private SlotActionType type = null;
		private boolean two = false;
		private int from = -1;
		private int to = -1;
		private int data = 0;
		private boolean isRecursive = false;

		private Action() {}

		public Action fromId(int id) {
			this.from = id;
			return this;
		}

		public Action from(int index) {
			return fromId(SlotUtils.indexToId(index));
		}

		public void toId(int id) {
			this.to = id;
			run();
		}

		public void to(int index) {
			toId(SlotUtils.indexToId(index));
		}

		public void toHotbar(int i) {
			to(i);
		}

		public void slotId(int id) {
			this.from = this.to = id;
			run();
		}

		private void run() {
			boolean hadEmptyCursor = mc.player
				.currentScreenHandler
				.getCursorStack()
				.isEmpty();

			if (this.type == SlotActionType.SWAP) {
				this.data = this.from;
				this.from = this.to;
			}

			if (this.type != null && this.from != -1 && this.to != -1) {
				click(this.from);

				if (this.two) {
					click(this.to);
				}
			}

			SlotActionType preType = this.type;
			boolean preTwo = this.two;
			int preFrom = this.from;
			int preTo = this.to;

			this.type = null;
			this.two = false;
			this.from = -1;
			this.to = -1;
			this.data = 0;

			boolean cursorNotEmpty = !mc.player
				.currentScreenHandler
				.getCursorStack()
				.isEmpty();

			boolean shouldFixCursor = !this.isRecursive
				&& hadEmptyCursor
				&& preType == SlotActionType.PICKUP
				&& preTwo
				&& preFrom != -1
				&& preTo != -1
				&& cursorNotEmpty;

			if (shouldFixCursor) {
				this.isRecursive = true;
				InvUtils.click().slotId(preFrom);
				this.isRecursive = false;
			}
		}

		private void click(int id) {
			mc.interactionManager.clickSlot(
				mc.player.currentScreenHandler.syncId,
				id,
				this.data,
				this.type,
				mc.player
			);
		}
	}
}
