package org.duollectis.client.utils.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

public class InvUtils {

    private static final int OFFHAND_SLOT_ID = 45;

    private static final Action ACTION = new Action();

    private static int previousSlot = -1;

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(MinecraftClient.getInstance().player.getMainHandStack());
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(MinecraftClient.getInstance().player.getOffHandStack());
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

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (testInOffHand(isGood)) {
            return new FindItemResult(OFFHAND_SLOT_ID, MinecraftClient.getInstance().player.getOffHandStack().getCount());
        }

        if (testInMainHand(isGood)) {
            return new FindItemResult(
                    MinecraftClient.getInstance().player.getInventory().getSelectedSlot(),
                    MinecraftClient.getInstance().player.getMainHandStack().getCount()
            );
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        if (MinecraftClient.getInstance().player == null) {
            return new FindItemResult(0, 0);
        }

        return find(isGood, 0, MinecraftClient.getInstance().player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (MinecraftClient.getInstance().player == null) {
            return new FindItemResult(0, 0);
        }

        int foundSlot = -1;
        int totalCount = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = MinecraftClient.getInstance().player.getInventory().getStack(i);

            if (isGood.test(stack) == false) {
                continue;
            }

            if (foundSlot == -1) {
                foundSlot = i;
            }

            totalCount += stack.getCount();
        }

        return new FindItemResult(foundSlot, totalCount);
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == OFFHAND_SLOT_ID) {
            return true;
        }

        if (slot < 0 || slot > 8) {
            return false;
        }

        if (swapBack && previousSlot == -1) {
            previousSlot = MinecraftClient.getInstance().player.getInventory().getSelectedSlot();
        } else if (!swapBack) {
            previousSlot = -1;
        }

        MinecraftClient.getInstance().player.getInventory().setSelectedSlot(slot);
        MinecraftClient.getInstance().interactionManager.syncSelectedSlot();
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
            return this.fromId(SlotUtils.indexToId(index));
        }

        public void toId(int id) {
            this.to = id;
            this.run();
        }

        public void to(int index) {
            this.toId(SlotUtils.indexToId(index));
        }

        public void toHotbar(int i) {
            this.to(i);
        }

        public void slotId(int id) {
            this.from = this.to = id;
            this.run();
        }

        private void run() {
            boolean hadEmptyCursor = MinecraftClient.getInstance()
                    .player
                    .currentScreenHandler
                    .getCursorStack()
                    .isEmpty();

            if (this.type == SlotActionType.SWAP) {
                this.data = this.from;
                this.from = this.to;
            }

            if (this.type != null && this.from != -1 && this.to != -1) {
                this.click(this.from);

                if (this.two) {
                    this.click(this.to);
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

            boolean cursorNotEmpty = !MinecraftClient.getInstance()
                    .player
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
            MinecraftClient.getInstance().interactionManager.clickSlot(
                    MinecraftClient.getInstance().player.currentScreenHandler.syncId,
                    id,
                    this.data,
                    this.type,
                    MinecraftClient.getInstance().player
            );
        }
    }
}
