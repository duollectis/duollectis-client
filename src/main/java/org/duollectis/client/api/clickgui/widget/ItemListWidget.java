package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.duollectis.client.api.clickgui.ClickGuiScreen;
import org.duollectis.client.api.clickgui.overlay.ItemPickerOverlay;
import org.duollectis.client.settings.ItemListSetting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Виджет списка предметов.
 * Первая строка: имя + кнопка "✕" для очистки списка.
 * Следующие строки: каждый предмет в списке с кнопкой удаления "✕".
 * Последняя строка: кнопка "+ Добавить" — открывает {@link ItemPickerOverlay}.
 */
public final class ItemListWidget implements ClickGuiSettingWidget {

	private static final int ITEM_ROW_H = 12;
	private static final int HEADER_ROW_H = SETTING_INLINE_H;
	private static final int ADD_BTN_H = 13;
	private static final int REMOVE_BTN_W = 10;

	private final ItemListSetting setting;

	public ItemListWidget(ItemListSetting setting) {
		this.setting = setting;
	}

	@Override
	public int height() {
		return HEADER_ROW_H + setting.get().size() * ITEM_ROW_H + ADD_BTN_H;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		renderHeader(ctx, textRenderer, x, y, width, mx, my);

		List<Item> items = setting.get();
		int itemY = y + HEADER_ROW_H;

		for (int i = 0; i < items.size(); i++) {
			renderItemRow(ctx, textRenderer, x, itemY, width, mx, my, items.get(i));
			itemY += ITEM_ROW_H;
		}

		renderAddButton(ctx, textRenderer, x, itemY, width, mx, my);
	}

	private void renderHeader(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + HEADER_ROW_H;
		ctx.fill(x, y, x + width, y + HEADER_ROW_H, hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW);

		String name = textRenderer.trimToWidth(setting.getName(), width - PAD * 2 - 14);
		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (HEADER_ROW_H - 8) / 2, COLOR_TEXT_DIM);

		int clearX = x + width - PAD - 10;
		boolean clearHov = mx >= clearX && mx < clearX + 10 && my >= y && my < y + HEADER_ROW_H;
		int clearColor = clearHov ? 0xFFFF4444 : COLOR_TEXT_DIM;
		ctx.drawTextWithShadow(textRenderer, Text.literal("✕"), clearX, y + (HEADER_ROW_H - 8) / 2, clearColor);
	}

	private void renderItemRow(
			DrawContext ctx,
			TextRenderer textRenderer,
			int x,
			int y,
			int width,
			int mx,
			int my,
			Item item
	) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + ITEM_ROW_H;
		ctx.fill(x, y, x + width, y + ITEM_ROW_H, hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW);

		String itemId = Registries.ITEM.getId(item).getPath();
		String trimmed = textRenderer.trimToWidth(itemId, width - PAD * 2 - REMOVE_BTN_W - 4);
		ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), x + PAD, y + (ITEM_ROW_H - 8) / 2, COLOR_TEXT_VALUE);

		int removeX = x + width - PAD - REMOVE_BTN_W;
		boolean removeHov = mx >= removeX && mx < removeX + REMOVE_BTN_W && my >= y && my < y + ITEM_ROW_H;
		int removeColor = removeHov ? 0xFFFF4444 : COLOR_TEXT_DIM;
		ctx.drawTextWithShadow(textRenderer, Text.literal("✕"), removeX, y + (ITEM_ROW_H - 8) / 2, removeColor);
	}

	private void renderAddButton(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + ADD_BTN_H;
		ctx.fill(x, y, x + width, y + ADD_BTN_H, hovered ? 0xFF0F2A18 : 0xFF0A1E12);
		drawBorder(ctx, x, y, width, ADD_BTN_H, COLOR_BORDER);

		String label = "+ Добавить";
		int labelX = x + (width - textRenderer.getWidth(label)) / 2;
		int labelColor = hovered ? COLOR_TEXT_ACTIVE : COLOR_TEXT_DIM;
		ctx.drawTextWithShadow(textRenderer, Text.literal(label), labelX, y + (ADD_BTN_H - 8) / 2, labelColor);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		if (mx < x || mx >= x + width || my < y || my >= y + height()) {
			return false;
		}

		if (btn != GLFW.GLFW_MOUSE_BUTTON_1) {
			return true;
		}

		if (my >= y && my < y + HEADER_ROW_H) {
			int clearX = x + width - PAD - 10;

			if (mx >= clearX && mx < clearX + 10) {
				setting.get().clear();
				setting.onChanged();
			}

			return true;
		}

		List<Item> items = setting.get();
		int itemY = y + HEADER_ROW_H;

		for (int i = 0; i < items.size(); i++) {
			if (my >= itemY && my < itemY + ITEM_ROW_H) {
				int removeX = x + width - PAD - REMOVE_BTN_W;

				if (mx >= removeX && mx < removeX + REMOVE_BTN_W) {
					items.remove(i);
					setting.onChanged();
				}

				return true;
			}

			itemY += ITEM_ROW_H;
		}

		if (my >= itemY && my < itemY + ADD_BTN_H) {
			ClickGuiScreen.getInstance().openOverlay(
				new ItemPickerOverlay(setting, () -> ClickGuiScreen.getInstance().closeOverlay())
			);
			return true;
		}

		return true;
	}

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
