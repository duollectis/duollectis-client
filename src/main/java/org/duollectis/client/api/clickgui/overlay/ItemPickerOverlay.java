package org.duollectis.client.api.clickgui.overlay;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.duollectis.client.settings.ItemListSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Оверлей выбора предмета для {@link ItemListSetting}.
 * Отображает поле фильтра и прокручиваемый список всех предметов Minecraft.
 * ЛКМ по предмету — добавить/убрать из списка настройки.
 * Уже добавленные предметы отмечены галочкой ✓.
 */
public final class ItemPickerOverlay extends ClickGuiOverlay {

	private static final int FILTER_H = 18;
	private static final int ITEM_ROW_H = 13;
	private static final int LIST_PAD = 4;

	private final ItemListSetting setting;

	private TextFieldWidget filterField;
	private List<Item> allItems;
	private List<Item> filteredItems;
	private int scrollOffset;

	public ItemPickerOverlay(ItemListSetting setting, Runnable onClose) {
		super(220, 240, "Выбор предмета", onClose);
		this.setting = setting;
	}

	@Override
	protected void onInit(TextRenderer textRenderer) {
		int fieldY = overlayY + TITLE_H + LIST_PAD;
		int fieldW = overlayW - LIST_PAD * 2;

		filterField = new TextFieldWidget(
			textRenderer,
			overlayX + LIST_PAD,
			fieldY,
			fieldW,
			FILTER_H - 2,
			Text.empty()
		);
		filterField.setPlaceholder(Text.literal("Фильтр по названию..."));
		filterField.setDrawsBackground(false);
		filterField.setFocused(true);

		Predicate<Item> filter = setting.getFilter();
		allItems = new ArrayList<>();

		for (Identifier id : Registries.ITEM.getIds()) {
			Item item = Registries.ITEM.get(id);

			if (filter == null || filter.test(item)) {
				allItems.add(item);
			}
		}

		allItems.sort((a, b) -> {
			String nameA = Registries.ITEM.getId(a).getPath();
			String nameB = Registries.ITEM.getId(b).getPath();
			return nameA.compareTo(nameB);
		});

		filteredItems = new ArrayList<>(allItems);
		scrollOffset = 0;
	}

	@Override
	protected void renderContent(DrawContext ctx, TextRenderer textRenderer, int mx, int my) {
		int filterAreaY = overlayY + TITLE_H;
		int filterAreaH = FILTER_H + LIST_PAD * 2;

		ctx.fill(overlayX, filterAreaY, overlayX + overlayW, filterAreaY + filterAreaH, 0xFF0A1018);
		drawBorder(ctx, overlayX, filterAreaY, overlayW, filterAreaH, COLOR_BORDER);

		filterField.setX(overlayX + LIST_PAD);
		filterField.setY(filterAreaY + LIST_PAD);
		filterField.setWidth(overlayW - LIST_PAD * 2);
		filterField.render(ctx, mx, my, 0);

		int listStartY = filterAreaY + filterAreaH;
		int listH = overlayY + overlayH - listStartY;

		ctx.enableScissor(overlayX, listStartY, overlayX + overlayW, overlayY + overlayH);

		int startIndex = scrollOffset / ITEM_ROW_H;
		int yOffset = listStartY - scrollOffset % ITEM_ROW_H;

		for (int i = startIndex; i < filteredItems.size(); i++) {
			int rowY = yOffset + (i - startIndex) * ITEM_ROW_H;

			if (rowY >= overlayY + overlayH) {
				break;
			}

			Item item = filteredItems.get(i);
			boolean hovered = mx >= overlayX && mx < overlayX + overlayW && my >= rowY && my < rowY + ITEM_ROW_H;
			boolean added = setting.get().contains(item);

			int rowBg = added
				? (hovered ? 0xFF163020 : 0xFF0F2018)
				: (hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW);

			ctx.fill(overlayX, rowY, overlayX + overlayW, rowY + ITEM_ROW_H, rowBg);

			String itemId = Registries.ITEM.getId(item).getPath();
			int maxW = overlayW - LIST_PAD * 2 - (added ? 12 : 0);
			String trimmed = textRenderer.trimToWidth(itemId, maxW);
			int textColor = added ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;

			ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), overlayX + LIST_PAD, rowY + (ITEM_ROW_H - 8) / 2, textColor);

			if (added) {
				int checkX = overlayX + overlayW - LIST_PAD - textRenderer.getWidth("✓");
				ctx.drawTextWithShadow(textRenderer, Text.literal("✓"), checkX, rowY + (ITEM_ROW_H - 8) / 2, COLOR_TEXT_ACTIVE);
			}
		}

		ctx.disableScissor();

		renderScrollbar(ctx, listStartY, listH);
	}

	private void renderScrollbar(DrawContext ctx, int listStartY, int listH) {
		int totalH = filteredItems.size() * ITEM_ROW_H;

		if (totalH <= listH) {
			return;
		}

		int maxScroll = totalH - listH;
		int trackH = listH - 4;
		int thumbH = Math.max(16, trackH * listH / totalH);
		int thumbY = listStartY + 2 + (int) ((long) (trackH - thumbH) * scrollOffset / maxScroll);

		int scrollbarX = overlayX + overlayW - 4;
		ctx.fill(scrollbarX, listStartY + 2, scrollbarX + 2, listStartY + 2 + trackH, 0xFF1A2840);
		ctx.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbH, COLOR_EXPANDED_ARROW);
	}

	@Override
	protected boolean onMouseClicked(double mx, double my, int btn) {
		int filterAreaY = overlayY + TITLE_H;
		int filterAreaH = FILTER_H + LIST_PAD * 2;
		int listStartY = filterAreaY + filterAreaH;

		if (my < listStartY) {
			return true;
		}

		if (btn != GLFW.GLFW_MOUSE_BUTTON_1) {
			return true;
		}

		int startIndex = scrollOffset / ITEM_ROW_H;
		int yOffset = listStartY - scrollOffset % ITEM_ROW_H;

		for (int i = startIndex; i < filteredItems.size(); i++) {
			int rowY = yOffset + (i - startIndex) * ITEM_ROW_H;

			if (rowY >= overlayY + overlayH) {
				break;
			}

			if (my >= rowY && my < rowY + ITEM_ROW_H) {
				toggleItem(filteredItems.get(i));
				return true;
			}
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double vAmt) {
		if (!super.mouseScrolled(mx, my, vAmt)) {
			return false;
		}

		int filterAreaH = FILTER_H + LIST_PAD * 2;
		int listH = overlayH - TITLE_H - filterAreaH;
		int totalH = filteredItems.size() * ITEM_ROW_H;
		int maxScroll = Math.max(0, totalH - listH);
		scrollOffset = (int) Math.clamp(scrollOffset - vAmt * ITEM_ROW_H * 3, 0, maxScroll);
		return true;
	}

	@Override
	protected boolean onKeyPressed(KeyInput input) {
		filterField.keyPressed(input);
		rebuildFilter();
		return true;
	}

	@Override
	public boolean charTyped(CharInput input) {
		filterField.charTyped(input);
		rebuildFilter();
		return true;
	}

	private void toggleItem(Item item) {
		List<Item> items = setting.get();

		if (items.contains(item)) {
			items.remove(item);
		}
		else {
			if (setting.getFilter() == null || setting.getFilter().test(item)) {
				items.add(item);
			}
		}

		setting.onChanged();
	}

	private void rebuildFilter() {
		String query = filterField.getText().trim().toLowerCase();
		filteredItems = allItems.stream()
			.filter(item -> {
				String id = Registries.ITEM.getId(item).getPath();
				return query.isEmpty() || id.contains(query);
			})
			.toList();

		scrollOffset = 0;
	}
}
