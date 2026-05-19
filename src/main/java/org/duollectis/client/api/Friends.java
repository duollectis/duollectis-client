package org.duollectis.client.api;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class Friends {

	private static final Friends INSTANCE = new Friends();

	private final List<String> nicknames = new ArrayList<>();

	private Friends() {
	}

	public static Friends getInstance() {
		return INSTANCE;
	}

	public boolean isFriend(PlayerEntity player) {
		return nicknames.contains(player.getName().getString());
	}
}
