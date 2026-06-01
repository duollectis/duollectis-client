package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.duollectis.client.settings.ItemListSetting;

import java.util.List;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.drawBorder;

/**
 * Виджет редактирования {@link ItemListSetting}: текстовое поле для ввода ID предмета,
 * кнопка «+» для добавления и список текущих предметов с кнопкой «×» для удаления.
 */
public final class ItemListWidget {

	private static final int ITEM_ROW_H = 14;
	private static final int BTN_W = 16;
	private static final int GAP = 2;
	private static final int COLOR_BG = 0xFF0C0F15;
	private static final int COLOR_BORDER = 0xFF2A3A5A;
	private static final int COLOR_ITEM_ROW = 0xFF111820;
	private static final int COLOR_ITEM_ROW_HOV = 0xFF1A2535;
	private static final int COLOR_TEXT = 0xFFCDD5E0;
	private static final int COLOR_TEXT_DIM = 0xFF6A7A8A;
	private static final int COLOR_BTN_ADD = 0xFF163020;
	private static final int COLOR_BTN_ADD_HOV = 0xFF1E4030;
	private static final int COLOR_BTN_ADD_BORDER = 0xFF55AA55;
	private static final int COLOR_BTN_DEL = 0xFF2A0A0A;
	private static final int COLOR_BTN_DEL_HOV = 0xFF4A1A1A;
	private static final int COLOR_BTN_DEL_BORDER = 0xFFAA5555;
	private static final int COLOR_TEXT_ADD = 0xFF55EE88;
	private static final int COLOR_TEXT_DEL = 0xFFFF8888;
	private static final int COLOR_ERROR = 0xFFFF6666;

	private final TextRenderer textRenderer;
	private final ItemListSetting setting;
	private final TextFieldWidget inputField;
	private final int x;
	private final int w;
	private int y;
	private String errorMsg = "";

	public ItemListWidget(TextRenderer textRenderer, int x, int y, int w, int fieldH, ItemListSetting setting) {
		this.textRenderer = textRenderer;
		this.setting = setting;
		this.x = x;
		this.y = y;
		this.w = w;

		int fieldW = w - BTN_W - GAP;
		inputField = new TextFieldWidget(textRenderer, x, y, fieldW, fieldH, Text.empty());
		inputField.setPlaceholder(Text.literal("minecraft:stone").formatted(Formatting.DARK_GRAY));
		inputField.setDrawsBackground(false);
	}

	public void setY(int y) {
		this.y = y;
		inputField.setY(y);
	}

	/** Полная высота виджета: поле ввода + строки предметов. */
	public int totalHeight(int fieldH) {
		List<Item> items = setting.get();
		return fieldH + (items.isEmpty() ? 0 : GAP + items.size() * (ITEM_ROW_H + GAP));
	}

	public boolean isFocused() {
		return inputField.isFocused();
	}

	public void render(DrawContext ctx, int mx, int my, float delta) {
		int fieldH = inputField.getHeight();
		int fieldW = w - BTN_W - GAP;
		int btnX = x + fieldW + GAP;

		ctx.fill(x, y, x + fieldW, y + fieldH, COLOR_BG);
		drawBorder(ctx, x, y, fieldW, fieldH, COLOR_BORDER);
		inputField.render(ctx, mx, my, delta);

		boolean addHov = mx >= btnX && mx < btnX + BTN_W && my >= y && my < y + fieldH;
		ctx.fill(btnX, y, btnX + BTN_W, y + fieldH, addHov ? COLOR_BTN_ADD_HOV : COLOR_BTN_ADD);
		drawBorder(ctx, btnX, y, BTN_W, fieldH, COLOR_BTN_ADD_BORDER);
		int plusX = btnX + (BTN_W - textRenderer.getWidth("+")) / 2;
		int plusY = y + (fieldH - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal("+"), plusX, plusY, COLOR_TEXT_ADD);

		if (!errorMsg.isEmpty()) {
			ctx.drawTextWithShadow(textRenderer, Text.literal(errorMsg), x, y + fieldH + GAP, COLOR_ERROR);
		}

		List<Item> items = setting.get();
		int rowY = y + fieldH + GAP;

		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			String id = Registries.ITEM.getId(item).toString();
			int delBtnX = x + w - BTN_W;
			int labelW = w - BTN_W - GAP - 2;

			boolean rowHov = mx >= x && mx < x + w && my >= rowY && my < rowY + ITEM_ROW_H;
			ctx.fill(x, rowY, x + w, rowY + ITEM_ROW_H, rowHov ? COLOR_ITEM_ROW_HOV : COLOR_ITEM_ROW);
			drawBorder(ctx, x, rowY, w, ITEM_ROW_H, COLOR_BORDER);

			String label = textRenderer.trimToWidth(id, labelW);
			ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 2, rowY + (ITEM_ROW_H - 8) / 2, COLOR_TEXT);

			boolean delHov = mx >= delBtnX && mx < delBtnX + BTN_W && my >= rowY && my < rowY + ITEM_ROW_H;
			ctx.fill(delBtnX, rowY, delBtnX + BTN_W, rowY + ITEM_ROW_H, delHov ? COLOR_BTN_DEL_HOV : COLOR_BTN_DEL);
			drawBorder(ctx, delBtnX, rowY, BTN_W, ITEM_ROW_H, COLOR_BTN_DEL_BORDER);
			int xBtnTextX = delBtnX + (BTN_W - textRenderer.getWidth("×")) / 2;
			ctx.drawTextWithShadow(textRenderer, Text.literal("×"), xBtnTextX, rowY + (ITEM_ROW_H - 8) / 2, COLOR_TEXT_DEL);

			rowY += ITEM_ROW_H + GAP;
		}

		if (items.isEmpty()) {
			ctx.drawTextWithShadow(
					textRenderer,
					Text.literal("(пусто)"),
					x + 2,
					y + inputField.getHeight() + GAP + (ITEM_ROW_H - 8) / 2,
					COLOR_TEXT_DIM
			);
		}
	}

	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() != 0) {
			return false;
		}

		double mx = click.x();
		double my = click.y();
		int fieldH = inputField.getHeight();
		int fieldW = w - BTN_W - GAP;
		int btnX = x + fieldW + GAP;

		if (mx >= x && mx < x + fieldW && my >= y && my < y + fieldH) {
			inputField.setFocused(true);
			inputField.mouseClicked(click, doubled);
			return true;
		}

		if (mx >= btnX && mx < btnX + BTN_W && my >= y && my < y + fieldH) {
			tryAddItem();
			return true;
		}

		List<Item> items = setting.get();
		int rowY = y + fieldH + GAP;

		for (int i = 0; i < items.size(); i++) {
			int delBtnX = x + w - BTN_W;

			if (mx >= delBtnX && mx < delBtnX + BTN_W && my >= rowY && my < rowY + ITEM_ROW_H) {
				setting.get().remove(i);
				errorMsg = "";
				return true;
			}

			rowY += ITEM_ROW_H + GAP;
		}

		inputField.setFocused(false);
		return false;
	}

	public boolean keyPressed(KeyInput input) {
		if (!inputField.isFocused()) {
			return false;
		}

		if (input.isEnter()) {
			tryAddItem();
			return true;
		}

		return inputField.keyPressed(input);
	}

	public boolean charTyped(CharInput input) {
		return inputField.isFocused() && inputField.charTyped(input);
	}

	private void tryAddItem() {
		String raw = inputField.getText().trim();

		if (raw.isEmpty()) {
			errorMsg = "Введите ID предмета";
			return;
		}

		Identifier id = Identifier.tryParse(raw);

		if (id == null || !Registries.ITEM.containsId(id)) {
			errorMsg = "Неизвестный предмет: " + raw;
			return;
		}

		Item item = Registries.ITEM.get(id);

		if (setting.getFilter() != null && !setting.getFilter().test(item)) {
			errorMsg = "Предмет не прошёл фильтр";
			return;
		}

		if (setting.get().contains(item)) {
			errorMsg = "Уже добавлен";
			return;
		}

		setting.get().add(item);
		inputField.setText("");
		errorMsg = "";
	}

}
