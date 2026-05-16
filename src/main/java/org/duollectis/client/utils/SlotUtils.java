//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.duollectis.client.utils;

import net.minecraft.client.MinecraftClient;
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

public class SlotUtils {

	private SlotUtils() {
	}

	public static int indexToId(int i) {
		if (MinecraftClient.getInstance().player == null) {
			return -1;
		}
		else {
			ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
			if (handler instanceof PlayerScreenHandler) {
				return survivalInventory(i);
			}
			else if (handler instanceof CreativeInventoryScreen.CreativeScreenHandler) {
				return creativeInventory(i);
			}
			else if (handler instanceof GenericContainerScreenHandler) {
				GenericContainerScreenHandler genericContainerScreenHandler = (GenericContainerScreenHandler) handler;
				return genericContainer(i, genericContainerScreenHandler.getRows());
			}
			else if (handler instanceof CraftingScreenHandler) {
				return craftingTable(i);
			}
			else if (handler instanceof FurnaceScreenHandler) {
				return furnace(i);
			}
			else if (handler instanceof BlastFurnaceScreenHandler) {
				return furnace(i);
			}
			else if (handler instanceof SmokerScreenHandler) {
				return furnace(i);
			}
			else if (handler instanceof Generic3x3ContainerScreenHandler) {
				return generic3x3(i);
			}
			else if (handler instanceof EnchantmentScreenHandler) {
				return enchantmentTable(i);
			}
			else if (handler instanceof BrewingStandScreenHandler) {
				return brewingStand(i);
			}
			else if (handler instanceof MerchantScreenHandler) {
				return villager(i);
			}
			else if (handler instanceof BeaconScreenHandler) {
				return beacon(i);
			}
			else if (handler instanceof AnvilScreenHandler) {
				return anvil(i);
			}
			else if (handler instanceof HopperScreenHandler) {
				return hopper(i);
			}
			else if (handler instanceof ShulkerBoxScreenHandler) {
				return genericContainer(i, 3);
			}
			else if (handler instanceof HorseScreenHandler horseScreenHandler) {
				return horse(horseScreenHandler, i);
			}
			else if (handler instanceof CartographyTableScreenHandler) {
				return cartographyTable(i);
			}
			else if (handler instanceof GrindstoneScreenHandler) {
				return grindstone(i);
			}
			else if (handler instanceof LecternScreenHandler) {
				return lectern();
			}
			else if (handler instanceof LoomScreenHandler) {
				return loom(i);
			}
			else {
				return handler instanceof StonecutterScreenHandler ? stonecutter(i) : -1;
			}
		}
	}

	private static int survivalInventory(int i) {
		if (isHotbar(i)) {
			return 36 + i;
		}
		else {
			return isArmor(i) ? 5 + (i - 36) : i;
		}
	}

	private static int creativeInventory(int i) {
		return MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen
				       && CreativeInventoryScreen.selectedTab == Registries.ITEM_GROUP.get(ItemGroups.INVENTORY)
		       ? survivalInventory(i)
		       : -1;
	}

	private static int genericContainer(int i, int rows) {
		if (isHotbar(i)) {
			return (rows + 3) * 9 + i;
		}
		else {
			return isMain(i) ? rows * 9 + (i - 9) : -1;
		}
	}

	private static int craftingTable(int i) {
		if (isHotbar(i)) {
			return 37 + i;
		}
		else {
			return isMain(i) ? i + 1 : -1;
		}
	}

	private static int furnace(int i) {
		if (isHotbar(i)) {
			return 30 + i;
		}
		else {
			return isMain(i) ? 3 + (i - 9) : -1;
		}
	}

	private static int generic3x3(int i) {
		if (isHotbar(i)) {
			return 36 + i;
		}
		else {
			return isMain(i) ? i : -1;
		}
	}

	private static int enchantmentTable(int i) {
		if (isHotbar(i)) {
			return 29 + i;
		}
		else {
			return isMain(i) ? 2 + (i - 9) : -1;
		}
	}

	private static int brewingStand(int i) {
		if (isHotbar(i)) {
			return 32 + i;
		}
		else {
			return isMain(i) ? 5 + (i - 9) : -1;
		}
	}

	private static int villager(int i) {
		if (isHotbar(i)) {
			return 30 + i;
		}
		else {
			return isMain(i) ? 3 + (i - 9) : -1;
		}
	}

	private static int beacon(int i) {
		if (isHotbar(i)) {
			return 28 + i;
		}
		else {
			return isMain(i) ? 1 + (i - 9) : -1;
		}
	}

	private static int anvil(int i) {
		if (isHotbar(i)) {
			return 30 + i;
		}
		else {
			return isMain(i) ? 3 + (i - 9) : -1;
		}
	}

	private static int hopper(int i) {
		if (isHotbar(i)) {
			return 32 + i;
		}
		else {
			return isMain(i) ? 5 + (i - 9) : -1;
		}
	}

	private static int horse(HorseScreenHandler handler, int i) {
		LivingEntity entity = handler.mount;

		if (entity instanceof LlamaEntity llamaEntity) {
			int strength = llamaEntity.getStrength();
			if (isHotbar(i)) {
				return 2 + 3 * strength + 28 + i;
			}

			if (isMain(i)) {
				return 2 + 3 * strength + 1 + (i - 9);
			}
		}
		else if (!(entity instanceof HorseEntity) && !(entity instanceof SkeletonHorseEntity)
				&& !(entity instanceof ZombieHorseEntity)) {
			if (entity instanceof AbstractDonkeyEntity) {
				AbstractDonkeyEntity abstractDonkeyEntity = (AbstractDonkeyEntity) entity;
				boolean chest = abstractDonkeyEntity.hasChest();
				if (isHotbar(i)) {
					return (chest ? 44 : 29) + i;
				}

				if (isMain(i)) {
					return (chest ? 17 : 2) + (i - 9);
				}
			}
		}
		else {
			if (isHotbar(i)) {
				return 29 + i;
			}

			if (isMain(i)) {
				return 2 + (i - 9);
			}
		}

		return -1;
	}

	private static int cartographyTable(int i) {
		if (isHotbar(i)) {
			return 30 + i;
		}
		else {
			return isMain(i) ? 3 + (i - 9) : -1;
		}
	}

	private static int grindstone(int i) {
		if (isHotbar(i)) {
			return 30 + i;
		}
		else {
			return isMain(i) ? 3 + (i - 9) : -1;
		}
	}

	private static int lectern() {
		return -1;
	}

	private static int loom(int i) {
		if (isHotbar(i)) {
			return 31 + i;
		}
		else {
			return isMain(i) ? 4 + (i - 9) : -1;
		}
	}

	private static int stonecutter(int i) {
		if (isHotbar(i)) {
			return 29 + i;
		}
		else {
			return isMain(i) ? 2 + (i - 9) : -1;
		}
	}

	public static boolean isHotbar(int i) {
		return i >= 0 && i <= 8;
	}

	public static boolean isMain(int i) {
		return i >= 9 && i <= 35;
	}

	public static boolean isArmor(int i) {
		return i >= 36 && i <= 39;
	}
}
