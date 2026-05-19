package org.duollectis.client.api.menu.proxy;

import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;

/**
 * Stateful row-based layout helper.
 * <p>Tracks the current Y cursor and the left edge ({@code x0}) of the column.
 * Each call to {@link #next()} or {@link #split(int...)} returns the X
 * coordinates for widgets in the current row, then advances the cursor by the
 * row height + gap.
 * <p>Typical usage:
 * <pre>{@code
 * RowLayout row = new RowLayout(lx, startY, lw);
 * // full-width row with a label above it
 * row.label();                          // advance by LABEL_H
 * int[] xs = row.next(lw);             // one full-width slot → xs[0] = lx
 * field = new TextFieldWidget(..., xs[0], row.prevY(), lw, FIELD_H, ...);
 * row.gap();                            // advance by ROW_GAP
 * // two equal columns
 * row.label();
 * int[] xs2 = row.split(lw/2, lw/2);  // xs2[0]=lx, xs2[1]=lx+lw/2+ROW_GAP
 * }</pre>
 */
final class RowLayout {

	/**
	 * Left edge of the column.
	 */
	private final int x0;
	/**
	 * Total available width.
	 */
	private final int totalW;
	/**
	 * Current Y cursor (top of the next widget row).
	 */
	private int y;
	/**
	 * Y of the last placed row (set by {@link #next} / {@link #split}).
	 */
	private int lastY;

	/**
	 * @param x0     left edge
	 * @param startY initial Y position
	 * @param totalW total available width (used for validation / fill)
	 */
	RowLayout(int x0, int startY, int totalW) {
		this.x0 = x0;
		this.y = startY;
		this.lastY = startY;
		this.totalW = totalW;
	}

	// ── cursor movement ───────────────────────────────────────────────────────

	/**
	 * Advances the cursor by {@code LABEL_H} (for a text label above a field).
	 */
	RowLayout label() {
		y += LABEL_H;
		return this;
	}

	/**
	 * Advances the cursor by {@code ROW_GAP}.
	 */
	RowLayout gap() {
		y += ROW_GAP;
		return this;
	}

	/**
	 * Advances the cursor by {@code FIELD_H + ROW_GAP} (after placing a row).
	 */
	RowLayout rowDone() {
		y += FIELD_H + ROW_GAP;
		return this;
	}

	/**
	 * Advances the cursor by an arbitrary amount.
	 */
	RowLayout skip(int pixels) {
		y += pixels;
		return this;
	}

	// ── coordinate queries ────────────────────────────────────────────────────

	/**
	 * Returns the current Y cursor (top of the next widget).
	 */
	int y() {
		return y;
	}

	/**
	 * Returns the Y of the last row placed by {@link #next} or {@link #split}.
	 */
	int prevY() {
		return lastY;
	}

	/**
	 * Returns the left edge {@code x0}.
	 */
	int x() {
		return x0;
	}

	// ── row placement ─────────────────────────────────────────────────────────

	/**
	 * Returns the X positions for a single full-width slot at the current Y,
	 * then records {@code lastY = y}.
	 *
	 * @return array of length 1: {@code [x0]}
	 */
	int[] next() {
		lastY = y;
		return new int[]{x0};
	}

	/**
	 * Returns the X positions for {@code n} equal-width columns separated by
	 * {@code ROW_GAP}, then records {@code lastY = y}.
	 *
	 * @param n number of equal columns
	 * @return array of length {@code n} with X positions
	 */
	int[] splitEqual(int n) {
		int slotW = (totalW - ROW_GAP * (n - 1)) / n;
		int[] xs = new int[n];

		for (int i = 0; i < n; i++) {
			xs[i] = x0 + i * (slotW + ROW_GAP);
		}

		lastY = y;
		return xs;
	}

	/**
	 * Returns the X positions for columns with explicit widths, separated by
	 * {@code ROW_GAP}, then records {@code lastY = y}.
	 * <p>The last width may be {@code -1} to mean "fill remaining space".
	 *
	 * @param widths explicit column widths (last may be -1 for fill)
	 * @return array of X positions, same length as {@code widths}
	 */
	int[] split(int... widths) {
		int[] xs = new int[widths.length];
		int cx = x0;

		// Pre-compute total width consumed by fixed columns + all gaps between columns.
		// The fill slot (-1) takes whatever remains.

		int fixedW = 0;

		for (int w : widths) {
			if (w >= 0) {
				fixedW += w;
			}
		}

		int totalGaps = ROW_GAP * (widths.length - 1);
		int fillW = totalW - fixedW - totalGaps;

		for (int i = 0; i < widths.length; i++) {
			xs[i] = cx;
			int w = widths[i] < 0 ? fillW : widths[i];
			cx += w + ROW_GAP;
		}

		lastY = y;
		return xs;
	}

	/**
	 * Computes equal slot width for {@code n} columns across {@code totalW}.
	 * Convenience for callers that need the width value separately.
	 */
	int slotW(int n) {
		return (totalW - ROW_GAP * (n - 1)) / n;
	}

	/**
	 * Returns the total available width.
	 */
	int totalW() {
		return totalW;
	}
}
