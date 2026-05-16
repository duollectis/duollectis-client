package org.duollectis.client.utils;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

public class InvUtility {

    public static final int GENERIC_INV_SIZE = 4 * 9;

    public static boolean isInventoryEmpty() {
        return isInventoryEmpty(null);
    }

    public static boolean isInventoryEmpty(List<Item> ignoredItems) {
        assert MinecraftClient.getInstance().player != null;

        DefaultedList<Slot> slots = MinecraftClient.getInstance().player.playerScreenHandler.slots;

        for (Slot slot : slots) {
            ItemStack itemStack = slot.getStack();

            if (itemStack == null)
                continue;

            if (itemStack.isEmpty())
                continue;

            if (ignoredItems == null || !ignoredItems.contains(itemStack.getItem()))
                return false;
        }
        return true;
    }

    // public static void dropItems(int amount, boolean whitelist, boolean
    // fullStack) {
    // dropItems(null, amount, whitelist, fullStack);
    // }

    public static void dropItems(List<Item> items, int amount, boolean whitelist, boolean fullStack) {
        assert MinecraftClient.getInstance().player != null;

        DefaultedList<Slot> slots = MinecraftClient.getInstance().player.playerScreenHandler.slots;

        int dropped = 0;
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack itemStack = slots.get(slot).getStack();

            if (itemStack == null || itemStack.isEmpty())
                continue;

            // Drop all if null
            if (items != null && ((!whitelist && items.contains(itemStack.getItem())) ||
                    (whitelist && !items.contains(itemStack.getItem()))
            ))
                continue;

            if (fullStack) {
                if (dropped >= amount)
                    return;

                clickSlot(slot, 1, SlotActionType.THROW);
                dropped++;

                continue;
            }

            for (int i = 0; i < amount; i++) {
                if (dropped >= amount)
                    return;

                clickSlot(slot, 0, SlotActionType.THROW);
                dropped++;
            }
        }
    }

    // public static boolean isInventoryFull() {
    // assert MinecraftClient.getInstance().player != null;
    //
    // for (int i = 0; i < GENERIC_INV_SIZE; i++) {
    // ItemStack itemStack = MinecraftClient.getInstance().player.getInventory().getStack(i);
    // if (itemStack == null)
    // return false;
    // if (itemStack.isEmpty())
    // return false;
    // }
    //
    // return true;
    // }

    // public static int findEmptySlot() {
    // assert MinecraftClient.getInstance().player != null;
    // Inventory inv = MinecraftClient.getInstance().player.getInventory();
    //
    // for (int i = 8; i >= 0; i--) {
    // ItemStack stack = inv.getStack(i);
    // if (stack == null || stack.isEmpty())
    // return 8 - i;
    // }
    //
    // for (int i = 35; i >= 9; i--) {
    // ItemStack stack = inv.getStack(i);
    // if (stack == null || stack.isEmpty())
    // return 35 + 9 - i;
    // }
    //
    // return -1;
    // }

    public static int countFullSlots() {
        assert MinecraftClient.getInstance().player != null;

        int slots = 0;
        for (int i = 0; i < GENERIC_INV_SIZE; i++) {
            if (!MinecraftClient.getInstance().player.getInventory().getStack(i).isEmpty())
                slots++;
        }
        return slots;
    }

    // public static int countEmptySlots() {
    // assert MinecraftClient.getInstance().player != null;
    //
    // int slots = 0;
    // for (int i = 0; i < GENERIC_INV_SIZE; i++) {
    // if (MinecraftClient.getInstance().player.getInventory().getStack(i).isEmpty())
    // slots++;
    // }
    // return slots;
    // }

    // public static int countShulkers() {
    // int counter = 0;
    // Inventory inv = MinecraftClient.getInstance().player.getInventory();
    // for (int i = 0; i < GENERIC_INV_SIZE; i++) {
    // ItemStack stack = inv.getStack(i);
    // if (stack == null || !(Block.getBlockFromItem(stack.getItem()) instanceof
    // ShulkerBoxBlock))
    // continue;
    // counter++;
    // }
    // return counter;
    // }

    // public static int countItemShulker(DefaultedList<ItemStack> shulker, Item
    // item) {
    // int count = 0;
    // for (ItemStack shulkerStack : shulker)
    // if
    // (shulkerStack.getItem().getTranslationKey().equals(item.getTranslationKey()))
    // count += shulkerStack.getCount();
    // return count;
    // }

    public static List<ItemStack> getShulkerBySlot(Inventory inv, int slot) {
        ItemStack stack = inv.getStack(slot);
        return getShulkerSlots(stack);
    }

    public static List<ItemStack> getShulkerSlots(ItemStack stack) {
        if (stack == null || !(Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock)) {
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

        Inventory inv = MinecraftClient.getInstance().player.getInventory();
        for (int i = 0; i < GENERIC_INV_SIZE; i++) {
            List<ItemStack> shulkerStacks = getShulkerBySlot(inv, i);
            if (shulkerStacks != null)
                shulkers.add(new Shulker(shulkerStacks, i));
        }

        return shulkers;
    }

    // public static Shulker getMinimumItemShulker(Item item) {
    // List<Shulker> shulkers = getAllShulkers();
    // if (shulkers.isEmpty())
    // return null;
    //
    // Shulker minShulker = null;
    // int count = countItemShulker(shulkers.get(0).shulker, item);
    //
    // for (Shulker shulker : shulkers) {
    // int shCount = countItemShulker(shulker.shulker, item);
    // if (shCount == 0)
    // continue;
    // if (shCount < count) {
    // count = shCount;
    // minShulker = shulker;
    // }
    // }
    //
    // return minShulker;
    // }

    // public static int getSlotCount(int slot) {
    // return MinecraftClient.getInstance().player.getInventory().getStack(slot).getCount();
    // }

    public static int countItem(Item item) {
        return countItem(item, true, false);
    }

    public static int countShulkers() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();

        int counter = 0;

        for (int i = 0; i <= GENERIC_INV_SIZE; i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                counter += stack.getCount();
            }
        }

        return counter;
    }


    public static int countItem(Item item, boolean checkOffHand, boolean checkShulkers) {
        return countItem(item, true, checkOffHand, checkShulkers);
    }

    public static int countItem(Item item, boolean checkInventory, boolean checkOffHand, boolean checkShulkers) {
        int counter = 0;

        if (checkOffHand) {
            ItemStack offHandStack = MinecraftClient.getInstance().player.getOffHandStack();
            if (offHandStack.getItem() == item)
                counter += offHandStack.getCount();
        }

        if (checkInventory) {
            PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
            for (int i = 0; i <= GENERIC_INV_SIZE; i++) {
                ItemStack stack = inventory.getStack(i);
                if (stack.getItem() == item)
                    counter += stack.getCount();
            }
        }

        if (checkShulkers) {
            for (Shulker shulker : InvUtility.getAllShulkers()) {
                for (ItemStack stack : shulker.stacks) {
                    if (stack.getItem() == item)
                        counter += stack.getCount();
                }
            }
        }

        return counter;
    }

    public static boolean itemContains(Item item, boolean checkOffHand) {
        return itemContains(item, checkOffHand, false);
    }

    public static boolean itemContains(Item item, boolean checkOffHand, boolean checkShulkers) {
        if (checkOffHand && MinecraftClient.getInstance().player.getOffHandStack().getItem() == item)
            return true;
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int i = 0; i <= GENERIC_INV_SIZE; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item)
                return true;
        }

        if (!checkShulkers)
            return false;

        for (Shulker shulk : InvUtility.getAllShulkers()) {
            for (ItemStack stak : shulk.stacks) {
                if (stak.getItem().equals(item))
                    return true;
            }
        }

        return false;
    }

    // public static void dropEmptyShulker() {
    // int slot = findEmptyShulker(0);
    // int selSlot = MinecraftClient.getInstance().player.getInventory().selectedSlot;
    // if (slot != selSlot) {
    // InvUtility.move(slot, selSlot);
    // return;
    // }
    // MinecraftClient.getInstance().player.dropSelectedItem(false);
    // }

    public static int getItemSlot(Item item, int count) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int i = 0; i < GENERIC_INV_SIZE; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item && (count < 0 || stack.getCount() == count))
                return i;
        }
        return -1;
    }

    // public static void updateSlot(int newSlot) {
    // MinecraftClient.getInstance().player.getInventory().selectedSlot = newSlot;
    // }

    public static int getSelectedSlot() {
        assert MinecraftClient.getInstance().player != null;
        return MinecraftClient.getInstance().player.getInventory().getSelectedSlot();
    }

    // public static void clickEmptySlot() {
    // int emptySlot = findEmptySlot();
    // if (emptySlot != -1)
    // click(emptySlot);
    // }

    // public static void swapSlots(int s1, int s2) {
    // assert MinecraftClient.getInstance().player != null;
    //
    // swapHotbarSlots(s1, 0);
    // swapHotbarSlots(s2, 0);
    // swapHotbarSlots(s1, 0);
    // }

    public static void clickSlot(int id, int button, SlotActionType action) {
        assert MinecraftClient.getInstance().interactionManager != null;
        assert MinecraftClient.getInstance().player != null;

        if (id < 0 || button < 0)
            return;

        MinecraftClient.getInstance().interactionManager.clickSlot(MinecraftClient.getInstance().player.currentScreenHandler.syncId, id, button, action, MinecraftClient.getInstance().player);
    }

    public static int getFirstHotbarSlotId() {
        assert MinecraftClient.getInstance().player != null;
        if (MinecraftClient.getInstance().player.currentScreenHandler instanceof PlayerScreenHandler)
            return 36;
        return MinecraftClient.getInstance().player.currentScreenHandler.slots.size() - 9;
    }


    public static int getStartIndex() {
        assert MinecraftClient.getInstance().player != null;
        return MinecraftClient.getInstance().player.currentScreenHandler.slots.size() - InvUtility.GENERIC_INV_SIZE;
    }

    public static class Shulker {

        public final List<ItemStack> stacks;

        public Shulker(List<ItemStack> shulker, int slot) {
            this.stacks = shulker;
        }

    }
}
