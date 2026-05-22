package org.duollectis.client.utils;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;

import static org.duollectis.client.utils.MC.mc;

/**
 * Утилита для преобразования логических индексов слотов инвентаря игрока
 * в сетевые идентификаторы слотов, зависящие от типа открытого {@link net.minecraft.screen.ScreenHandler}.
 * Каждый тип контейнера имеет собственную нумерацию слотов в протоколе Minecraft.
 */
public final class SlotUtils {

	private static final int HOTBAR_START = 0;
	private static final int HOTBAR_END = 8;
	private static final int MAIN_START = 9;
	private static final int MAIN_END = 35;
	private static final int ARMOR_START = 36;
	private static final int ARMOR_END = 39;
	private static final int INVALID_SLOT = -1;

	private static final int SURVIVAL_ARMOR_SLOT_OFFSET = 5;

	private static final int CRAFTING_HOTBAR_BASE = 37;
	private static final int CRAFTING_MAIN_OFFSET = 1;

	private static final int FURNACE_HOTBAR_BASE = 30;
	private static final int FURNACE_MAIN_BASE = 3;

	private static final int ENCHANTMENT_HOTBAR_BASE = 29;
	private static final int ENCHANTMENT_MAIN_BASE = 2;

	private static final int BREWING_HOTBAR_BASE = 32;
	private static final int BREWING_MAIN_BASE = 5;

	private static final int MERCHANT_HOTBAR_BASE = 30;
	private static final int MERCHANT_MAIN_BASE = 3;

	private static final int BEACON_HOTBAR_BASE = 28;
	private static final int BEACON_MAIN_BASE = 1;

	private static final int ANVIL_HOTBAR_BASE = 30;
	private static final int ANVIL_MAIN_BASE = 3;

	private static final int HOPPER_HOTBAR_BASE = 32;
	private static final int HOPPER_MAIN_BASE = 5;

	private static final int SHULKER_BOX_ROWS = 3;
	private static final int PLAYER_INVENTORY_ROWS = 3;

	private static final int CARTOGRAPHY_HOTBAR_BASE = 30;
	private static final int CARTOGRAPHY_MAIN_BASE = 3;

	private static final int GRINDSTONE_HOTBAR_BASE = 30;
	private static final int GRINDSTONE_MAIN_BASE = 3;

	private static final int LOOM_HOTBAR_BASE = 31;
	private static final int LOOM_MAIN_BASE = 4;

	private static final int STONECUTTER_HOTBAR_BASE = 29;
	private static final int STONECUTTER_MAIN_BASE = 2;

	private static final int HORSE_HOTBAR_BASE = 29;
	private static final int HORSE_MAIN_BASE = 2;

	private static final int LLAMA_SLOT_OFFSET = 2;
	private static final int LLAMA_COLUMNS = 3;
	private static final int LLAMA_HOTBAR_EXTRA = 28;
	private static final int LLAMA_MAIN_EXTRA = 1;

	private static final int DONKEY_WITH_CHEST_HOTBAR_BASE = 44;
	private static final int DONKEY_WITH_CHEST_MAIN_BASE = 17;
	private static final int DONKEY_NO_CHEST_HOTBAR_BASE = 29;
	private static final int DONKEY_NO_CHEST_MAIN_BASE = 2;

	private SlotUtils() {}

	public static int indexToId(int slotIndex) {
		var player = mc.player;

		if (player == null) {
			return INVALID_SLOT;
		}

		ScreenHandler handler = player.currentScreenHandler;

		if (handler instanceof PlayerScreenHandler) {
			return survivalInventory(slotIndex);
		}

		if (handler instanceof CreativeInventoryScreen.CreativeScreenHandler) {
			return creativeInventory(slotIndex);
		}

		if (handler instanceof GenericContainerScreenHandler genericHandler) {
			return genericContainer(slotIndex, genericHandler.getRows());
		}

		if (handler instanceof CraftingScreenHandler) {
			return craftingTable(slotIndex);
		}

		if (handler instanceof FurnaceScreenHandler
			|| handler instanceof BlastFurnaceScreenHandler
			|| handler instanceof SmokerScreenHandler
		) {
			return resolveSlot(slotIndex, FURNACE_HOTBAR_BASE, FURNACE_MAIN_BASE);
		}

		if (handler instanceof Generic3x3ContainerScreenHandler) {
			return generic3x3(slotIndex);
		}

		if (handler instanceof EnchantmentScreenHandler) {
			return resolveSlot(slotIndex, ENCHANTMENT_HOTBAR_BASE, ENCHANTMENT_MAIN_BASE);
		}

		if (handler instanceof BrewingStandScreenHandler) {
			return resolveSlot(slotIndex, BREWING_HOTBAR_BASE, BREWING_MAIN_BASE);
		}

		if (handler instanceof MerchantScreenHandler) {
			return resolveSlot(slotIndex, MERCHANT_HOTBAR_BASE, MERCHANT_MAIN_BASE);
		}

		if (handler instanceof BeaconScreenHandler) {
			return resolveSlot(slotIndex, BEACON_HOTBAR_BASE, BEACON_MAIN_BASE);
		}

		if (handler instanceof AnvilScreenHandler) {
			return resolveSlot(slotIndex, ANVIL_HOTBAR_BASE, ANVIL_MAIN_BASE);
		}

		if (handler instanceof HopperScreenHandler) {
			return resolveSlot(slotIndex, HOPPER_HOTBAR_BASE, HOPPER_MAIN_BASE);
		}

		if (handler instanceof ShulkerBoxScreenHandler) {
			return genericContainer(slotIndex, SHULKER_BOX_ROWS);
		}

		if (handler instanceof HorseScreenHandler horseHandler) {
			return horse(horseHandler, slotIndex);
		}

		if (handler instanceof CartographyTableScreenHandler) {
			return resolveSlot(slotIndex, CARTOGRAPHY_HOTBAR_BASE, CARTOGRAPHY_MAIN_BASE);
		}

		if (handler instanceof GrindstoneScreenHandler) {
			return resolveSlot(slotIndex, GRINDSTONE_HOTBAR_BASE, GRINDSTONE_MAIN_BASE);
		}

		if (handler instanceof LecternScreenHandler) {
			return INVALID_SLOT;
		}

		if (handler instanceof LoomScreenHandler) {
			return resolveSlot(slotIndex, LOOM_HOTBAR_BASE, LOOM_MAIN_BASE);
		}

		if (handler instanceof StonecutterScreenHandler) {
			return resolveSlot(slotIndex, STONECUTTER_HOTBAR_BASE, STONECUTTER_MAIN_BASE);
		}

		return INVALID_SLOT;
	}

	public static boolean isHotbar(int slotIndex) {
		return slotIndex >= HOTBAR_START && slotIndex <= HOTBAR_END;
	}

	public static boolean isMain(int slotIndex) {
		return slotIndex >= MAIN_START && slotIndex <= MAIN_END;
	}

	public static boolean isArmor(int slotIndex) {
		return slotIndex >= ARMOR_START && slotIndex <= ARMOR_END;
	}

	private static int survivalInventory(int slotIndex) {
		if (isHotbar(slotIndex)) {
			return ARMOR_START + slotIndex;
		}

		return isArmor(slotIndex) ? SURVIVAL_ARMOR_SLOT_OFFSET + (slotIndex - ARMOR_START) : slotIndex;
	}

	private static int creativeInventory(int slotIndex) {
		var screen = mc.currentScreen;
		boolean isInventoryTab = screen instanceof CreativeInventoryScreen
			&& CreativeInventoryScreen.selectedTab == Registries.ITEM_GROUP.get(ItemGroups.INVENTORY);

		return isInventoryTab ? survivalInventory(slotIndex) : INVALID_SLOT;
	}

	private static int genericContainer(int slotIndex, int rows) {
		if (isHotbar(slotIndex)) {
			return (rows + PLAYER_INVENTORY_ROWS) * MAIN_START + slotIndex;
		}

		return isMain(slotIndex) ? rows * MAIN_START + (slotIndex - MAIN_START) : INVALID_SLOT;
	}

	private static int craftingTable(int slotIndex) {
		if (isHotbar(slotIndex)) {
			return CRAFTING_HOTBAR_BASE + slotIndex;
		}

		return isMain(slotIndex) ? slotIndex + CRAFTING_MAIN_OFFSET : INVALID_SLOT;
	}

	private static int generic3x3(int slotIndex) {
		if (isHotbar(slotIndex)) {
			return ARMOR_START + slotIndex;
		}

		return isMain(slotIndex) ? slotIndex : INVALID_SLOT;
	}

	private static int horse(HorseScreenHandler handler, int slotIndex) {
		LivingEntity entity = handler.mount;

		if (entity instanceof LlamaEntity llama) {
			return llamaSlot(llama, slotIndex);
		}

		if (entity instanceof AbstractDonkeyEntity donkey) {
			return donkeySlot(donkey, slotIndex);
		}

		if (entity instanceof HorseEntity
			|| entity instanceof SkeletonHorseEntity
			|| entity instanceof ZombieHorseEntity
		) {
			return resolveSlot(slotIndex, HORSE_HOTBAR_BASE, HORSE_MAIN_BASE);
		}

		return INVALID_SLOT;
	}

	private static int llamaSlot(LlamaEntity llama, int slotIndex) {
		int strength = llama.getStrength();
		int hotbarBase = LLAMA_SLOT_OFFSET + LLAMA_COLUMNS * strength + LLAMA_HOTBAR_EXTRA;
		int mainBase = LLAMA_SLOT_OFFSET + LLAMA_COLUMNS * strength + LLAMA_MAIN_EXTRA;

		if (isHotbar(slotIndex)) {
			return hotbarBase + slotIndex;
		}

		return isMain(slotIndex) ? mainBase + (slotIndex - MAIN_START) : INVALID_SLOT;
	}

	private static int donkeySlot(AbstractDonkeyEntity donkey, int slotIndex) {
		boolean hasChest = donkey.hasChest();
		int hotbarBase = hasChest ? DONKEY_WITH_CHEST_HOTBAR_BASE : DONKEY_NO_CHEST_HOTBAR_BASE;
		int mainBase = hasChest ? DONKEY_WITH_CHEST_MAIN_BASE : DONKEY_NO_CHEST_MAIN_BASE;

		if (isHotbar(slotIndex)) {
			return hotbarBase + slotIndex;
		}

		return isMain(slotIndex) ? mainBase + (slotIndex - MAIN_START) : INVALID_SLOT;
	}

	private static int resolveSlot(int slotIndex, int hotbarBase, int mainBase) {
		if (isHotbar(slotIndex)) {
			return hotbarBase + slotIndex;
		}

		return isMain(slotIndex) ? mainBase + (slotIndex - MAIN_START) : INVALID_SLOT;
	}
}
