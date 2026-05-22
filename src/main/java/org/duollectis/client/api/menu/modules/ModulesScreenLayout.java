package org.duollectis.client.api.menu.modules;

import net.minecraft.client.gui.DrawContext;

/**
 * Константы разметки и цветовая палитра экрана модулей.
 * Все размеры в пикселях, цвета в формате ARGB.
 */
public final class ModulesScreenLayout {

	public static final int PANEL_W = 520;
	public static final int PANEL_H = 340;
	public static final int LIST_W = 150;
	public static final int DIVIDER_W = 1;
	public static final int PAD = 8;
	public static final int ENTRY_H = 22;
	public static final int ROW_H = 20;
	public static final int NAME_GAP = 2;
	public static final int SLIDER_NAME_GAP = 0;
	public static final int SETTING_DIVIDER_PAD = 3;

	/** Высота строки вкладок под списком модулей. */
	public static final int TAB_ROW_H = 18;
	/** Ширина одной кнопки вкладки. */
	public static final int TAB_BTN_W = (LIST_W - PAD * 2 - 2) / 2;
	/** Высота кнопки вкладки. */
	public static final int TAB_BTN_H = 14;
	/** Зазор между кнопками вкладок. */
	public static final int TAB_BTN_GAP = 2;

	public static final int ACC_ENTRY_H = 20;
	public static final int ACC_FIELD_H = 20;
	public static final int ACC_ROW_GAP = 4;
	/** Высота нижней области: статус + текущий ник + зазоры. */
	public static final int ACC_FORM_H = 8 + ACC_ROW_GAP + 8 + ACC_ROW_GAP;

	public static final int BOOL_OFFSET_TOP = 0;
	public static final int BOOL_OFFSET_BOTTOM = 0;
	public static final int SLIDER_OFFSET_TOP = 4;
	public static final int SLIDER_OFFSET_BOTTOM = 0;
	public static final int ENUM_OFFSET_TOP = 4;
	public static final int ENUM_OFFSET_BOTTOM = 4;
	public static final int FIELD_OFFSET_TOP = 2;
	public static final int FIELD_OFFSET_BOTTOM = 2;

	/** Ширина кнопки бинда в строке модуля. */
	public static final int BIND_BTN_W = 34;
	/** Высота кнопки бинда в строке модуля. */
	public static final int BIND_BTN_H = 14;
	/** Отступ кнопки бинда от правого края панели. */
	public static final int BIND_BTN_PAD = 4;

	public static final int SLIDER_INPUT_W = 150;
	public static final int ENUM_DROPDOWN_W = 100;
	public static final int LANG_FIELD_W = 80;
	public static final int LANG_BTN_W = 50;
	public static final int LANG_BTN_GAP = 4;

	// --- Цветовая палитра: тёмно-синяя тема ---
	public static final int COLOR_PANEL_BG = 0xF0111318;
	public static final int COLOR_PANEL_BORDER = 0xFF2A3A5A;
	public static final int COLOR_LIST_BG = 0xFF0C0F15;
	public static final int COLOR_DIVIDER = 0xFF1E2A40;
	public static final int COLOR_ENTRY_NORMAL = 0xFF0C0F15;
	public static final int COLOR_ENTRY_HOVER = 0xFF141A28;
	public static final int COLOR_ENTRY_SELECTED = 0xFF182440;
	public static final int COLOR_ACTIVE_DOT = 0xFF55EE88;
	public static final int COLOR_INACTIVE_DOT = 0xFF2A3040;
	public static final int COLOR_TEXT_PRIMARY = 0xFFCDD5E0;
	public static final int COLOR_TEXT_DIM = 0xFF6A7A8A;
	public static final int COLOR_TEXT_ACTIVE = 0xFF55EE88;
	public static final int COLOR_GROUP_HEADER = 0xFF5599CC;
	public static final int COLOR_GROUP_LINE = 0xFF1E3050;
	public static final int COLOR_SETTING_DIVIDER = 0xFF161E2E;
	public static final int COLOR_TITLE = 0xFFAAC8F0;
	public static final int COLOR_BTN_BG = 0xFF141C2C;
	public static final int COLOR_BTN_HOVER = 0xFF1E2E48;
	public static final int COLOR_BTN_ACTIVE = 0xFF163020;
	public static final int COLOR_BTN_ACTIVE_HOV = 0xFF1E4030;
	public static final int COLOR_BTN_BORDER = 0xFF2A3A5A;
	public static final int COLOR_BTN_TEXT = 0xFFBBCCDD;
	public static final int COLOR_BTN_TEXT_ON = 0xFF55EE88;
	public static final int COLOR_OVERLAY_BG = 0x88050810;
	public static final int COLOR_SHADOW = 0x66000510;

	private ModulesScreenLayout() {}

	/** Режимы сортировки списка аккаунтов. */
	public enum AccSortMode {
		ADD("Добавление"),
		A_Z("A → Z"),
		Z_A("Z → A"),
		GROUP("Группа");

		public final String label;

		AccSortMode(String label) {
			this.label = label;
		}

		public AccSortMode next() {
			AccSortMode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}

	/** Высота области списка модулей (без строки вкладок). */
	public static int listAreaH() {
		return PANEL_H - PAD * 2 - TAB_ROW_H;
	}

	public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
