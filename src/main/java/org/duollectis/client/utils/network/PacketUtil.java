package org.duollectis.client.utils.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Function;

public class PacketUtil {

    private static ClientPlayNetworkHandler playNetworkHandler;

    public static ClientPlayNetworkHandler getPlayNetworkHandler() {
        return playNetworkHandler;
    }

    public static void setPlayNetworkHandler(ClientPlayNetworkHandler handler) {
        playNetworkHandler = handler;
    }

    public static void breakAction(BlockPos pos, Direction direction, PlayerActionC2SPacket.Action action) {
        if (pos == null) {
            return;
        }

        Direction resolvedDirection = direction != null ? direction : Direction.DOWN;
        sendSequencedPacket(s -> new PlayerActionC2SPacket(action, pos, resolvedDirection, s));
        swingHand(Hand.MAIN_HAND);
    }

    public static void swingHand(Hand hand) {
        sendPacket(new HandSwingC2SPacket(hand));
    }

    public static void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, MinecraftClient.getInstance().player.isOnGround());
    }

    public static void rotate(double yaw, double pitch, boolean onGround) {
        if (yaw == -1 || pitch == -1) {
            return;
        }

        sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                (float) yaw,
                (float) pitch,
                onGround,
                MinecraftClient.getInstance().player.horizontalCollision
        ));
    }

    public static void useItem(Hand hand) {
        sendSequencedPacket(s -> new PlayerInteractItemC2SPacket(
                hand,
                s,
                MinecraftClient.getInstance().player.getYaw(),
                MinecraftClient.getInstance().player.getPitch()
        ));
    }

    public static void stopUsingItem() {
        sendSequencedPacket(s -> new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
    }

    public static void sendSequencedPacket(Function<Integer, Packet<ServerPlayPacketListener>> packetFunction) {
        if (MinecraftClient.getInstance().interactionManager == null
                || MinecraftClient.getInstance().world == null) {
            return;
        }

        MinecraftClient.getInstance().interactionManager.sendSequencedPacket(
                MinecraftClient.getInstance().world,
                packetFunction::apply
        );
    }

    public static void sendPacket(Packet<?> packet) {
        if (packet == null || playNetworkHandler == null) {
            return;
        }

        playNetworkHandler.sendPacket(packet);
    }
}
