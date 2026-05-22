package org.duollectis.client.api.menu.widget;

/** Утилиты анимации виджетов: функция сглаживания smoothstep и линейная интерполяция цветов ARGB. */
public final class WidgetAnimUtils {

	private WidgetAnimUtils() {}

	public static float smoothstep(float t) {
		float clamped = Math.clamp(t, 0f, 1f);
		return clamped * clamped * (3f - 2f * clamped);
	}

	public static int lerpColor(int from, int to, float t) {
		int aFrom = (from >> 24) & 0xFF;
		int rFrom = (from >> 16) & 0xFF;
		int gFrom = (from >> 8) & 0xFF;
		int bFrom = from & 0xFF;

		int aTo = (to >> 24) & 0xFF;
		int rTo = (to >> 16) & 0xFF;
		int gTo = (to >> 8) & 0xFF;
		int bTo = to & 0xFF;

		int a = aFrom + Math.round((aTo - aFrom) * t);
		int r = rFrom + Math.round((rTo - rFrom) * t);
		int g = gFrom + Math.round((gTo - gFrom) * t);
		int b = bFrom + Math.round((bTo - bFrom) * t);

		return (a << 24) | (r << 16) | (g << 8) | b;
	}

}
