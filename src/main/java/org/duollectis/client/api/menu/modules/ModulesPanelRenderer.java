package org.duollectis.client.api.menu.modules;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.menu.main.ClientScreen;
import org.duollectis.client.api.menu.main.ClientScreenLayout;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.*;

import java.util.List;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

/**
 * Утилита рендеринга панели модулей: список модулей, кнопки биндов,
 * вкладки переключения и панель настроек выбранного модуля.
 */
public final class ModulesPanelRenderer {

	private ModulesPanelRenderer() {}

	public static void renderPanel(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			List<Module> modules,
			int listScroll,
			Module selectedModule,
			int settingsScroll,
			ClientScreen.Tab activeTab,
			Module bindListeningModule
	) {
		ctx.fill(panelX + 3, panelY + 3, panelX + PANEL_W + 3, panelY + PANEL_H + 3, COLOR_SHADOW);
		ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COLOR_PANEL_BG);
		ClientScreenLayout.drawBorder(ctx, panelX, panelY, PANEL_W, PANEL_H, COLOR_PANEL_BORDER);

		int listRight = panelX + LIST_W;
		ctx.fill(panelX, panelY, listRight, panelY + PANEL_H, COLOR_LIST_BG);
		ctx.fill(listRight, panelY, listRight + DIVIDER_W, panelY + PANEL_H, COLOR_DIVIDER);

		renderModuleList(ctx, mx, my, panelX, panelY, textRenderer, modules, listScroll, selectedModule, bindListeningModule);
		renderTabButtons(ctx, mx, my, panelX, panelY, textRenderer, activeTab);

		if (activeTab == ClientScreen.Tab.MODULES) {
			renderSettingsPanel(ctx, panelX, panelY, mx, my, textRenderer, selectedModule, settingsScroll);
		}

		renderLangLabel(ctx, panelX, panelY, textRenderer);
	}

	private static void renderModuleList(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			List<Module> modules,
			int listScroll,
			Module selectedModule,
			Module bindListeningModule
	) {
		int listRight = panelX + LIST_W;
		int clipTop = panelY + PAD;
		int clipBottom = panelY + PAD + listAreaH();
		int visibleCount = listAreaH() / ENTRY_H;
		int maxScroll = Math.max(0, modules.size() - visibleCount);
		int startIndex = Math.clamp(listScroll, 0, maxScroll);

		ctx.enableScissor(panelX, clipTop, listRight, clipBottom);

		for (int i = startIndex; i < modules.size(); i++) {
			int relativeIndex = i - startIndex;
			int entryY = panelY + PAD + relativeIndex * ENTRY_H;

			if (entryY + ENTRY_H > clipBottom) {
				break;
			}

			Module module = modules.get(i);
			boolean isSelected = module == selectedModule;
			boolean isHovered = mx >= panelX && mx < listRight && my >= entryY && my < entryY + ENTRY_H;
			boolean isActive = module.isActive();

			int bg = isSelected ? COLOR_ENTRY_SELECTED
				: isHovered ? COLOR_ENTRY_HOVER
				: COLOR_ENTRY_NORMAL;

			ctx.fill(panelX, entryY, listRight, entryY + ENTRY_H, bg);

			int dotColor = isActive ? COLOR_ACTIVE_DOT : COLOR_INACTIVE_DOT;
			ctx.fill(panelX + 4, entryY + ENTRY_H / 2 - 2, panelX + 7, entryY + ENTRY_H / 2 + 2, dotColor);

			int nameColor = isActive ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;
			ctx.drawTextWithShadow(
				textRenderer,
				Text.literal(module.getName()),
				panelX + 12,
				entryY + (ENTRY_H - 8) / 2,
				nameColor
			);

			renderBindButton(ctx, textRenderer, module, panelX, entryY, mx, my, bindListeningModule);
		}

		ctx.disableScissor();
	}

	private static void renderBindButton(
			DrawContext ctx,
			TextRenderer textRenderer,
			Module module,
			int panelX,
			int entryY,
			int mx,
			int my,
			Module bindListeningModule
	) {
		int btnX = panelX + LIST_W - BIND_BTN_PAD - BIND_BTN_W;
		int btnY = entryY + (ENTRY_H - BIND_BTN_H) / 2;

		boolean isListening = module == bindListeningModule;
		boolean isHovered = mx >= btnX && mx < btnX + BIND_BTN_W && my >= btnY && my < btnY + BIND_BTN_H;

		int btnBg = isListening ? 0xFF1A3A1A
			: isHovered ? COLOR_BTN_HOVER
			: COLOR_BTN_BG;

		ctx.fill(btnX, btnY, btnX + BIND_BTN_W, btnY + BIND_BTN_H, btnBg);
		ClientScreenLayout.drawBorder(ctx, btnX, btnY, BIND_BTN_W, BIND_BTN_H, COLOR_BTN_BORDER);

		String label = isListening
			? "..."
			: (module.getKeyName() == null ? "---" : module.getKeyName());

		int maxLabelW = BIND_BTN_W - 4;
		String trimmed = textRenderer.getWidth(label) <= maxLabelW
			? label
			: trimToWidth(textRenderer, label, maxLabelW);

		int labelColor = isListening ? 0xFF55EE88 : COLOR_TEXT_DIM;
		int labelX = btnX + (BIND_BTN_W - textRenderer.getWidth(trimmed)) / 2;
		int labelY = btnY + (BIND_BTN_H - 8) / 2;

		ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), labelX, labelY, labelColor);
	}

	private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
		StringBuilder result = new StringBuilder();

		for (char c : text.toCharArray()) {
			if (textRenderer.getWidth(result + String.valueOf(c)) > maxWidth) {
				break;
			}

			result.append(c);
		}

		return result.toString();
	}

	private static void renderTabButtons(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			ClientScreen.Tab activeTab
	) {
		int tabsY = panelY + PAD + listAreaH() + (TAB_ROW_H - TAB_BTN_H) / 2;
		int tabsX = panelX + PAD;

		renderSingleTab(ctx, mx, my, textRenderer, tabsX, tabsY, "Модули", activeTab == ClientScreen.Tab.MODULES);
		renderSingleTab(
			ctx, mx, my, textRenderer,
			tabsX + TAB_BTN_W + TAB_BTN_GAP, tabsY,
			"Акки", activeTab == ClientScreen.Tab.ACCOUNTS
		);
	}

	private static void renderSingleTab(
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			int x,
			int y,
			String label,
			boolean active
	) {
		boolean hovered = mx >= x && mx < x + TAB_BTN_W && my >= y && my < y + TAB_BTN_H;

		int bg = active ? COLOR_ENTRY_SELECTED
			: hovered ? COLOR_BTN_HOVER
			: COLOR_BTN_BG;

		ctx.fill(x, y, x + TAB_BTN_W, y + TAB_BTN_H, bg);
		ClientScreenLayout.drawBorder(ctx, x, y, TAB_BTN_W, TAB_BTN_H, active ? COLOR_ACTIVE_DOT : COLOR_BTN_BORDER);

		int textColor = active ? COLOR_TEXT_ACTIVE : COLOR_BTN_TEXT;
		int textX = x + (TAB_BTN_W - textRenderer.getWidth(label)) / 2;
		int textY = y + (TAB_BTN_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal(label), textX, textY, textColor);
	}

	private static void renderSettingsPanel(
			DrawContext ctx,
			int panelX,
			int panelY,
			int mx,
			int my,
			TextRenderer textRenderer,
			Module selectedModule,
			int settingsScroll
	) {
		if (selectedModule == null) {
			ctx.drawCenteredTextWithShadow(
				textRenderer,
				Text.literal("Select a module"),
				panelX + LIST_W + DIVIDER_W + (PANEL_W - LIST_W - DIVIDER_W) / 2,
				panelY + PANEL_H / 2,
				COLOR_TEXT_DIM
			);

			return;
		}

		int settingsX = panelX + LIST_W + DIVIDER_W + PAD;
		int settingsW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int contentTop = panelY + PAD + ROW_H;
		int panelBottom = panelY + PANEL_H - PAD;

		ctx.drawTextWithShadow(
			textRenderer,
			Text.literal(selectedModule.getName()),
			settingsX,
			panelY + PAD + (ROW_H - 8) / 2,
			COLOR_TITLE
		);

		ctx.enableScissor(panelX + LIST_W + DIVIDER_W, contentTop, panelX + PANEL_W, panelBottom);

		int y = contentTop - settingsScroll;

		for (SettingGroup group : selectedModule.getSettings().getGroups()) {
			if (y + ROW_H > contentTop && y < panelBottom) {
				ctx.drawTextWithShadow(
					textRenderer,
					Text.literal(group.id()),
					settingsX,
					y + (ROW_H - 8) / 2,
					COLOR_GROUP_HEADER
				);
				ctx.fill(settingsX, y + ROW_H, settingsX + settingsW, y + ROW_H + 1, COLOR_GROUP_LINE);
			}

			y += ROW_H;

			List<Setting<?>> visibleSettings = group.settings()
				.stream()
				.filter(Setting::isVisible)
				.toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				Setting<?> setting = visibleSettings.get(i);
				int itemH = itemHeight(textRenderer, setting);

				if (y + itemH > contentTop && y < panelBottom) {
					renderSettingName(ctx, textRenderer, setting, settingsX, settingsW, y);
					renderSettingResetIcon(ctx, textRenderer, setting, panelX, y, itemH, mx, my);
				}

				y += itemH;

				boolean isLast = i == visibleSettings.size() - 1;

				if (isLast) {
					continue;
				}

				y += SETTING_DIVIDER_PAD;

				if (y > contentTop && y < panelBottom) {
					ctx.fill(settingsX, y, settingsX + settingsW, y + 1, COLOR_SETTING_DIVIDER);
				}

				y += SETTING_DIVIDER_PAD + 1;
			}
		}

		ctx.disableScissor();
	}

	/**
		* Вычисляет X-координату иконки сброса для строки настройки.
		* Иконка прижата к правому краю панели настроек.
		*/
	public static int settingResetIconX(int panelX) {
		return panelX + PANEL_W - PAD - SETTING_RESET_W;
	}

	/**
		* Вычисляет Y-координату иконки сброса — центрирует по высоте строки настройки.
		*/
	public static int settingResetIconY(int itemY, int itemH) {
		return itemY + (itemH - SETTING_RESET_H) / 2;
	}

	private static void renderSettingResetIcon(
			DrawContext ctx,
			TextRenderer textRenderer,
			Setting<?> setting,
			int panelX,
			int itemY,
			int itemH,
			int mx,
			int my
	) {
		boolean changed = setting.wasChanged();
		int iconX = settingResetIconX(panelX);
		int iconY = settingResetIconY(itemY, itemH);

		boolean rowHovered = mx >= panelX + LIST_W + DIVIDER_W
			&& mx < panelX + PANEL_W
			&& my >= itemY
			&& my < itemY + itemH;

		boolean visible = changed || rowHovered;

		if (!visible) {
			return;
		}

		boolean iconHovered = mx >= iconX && mx < iconX + SETTING_RESET_W
			&& my >= iconY && my < iconY + SETTING_RESET_H;

		int bg = changed
			? (iconHovered ? 0xFF3A1A1A : 0xFF1E0E0E)
			: (iconHovered ? COLOR_BTN_HOVER : 0x00000000);

		int borderColor = changed ? 0xFF773333 : COLOR_BTN_BORDER;
		int iconColor = changed ? 0xFFEE6655 : COLOR_TEXT_DIM;

		if (bg != 0) {
			ctx.fill(iconX, iconY, iconX + SETTING_RESET_W, iconY + SETTING_RESET_H, bg);
		}

		if (changed || iconHovered) {
			ClientScreenLayout.drawBorder(ctx, iconX, iconY, SETTING_RESET_W, SETTING_RESET_H, borderColor);
		}

		String icon = "\u21BA";
		int iconTextX = iconX + (SETTING_RESET_W - textRenderer.getWidth(icon)) / 2;
		int iconTextY = iconY + (SETTING_RESET_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal(icon), iconTextX, iconTextY, iconColor);
	}

	public static int itemHeight(TextRenderer textRenderer, Setting<?> setting) {
		if (setting instanceof ItemListSetting itemListSetting) {
			int fieldH = ROW_H;
			int itemRowH = 14;
			int gap = 2;
			List<net.minecraft.item.Item> items = itemListSetting.get();
			int listH = items.isEmpty() ? itemRowH + gap : items.size() * (itemRowH + gap);
			int widgetH = textRenderer.fontHeight + NAME_GAP + fieldH + gap + listH;
			return offsetTop(setting) + widgetH + offsetBottom(setting);
		}

		int widgetH = isInlineWidget(setting)
			? ROW_H
			: textRenderer.fontHeight + nameGap(setting) + ROW_H;
		return offsetTop(setting) + widgetH + offsetBottom(setting);
	}

	public static int nameGap(Setting<?> setting) {
		return isSlider(setting) ? SLIDER_NAME_GAP : NAME_GAP;
	}

	public static int offsetTop(Setting<?> setting) {
		if (setting instanceof BoolSetting) {
			return BOOL_OFFSET_TOP;
		}

		if (isSlider(setting)) {
			return SLIDER_OFFSET_TOP;
		}

		if (setting instanceof EnumSetting) {
			return ENUM_OFFSET_TOP;
		}

		return FIELD_OFFSET_TOP;
	}

	public static int offsetBottom(Setting<?> setting) {
		if (setting instanceof BoolSetting) {
			return BOOL_OFFSET_BOTTOM;
		}

		if (isSlider(setting)) {
			return SLIDER_OFFSET_BOTTOM;
		}

		if (setting instanceof EnumSetting) {
			return ENUM_OFFSET_BOTTOM;
		}

		return FIELD_OFFSET_BOTTOM;
	}

	private static boolean isSlider(Setting<?> setting) {
		return setting instanceof IntSetting intS && intS.isSlider()
			|| setting instanceof DoubleSetting dblS && dblS.isSlider();
	}

	private static boolean isInlineWidget(Setting<?> setting) {
		return setting instanceof BoolSetting || setting instanceof EnumSetting;
	}

	private static void renderSettingName(
			DrawContext ctx,
			TextRenderer textRenderer,
			Setting<?> setting,
			int settingsX,
			int settingsW,
			int y
	) {
		if (setting instanceof BoolSetting) {
			return;
		}

		int top = offsetTop(setting);
		String name = textRenderer.trimToWidth(setting.getName(), settingsW);
		int nameY = setting instanceof EnumSetting
			? y + top + (ROW_H - 8) / 2
			: y + top;

		ctx.drawTextWithShadow(textRenderer, Text.literal(name), settingsX, nameY, COLOR_TEXT_DIM);
	}

	private static void renderLangLabel(
			DrawContext ctx,
			int panelX,
			int panelY,
			TextRenderer textRenderer
	) {
		String langLabel = "Lang: ";
		int langLabelX = panelX + PANEL_W - LANG_FIELD_W - LANG_BTN_W - PAD
			- textRenderer.getWidth(langLabel) - LANG_BTN_GAP;
		int langLabelY = panelY + PANEL_H + PAD + (ROW_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal(langLabel), langLabelX, langLabelY, COLOR_TEXT_DIM);
	}
}
