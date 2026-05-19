package org.duollectis.client.utils.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class ScreenHandlerUtil {

    public static void clickSlot(
            ScreenHandler screenHandler,
            int slotId,
            int button,
            SlotActionType actionType,
            PlayerEntity player
    ) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        DefaultedList<Slot> defaultedList = screenHandler.slots;

        int i = defaultedList.size();
        List<ItemStack> list = Lists.newArrayListWithCapacity(i);

        for (Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
        }

        screenHandler.onSlotClick(slotId, button, actionType, player);
        Int2ObjectMap<ItemStackHash> int2ObjectMap = new Int2ObjectOpenHashMap<>();

        ComponentChangesHash.ComponentHasher hasher = MinecraftClient
                .getInstance()
                .player
                .networkHandler
                .getComponentHasher();

        for (int j = 0; j < i; ++j) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = defaultedList.get(j).getStack();

            if (!ItemStack.areEqual(itemStack, itemStack2)) {
                int2ObjectMap.put(j, ItemStackHash.fromItemStack(itemStack2, hasher));
            }
        }

        PacketUtil.sendPacket(new ClickSlotC2SPacket(
                screenHandler.syncId,
                screenHandler.getRevision(),
                Shorts.checkedCast(slotId),
                SignedBytes.checkedCast(button),
                actionType,
                int2ObjectMap,
                ItemStackHash.fromItemStack(screenHandler.getCursorStack(), hasher)
        ));
    }
}
