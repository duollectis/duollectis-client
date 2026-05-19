/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.duollectis.client.mixin.modules.autoreconnect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.AutoReconnect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

	@Shadow
	@Final
	private DirectionalLayoutWidget grid;
	@Unique
	private ButtonWidget reconnectBtn;
	@Unique
	private double time = ModuleManager.getInstance().get(AutoReconnect.class).getTime().get() * 20;

	protected DisconnectedScreenMixin(Text title) {
		super(title);
	}

	@Inject(
			method = "init",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V",
					shift = At.Shift.BEFORE
			)
	)
	private void addButtons(CallbackInfo ci) {
		AutoReconnect autoReconnect = ModuleManager.getInstance().get(AutoReconnect.class);

		if (autoReconnect.getLastServerConnection() != null && autoReconnect.getButton().get() == false) {
			reconnectBtn = new ButtonWidget.Builder(Text.literal(getText()), button -> tryConnecting()).build();
			grid.add(reconnectBtn);

			grid.add(
					new ButtonWidget.Builder(
							Text.literal("Toggle Auto Reconnect"), button -> {
								autoReconnect.toggle();
								reconnectBtn.setMessage(Text.literal(getText()));
								time = autoReconnect.getTime().get() * 20;
							}
					).build()
			);
		}
	}

	@Override
	public void tick() {
		AutoReconnect autoReconnect = ModuleManager.getInstance().get(AutoReconnect.class);

		if (autoReconnect.isActive() == false || autoReconnect.getLastServerConnection() == null) {
			return;
		}

		if (time <= 0) {
			tryConnecting();
		} else {
			time--;

			if (reconnectBtn != null) {
				reconnectBtn.setMessage(Text.literal(getText()));
			}
		}
	}

	@Unique
	private String getText() {
		String reconnectText = "Reconnect";
		if (ModuleManager.getInstance().get(AutoReconnect.class).isActive())
			reconnectText += " " + String.format("(%.1f)", time / 20);
		return reconnectText;
	}

	@Unique
	private void tryConnecting() {
		var lastServer = ModuleManager.getInstance().get(AutoReconnect.class).getLastServerConnection();
		ConnectScreen.connect(
				new TitleScreen(),
				MinecraftClient.getInstance(),
				lastServer.left(),
				lastServer.right(),
				false,
				null
		);
	}
}
