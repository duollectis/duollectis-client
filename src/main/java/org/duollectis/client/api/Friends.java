package org.duollectis.client.api;

import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class Friends {

	@Getter
	private static Friends instance = new Friends();

	private final List<String> nicknames = new ArrayList<>();

	private Friends() {
	}

	public boolean isFriend(PlayerEntity player) {
		return nicknames.contains(player.getName().getString());
	}
}
