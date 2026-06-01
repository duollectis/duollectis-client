package org.duollectis.client.api.clickgui;

/** Константы разметки и цветовая палитра ClickGui. */
public final class ClickGuiLayout {

	/** Минимальная ширина панели категории. */
	public static final int PANEL_MIN_W = 100;

	/** Максимальная ширина панели категории. */
	public static final int PANEL_MAX_W = 180;

	/**
	 * Суммарный горизонтальный паддинг для вычисления ширины панели по тексту.
	 * Слагаемые: левый отступ(10) + точка(7) + зазор(3) + зазор_до_бинда(6) + стрелка(8) + зазор(4) + BIND_BTN_W(30) + правый отступ(4).
	 */
	public static final int PANEL_W_PADDING = 10 + 7 + 3 + 6 + 8 + 4 + 30 + 4;

	/** Ширина по умолчанию (используется до первого рендера). */
	public static final int PANEL_W = 130;

	public static final int PANEL_GAP = 6;
	public static final int HEADER_H = 16;
	public static final int MODULE_H = 18;
	public static final int PAD = 5;

	/** Высота строки Bool/Enum настройки (имя слева, значение справа). */
	public static final int SETTING_INLINE_H = 14;

	/** Высота строки слайдера (прогресс-бар на всю ширину). */
	public static final int SETTING_SLIDER_H = 14;

	public static final int BIND_BTN_W = 30;
	public static final int BIND_BTN_H = 10;

	// Цветовая палитра
	public static final int COLOR_PANEL_BG = 0xEE0D1017;
	public static final int COLOR_PANEL_BORDER = 0xFF1E2D45;
	public static final int COLOR_HEADER_BG = 0xFF0A1520;
	public static final int COLOR_HEADER_ACTIVE = 0xFF0E2035;
	public static final int COLOR_MODULE_BG = 0xFF0C1018;
	public static final int COLOR_MODULE_HOVER = 0xFF131C2A;
	public static final int COLOR_MODULE_ACTIVE = 0xFF0F2218;
	public static final int COLOR_MODULE_ACTIVE_HOVER = 0xFF163020;
	public static final int COLOR_SETTINGS_BG = 0xFF080D14;
	public static final int COLOR_SETTING_ROW = 0xFF0A1018;
	public static final int COLOR_SETTING_ROW_HOVER = 0xFF111820;

	/** Цвет заливки прогресс-бара слайдера. */
	public static final int COLOR_SLIDER_FILL = 0xFF0E2A3A;

	/** Цвет заливки прогресс-бара при наведении. */
	public static final int COLOR_SLIDER_FILL_HOVER = 0xFF143548;

	/** Цвет заливки поля ввода слайдера (когда активен режим ввода). */
	public static final int COLOR_SLIDER_EDIT_BG = 0xFF0A1E2C;

	public static final int COLOR_ACTIVE_DOT = 0xFF44DD77;
	public static final int COLOR_INACTIVE_DOT = 0xFF1E2A38;
	public static final int COLOR_TEXT_PRIMARY = 0xFFCDD5E0;
	public static final int COLOR_TEXT_ACTIVE = 0xFF44DD77;
	public static final int COLOR_TEXT_DIM = 0xFF5A6A7A;
	public static final int COLOR_TEXT_HEADER = 0xFF88AACC;
	public static final int COLOR_TEXT_VALUE = 0xFF8ABCDD;
	public static final int COLOR_BORDER = 0xFF1A2840;
	public static final int COLOR_BTN_BG = 0xFF101820;
	public static final int COLOR_BTN_HOVER = 0xFF1A2838;
	public static final int COLOR_BTN_BORDER = 0xFF243448;
	public static final int COLOR_BTN_LISTEN = 0xFF1A3A1A;
	public static final int COLOR_EXPANDED_ARROW = 0xFF5599CC;

	private ClickGuiLayout() {}
}
