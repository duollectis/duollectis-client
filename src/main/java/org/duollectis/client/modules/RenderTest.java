package org.duollectis.client.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.HudRenderEvent;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.utils.render.RenderUtils;

import static org.duollectis.client.utils.MC.mc;

@ModuleInfo(
	value = "render-test",
	category = Category.RENDER
)
public class RenderTest extends Module {

	private static final int COLOR_PLAYER_FILL = 0x2200AAFF;
	private static final int COLOR_PLAYER_OUTLINE = 0xFF00AAFF;
	private static final int COLOR_MOB_FILL = 0x22FF4400;
	private static final int COLOR_MOB_OUTLINE = 0xFFFF4400;
	private static final int COLOR_ENTITY_FILL = 0x2200FF88;
	private static final int COLOR_ENTITY_OUTLINE = 0xFF00FF88;
	private static final int COLOR_FOOT_BLOCK = 0xFFFFFF00;
	private static final int COLOR_CROSSHAIR_BLOCK = 0xFFFF00FF;
	private static final int COLOR_HUD_BG = 0xAA000000;
	private static final int COLOR_HUD_BORDER = 0xFF00AAFF;
	private static final int COLOR_TEXT = 0xFFFFFFFF;

	private static final float ESP_LINE_WIDTH = 1.5f;
	private static final float BLOCK_LINE_WIDTH = 2.0f;
	private static final int HUD_X = 4;
	private static final int HUD_Y = 4;
	private static final int HUD_WIDTH = 170;
	private static final int HUD_LINE_HEIGHT = 10;
	private static final int HUD_PADDING = 4;
	private static final int HUD_BORDER_WIDTH = 1;

	private final BoolSetting espPlayers = settings.add(
		BoolSetting.create("esp-players")
			.defaultValue(true)
			.done()
	);

	private final BoolSetting espMobs = settings.add(
		BoolSetting.create("esp-mobs")
			.defaultValue(true)
			.done()
	);

	private final BoolSetting espEntities = settings.add(
		BoolSetting.create("esp-entities")
			.defaultValue(false)
			.done()
	);

	private final BoolSetting espFilled = settings.add(
		BoolSetting.create("esp-filled")
			.defaultValue(true)
			.done()
	);

	private final BoolSetting espThroughWalls = settings.add(
		BoolSetting.create("esp-through-walls")
			.defaultValue(false)
			.done()
	);

	private final DoubleSetting espRange = settings.add(
		DoubleSetting.create("esp-range")
			.defaultValue(32.0)
			.range(4.0, 128.0)
			.sliderRange(4.0, 64.0)
			.decimalPlaces(0)
			.done()
	);

	private final BoolSetting showFootBlock = settings.add(
		BoolSetting.create("foot-block")
			.defaultValue(true)
			.done()
	);

	private final BoolSetting showCrosshairBlock = settings.add(
		BoolSetting.create("crosshair-block")
			.defaultValue(true)
			.done()
	);

	@Subscribe
	private void onRender(RenderEvent event) {
		if (mc.player == null || mc.world == null) {
			return;
		}

		float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
		boolean throughWalls = espThroughWalls.get();
		double range = espRange.get();
		boolean filled = espFilled.get();

		for (Entity entity : mc.world.getEntities()) {
			if (entity == mc.player) {
				continue;
			}

			if (entity.squaredDistanceTo(mc.player) > range * range) {
				continue;
			}

			if (entity instanceof PlayerEntity) {
				if (espPlayers.get()) {
					renderEntityBox(event, entity, tickDelta, COLOR_PLAYER_FILL, COLOR_PLAYER_OUTLINE, filled, throughWalls);
				}
			} else if (entity instanceof LivingEntity) {
				if (espMobs.get()) {
					renderEntityBox(event, entity, tickDelta, COLOR_MOB_FILL, COLOR_MOB_OUTLINE, filled, throughWalls);
				}
			} else if (espEntities.get()) {
				renderEntityBox(event, entity, tickDelta, COLOR_ENTITY_FILL, COLOR_ENTITY_OUTLINE, filled, throughWalls);
			}
		}

		if (showFootBlock.get()) {
			BlockPos foot = mc.player.getBlockPos();
			RenderUtils.drawLinesBox(event.getMatrices(), foot, COLOR_FOOT_BLOCK, BLOCK_LINE_WIDTH, false);
		}

		if (showCrosshairBlock.get()) {
			HitResult hit = mc.crosshairTarget;

			if (hit instanceof BlockHitResult blockHit) {
				RenderUtils.drawLinesBox(event.getMatrices(), blockHit.getBlockPos(), COLOR_CROSSHAIR_BLOCK, BLOCK_LINE_WIDTH, false);
			}
		}
	}

	@Subscribe
	private void onHud(HudRenderEvent event) {
		int lineCount = 6;
		int panelHeight = HUD_PADDING * 2 + lineCount * HUD_LINE_HEIGHT;

		RenderUtils.drawRect(event.getContext(), HUD_X, HUD_Y, HUD_WIDTH, panelHeight, COLOR_HUD_BG);
		RenderUtils.drawRectOutline(event.getContext(), HUD_X, HUD_Y, HUD_WIDTH, panelHeight, HUD_BORDER_WIDTH, COLOR_HUD_BORDER);

		int textX = HUD_X + HUD_PADDING;
		int textY = HUD_Y + HUD_PADDING;

		RenderUtils.drawText(event.getContext(), "§b[RenderTest ESP]", textX, textY, COLOR_TEXT);
		textY += HUD_LINE_HEIGHT;

		RenderUtils.drawText(event.getContext(), "Players:   " + activeLabel(espPlayers.get()), textX, textY, COLOR_TEXT);
		textY += HUD_LINE_HEIGHT;

		RenderUtils.drawText(event.getContext(), "Mobs:      " + activeLabel(espMobs.get()), textX, textY, COLOR_TEXT);
		textY += HUD_LINE_HEIGHT;

		RenderUtils.drawText(event.getContext(), "Entities:  " + activeLabel(espEntities.get()), textX, textY, COLOR_TEXT);
		textY += HUD_LINE_HEIGHT;

		RenderUtils.drawText(event.getContext(), "FootBlock: " + activeLabel(showFootBlock.get()), textX, textY, COLOR_TEXT);
		textY += HUD_LINE_HEIGHT;

		RenderUtils.drawText(event.getContext(), "Crosshair: " + activeLabel(showCrosshairBlock.get()), textX, textY, COLOR_TEXT);
	}

	private static void renderEntityBox(
		RenderEvent event,
		Entity entity,
		float tickDelta,
		int fillColor,
		int outlineColor,
		boolean filled,
		boolean throughWalls
	) {
		if (filled) {
			RenderUtils.drawEntityFilledBox(
				event.getMatrices(),
				entity,
				tickDelta,
				fillColor,
				outlineColor,
				ESP_LINE_WIDTH,
				throughWalls
			);
		} else {
			RenderUtils.drawEntityBox(
				event.getMatrices(),
				entity,
				tickDelta,
				outlineColor,
				ESP_LINE_WIDTH,
				throughWalls
			);
		}
	}

	private static String activeLabel(boolean active) {
		return active ? "§aON" : "§cOFF";
	}

}
