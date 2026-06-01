package org.duollectis.client.api.clickgui.widget;

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
 * Всплывающий оверлей выбора предмета для {@link ItemListSetting}.
 * Отображается поверх ClickGui по центру экрана.
 * Содержит строку фильтра и прокручиваемый список всех предметов Minecraft.
 * ЛКМ по предмету — добавить в список настройки и закрыть оверлей.
 * Escape или клик вне окна — закрыть без изменений.
 */
public final class ItemPickerOverlay {

	private static final int OVERLAY_W = 200;
	private static final int OVERLAY_H = 220;
	private static final int FILTER_H = 18;
	private static final int ITEM_ROW_H = 13;
	private static final int LIST_PAD = 3;

	private final ItemListSetting setting;
	private final Runnable onClose;

	private TextFieldWidget filterField;
	private List<Item> allItems;
	private List<Item> filteredItems;
	private int scrollOffset;

	/** X-координата левого края оверлея (вычисляется при открытии). */
	private int overlayX;

	/** Y-координата верхнего края оверлея (вычисляется при открытии). */
	private int overlayY;

	public ItemPickerOverlay(ItemListSetting setting, Runnable onClose) {
		this.setting = setting;
		this.onClose = onClose;
	}

	/** Инициализирует оверлей: вычисляет позицию, создаёт поле фильтра, загружает список предметов. */
	public void init(TextRenderer textRenderer, int screenW, int screenH) {
		overlayX = (screenW - OVERLAY_W) / 2;
		overlayY = (screenH - OVERLAY_H) / 2;

		filterField = new TextFieldWidget(
			textRenderer,
			overlayX + LIST_PAD,
			overlayY + LIST_PAD,
			OVERLAY_W - LIST_PAD * 2,
			FILTER_H - 2,
			Text.empty()
		);
		filterField.setPlaceholder(Text.literal("Фильтр..."));
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

	public void render(DrawContext ctx, TextRenderer textRenderer, int mx, int my) {
		// Затемнение фона
		ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), 0x88000000);

		// Тень оверлея
		ctx.fill(overlayX + 3, overlayY + 3, overlayX + OVERLAY_W + 3, overlayY + OVERLAY_H + 3, 0x66000000);

		// Фон оверлея
		ctx.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + OVERLAY_H, 0xFF0D1520);
		drawBorder(ctx, overlayX, overlayY, OVERLAY_W, OVERLAY_H, COLOR_PANEL_BORDER);

		// Строка фильтра
		ctx.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + FILTER_H, 0xFF0A1018);
		drawBorder(ctx, overlayX, overlayY, OVERLAY_W, FILTER_H, COLOR_BORDER);
		filterField.render(ctx, mx, my, 0);

		// Список предметов
		int listY = overlayY + FILTER_H;
		int listH = OVERLAY_H - FILTER_H;
		int visibleCount = listH / ITEM_ROW_H;

		ctx.enableScissor(overlayX, listY, overlayX + OVERLAY_W, overlayY + OVERLAY_H);

		int y = listY - scrollOffset % ITEM_ROW_H;
		int startIndex = scrollOffset / ITEM_ROW_H;

		for (int i = startIndex; i < filteredItems.size() && y < overlayY + OVERLAY_H; i++) {
			Item item = filteredItems.get(i);
			boolean hovered = mx >= overlayX && mx < overlayX + OVERLAY_W && my >= y && my < y + ITEM_ROW_H;
			boolean alreadyAdded = setting.get().contains(item);

			int rowBg = alreadyAdded
				? (hovered ? 0xFF0F3020 : 0xFF0A2018)
				: (hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW);

			ctx.fill(overlayX, y, overlayX + OVERLAY_W, y + ITEM_ROW_H, rowBg);

			String itemId = Registries.ITEM.getId(item).getPath();
			String trimmed = textRenderer.trimToWidth(itemId, OVERLAY_W - LIST_PAD * 2 - (alreadyAdded ? 12 : 0));
			int textColor = alreadyAdded ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;

			ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), overlayX + LIST_PAD, y + (ITEM_ROW_H - 8) / 2, textColor);

			if (alreadyAdded) {
				ctx.drawTextWithShadow(textRenderer, Text.literal("✓"), overlayX + OVERLAY_W - LIST_PAD - 8, y + (ITEM_ROW_H - 8) / 2, COLOR_TEXT_ACTIVE);
			}

			y += ITEM_ROW_H;
		}

		ctx.disableScissor();

		// Скроллбар
		renderScrollbar(ctx, listY, listH, visibleCount);
	}

	private void renderScrollbar(DrawContext ctx, int listY, int listH, int visibleCount) {
		if (filteredItems.size() <= visibleCount) {
			return;
		}

		int totalH = filteredItems.size() * ITEM_ROW_H;
		int maxScroll = totalH - listH;
		int trackH = listH - 4;
		int thumbH = Math.max(16, trackH * listH / totalH);
		int thumbY = listY + 2 + (int) ((long) (trackH - thumbH) * scrollOffset / maxScroll);

		int scrollbarX = overlayX + OVERLAY_W - 4;
		ctx.fill(scrollbarX, listY + 2, scrollbarX + 2, listY + 2 + trackH, 0xFF1A2840);
		ctx.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbH, COLOR_EXPANDED_ARROW);
	}

	public boolean mouseClicked(double mx, double my, int btn) {
		// Клик вне оверлея — закрыть
		if (mx < overlayX || mx >= overlayX + OVERLAY_W || my < overlayY || my >= overlayY + OVERLAY_H) {
			onClose.run();
			return true;
		}

		// Клик по полю фильтра
		if (my >= overlayY && my < overlayY + FILTER_H) {
			return true;
		}

		if (btn != GLFW.GLFW_MOUSE_BUTTON_1) {
			return true;
		}

		// Клик по предмету в списке
		int listY = overlayY + FILTER_H;
		int startIndex = scrollOffset / ITEM_ROW_H;
		int yOffset = listY - scrollOffset % ITEM_ROW_H;

		for (int i = startIndex; i < filteredItems.size(); i++) {
			int rowY = yOffset + (i - startIndex) * ITEM_ROW_H;

			if (rowY >= overlayY + OVERLAY_H) {
				break;
			}

			if (my >= rowY && my < rowY + ITEM_ROW_H) {
				Item item = filteredItems.get(i);
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
				return true;
			}
		}

		return true;
	}

	public boolean mouseScrolled(double mx, double my, double vAmt) {
		if (mx < overlayX || mx >= overlayX + OVERLAY_W || my < overlayY || my >= overlayY + OVERLAY_H) {
			return false;
		}

		int listH = OVERLAY_H - FILTER_H;
		int totalH = filteredItems.size() * ITEM_ROW_H;
		int maxScroll = Math.max(0, totalH - listH);
		scrollOffset = (int) Math.clamp(scrollOffset - vAmt * ITEM_ROW_H * 3, 0, maxScroll);
		return true;
	}

	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			onClose.run();
			return true;
		}

		filterField.keyPressed(input);
		rebuildFilter();
		return true;
	}

	public boolean charTyped(CharInput input) {
		filterField.charTyped(input);
		rebuildFilter();
		return true;
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

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
