package org.duollectis.client.utils.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public record FindItemResult(int slot, int count) {

    private static final int OFFHAND_SLOT_ID = 45;
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;
    private static final int MAIN_INV_START = 9;
    private static final int MAIN_INV_END = 35;
    private static final int ARMOR_START = 36;
    private static final int ARMOR_END = 39;

    public boolean found() {
        return this.slot != -1;
    }

    public Hand getHand() {
        if (this.slot == OFFHAND_SLOT_ID) {
            return Hand.OFF_HAND;
        }

        int selectedSlot = MinecraftClient.getInstance().player.getInventory().getSelectedSlot();
        return this.slot == selectedSlot ? Hand.MAIN_HAND : null;
    }

    public boolean isMainHand() {
        return this.getHand() == Hand.MAIN_HAND;
    }

    public boolean isOffhand() {
        return this.getHand() == Hand.OFF_HAND;
    }

    public boolean isHotbar() {
        return this.slot >= HOTBAR_START && this.slot <= HOTBAR_END;
    }

    public boolean isMain() {
        return this.slot >= MAIN_INV_START && this.slot <= MAIN_INV_END;
    }

    public boolean isArmor() {
        return this.slot >= ARMOR_START && this.slot <= ARMOR_END;
    }
}
