package org.duollectis.client.utils.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

import static org.duollectis.client.utils.MC.mc;

/** Расширенные утилиты инвентаря: подсчёт предметов, работа с шалкерами, проверка заполненности. */
public final class InvUtility {

	public static final int GENERIC_INV_SIZE = 4 * 9;

	private InvUtility() {}

	public static boolean isInventoryEmpty() {
		return isInventoryEmpty(null);
	}

	public static boolean isInventoryEmpty(List<Item> ignoredItems) {
		DefaultedList<Slot> slots = mc.player.playerScreenHandler.slots;

		for (Slot slot : slots) {
			ItemStack stack = slot.getStack();

			if (stack.isEmpty()) {
				continue;
			}

			if (ignoredItems == null || !ignoredItems.contains(stack.getItem())) {
				return false;
			}
		}

		return true;
	}

	public static int countFullSlots() {
		int count = 0;

		for (int i = 0; i < GENERIC_INV_SIZE; i++) {
			if (!mc.player.getInventory().getStack(i).isEmpty()) {
				count++;
			}
		}

		return count;
	}

	public static List<ItemStack> getShulkerBySlot(Inventory inv, int slot) {
		return getShulkerSlots(inv.getStack(slot));
	}

	/**
	 * Извлекает содержимое шалкер-бокса из компонента {@link DataComponentTypes#CONTAINER}.
	 *
	 * @param stack стек предмета (должен быть шалкер-боксом)
	 * @return список из 27 стеков содержимого, или {@code null} если стек не является шалкером
	 */
	public static List<ItemStack> getShulkerSlots(ItemStack stack) {
		if (stack == null) {
			return null;
		}

		if (!(Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock)) {
			return null;
		}

		ContainerComponent component = stack.getComponents().get(DataComponentTypes.CONTAINER);

		if (component == null) {
			return null;
		}

		DefaultedList<ItemStack> shulker = DefaultedList.ofSize(27, ItemStack.EMPTY);

		for (int i = 0; i < component.stacks.size(); i++) {
			shulker.set(i, component.stacks.get(i));
		}

		return shulker;
	}

	public static List<Shulker> getAllShulkers() {
		List<Shulker> shulkers = new ArrayList<>();
		Inventory inv = mc.player.getInventory();

		for (int i = 0; i < GENERIC_INV_SIZE; i++) {
			List<ItemStack> shulkerStacks = getShulkerBySlot(inv, i);

			if (shulkerStacks != null) {
				shulkers.add(new Shulker(shulkerStacks));
			}
		}

		return shulkers;
	}

	public static int countItem(Item item) {
		return countItem(item, true, false);
	}

	public static int countShulkers() {
		PlayerInventory inventory = mc.player.getInventory();
		int counter = 0;

		for (int i = 0; i < GENERIC_INV_SIZE; i++) {
			ItemStack stack = inventory.getStack(i);

			if (stack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock) {
				counter += stack.getCount();
			}
		}

		return counter;
	}

	public static int countItem(Item item, boolean checkOffHand, boolean checkShulkers) {
		return countItem(item, true, checkOffHand, checkShulkers);
	}

	/**
	 * Подсчитывает суммарное количество предмета с гибким выбором источников поиска.
	 *
	 * @param checkInventory  искать в основном инвентаре
	 * @param checkOffHand    искать в оффхенде
	 * @param checkShulkers   искать внутри шалкер-боксов в инвентаре
	 * @return суммарное количество найденных единиц предмета
	 */
	public static int countItem(Item item, boolean checkInventory, boolean checkOffHand, boolean checkShulkers) {
		int counter = 0;

		if (checkOffHand) {
			ItemStack offHandStack = mc.player.getOffHandStack();

			if (offHandStack.getItem() == item) {
				counter += offHandStack.getCount();
			}
		}

		if (checkInventory) {
			PlayerInventory inventory = mc.player.getInventory();

			for (int i = 0; i < GENERIC_INV_SIZE; i++) {
				ItemStack stack = inventory.getStack(i);

				if (stack.getItem() == item) {
					counter += stack.getCount();
				}
			}
		}

		if (checkShulkers) {
			for (Shulker shulker : getAllShulkers()) {
				for (ItemStack stack : shulker.stacks) {
					if (stack.getItem() == item) {
						counter += stack.getCount();
					}
				}
			}
		}

		return counter;
	}

	public static boolean itemContains(Item item, boolean checkOffHand) {
		return itemContains(item, checkOffHand, false);
	}

	public static boolean itemContains(Item item, boolean checkOffHand, boolean checkShulkers) {
		if (checkOffHand && mc.player.getOffHandStack().getItem() == item) {
			return true;
		}

		PlayerInventory inventory = mc.player.getInventory();

		for (int i = 0; i < GENERIC_INV_SIZE; i++) {
			if (inventory.getStack(i).getItem() == item) {
				return true;
			}
		}

		if (checkShulkers) {
			for (Shulker shulker : getAllShulkers()) {
				for (ItemStack stack : shulker.stacks) {
					if (stack.getItem() == item) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static class Shulker {

		public final List<ItemStack> stacks;

		public Shulker(List<ItemStack> stacks) {
			this.stacks = stacks;
		}
	}
}
