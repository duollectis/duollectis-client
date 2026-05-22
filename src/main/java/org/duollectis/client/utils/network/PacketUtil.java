package org.duollectis.client.utils.network;

import lombok.Getter;
import lombok.Setter;
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

import static org.duollectis.client.utils.MC.mc;

/** Утилиты для отправки игровых пакетов на сервер без прямого доступа к сетевому обработчику. */
public final class PacketUtil {

	@Setter
	@Getter
	private static ClientPlayNetworkHandler playNetworkHandler;

	private PacketUtil() {}

	/**
	 * Отправляет действие с блоком (копание, отмена) и анимацию взмаха рукой.
	 *
	 * @param pos       позиция блока; если {@code null}, вызов игнорируется
	 * @param direction сторона блока; если {@code null}, используется {@link Direction#DOWN}
	 * @param action    тип действия (START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK и т.д.)
	 */
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
		rotate(yaw, pitch, mc.player.isOnGround());
	}

	/**
	 * Отправляет пакет поворота игрока с явным указанием состояния на земле.
	 * Значения {@code -1} для yaw или pitch пропускают отправку.
	 *
	 * @param yaw      горизонтальный угол в градусах
	 * @param pitch    вертикальный угол в градусах
	 * @param onGround находится ли игрок на земле
	 */
	public static void rotate(double yaw, double pitch, boolean onGround) {
		if (yaw == -1 || pitch == -1) {
			return;
		}

		sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
			(float) yaw,
			(float) pitch,
			onGround,
			mc.player.horizontalCollision
		));
	}

	public static void useItem(Hand hand) {
		sendSequencedPacket(s -> new PlayerInteractItemC2SPacket(
			hand,
			s,
			mc.player.getYaw(),
			mc.player.getPitch()
		));
	}

	public static void stopUsingItem() {
		sendSequencedPacket(s -> new PlayerActionC2SPacket(
			PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
			BlockPos.ORIGIN,
			Direction.DOWN
		));
	}

	/**
	 * Отправляет пакет с sequence-номером через {@code interactionManager}.
	 * Sequence необходим для синхронизации инвентаря с сервером.
	 *
	 * @param packetFunction фабрика пакета, принимающая sequence-номер
	 */
	public static void sendSequencedPacket(Function<Integer, Packet<ServerPlayPacketListener>> packetFunction) {
		if (mc.interactionManager == null || mc.world == null) {
			return;
		}

		mc.interactionManager.sendSequencedPacket(
			mc.world,
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
