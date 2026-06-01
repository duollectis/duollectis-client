package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.drawBorder;

/**
 * Модальный диалог подтверждения действия.
 * Хранит текст вопроса и колбэк, вызываемый при нажатии «Да».
 * Умеет сам себя рендерить и обрабатывать клики.
 */
public record ConfirmDialog(String message, Runnable onConfirm) {

	private static final int DIALOG_W = 200;
	private static final int DIALOG_H = 70;
	private static final int BTN_W = 60;
	private static final int BTN_H = 18;
	private static final int BTN_BOTTOM_PAD = 10;

	public void render(DrawContext ctx, TextRenderer tr, int mx, int my, int screenW, int screenH) {
		int dialogX = (screenW - DIALOG_W) / 2;
		int dialogY = (screenH - DIALOG_H) / 2;

		ctx.fill(0, 0, screenW, screenH, 0x88000000);
		ctx.fill(dialogX, dialogY, dialogX + DIALOG_W, dialogY + DIALOG_H, 0xFF0D1A2E);
		drawBorder(ctx, dialogX, dialogY, DIALOG_W, DIALOG_H, 0xFF2A3A5A);

		int msgW = tr.getWidth(message);
		int msgX = dialogX + (DIALOG_W - msgW) / 2;
		ctx.drawTextWithShadow(tr, Text.literal(message), msgX, dialogY + 12, 0xFFCCDDEE);

		int btnY = dialogY + DIALOG_H - BTN_H - BTN_BOTTOM_PAD;
		int yesBtnX = dialogX + DIALOG_W / 2 - BTN_W - 5;
		int noBtnX = dialogX + DIALOG_W / 2 + 5;

		boolean yesHov = mx >= yesBtnX && mx < yesBtnX + BTN_W && my >= btnY && my < btnY + BTN_H;
		boolean noHov = mx >= noBtnX && mx < noBtnX + BTN_W && my >= btnY && my < btnY + BTN_H;

		ctx.fill(yesBtnX, btnY, yesBtnX + BTN_W, btnY + BTN_H, yesHov ? 0xFF1A4A1A : 0xFF0A2A0A);
		drawBorder(ctx, yesBtnX, btnY, BTN_W, BTN_H, 0xFF55AA55);
		int yesTextX = yesBtnX + (BTN_W - tr.getWidth("Да")) / 2;
		ctx.drawTextWithShadow(tr, Text.literal("Да"), yesTextX, btnY + 5, 0xFF88FF88);

		ctx.fill(noBtnX, btnY, noBtnX + BTN_W, btnY + BTN_H, noHov ? 0xFF4A1A1A : 0xFF2A0A0A);
		drawBorder(ctx, noBtnX, btnY, BTN_W, BTN_H, 0xFFAA5555);
		int noTextX = noBtnX + (BTN_W - tr.getWidth("Нет")) / 2;
		ctx.drawTextWithShadow(tr, Text.literal("Нет"), noTextX, btnY + 5, 0xFFFF8888);
	}

	/**
	 * Обрабатывает клик. Возвращает true если клик поглощён диалогом.
	 * При попадании в кнопку «Да» вызывает {@link #onConfirm()}.
	 */
	public boolean handleClick(double mx, double my, int screenW, int screenH) {
		int dialogX = (screenW - DIALOG_W) / 2;
		int dialogY = (screenH - DIALOG_H) / 2;

		int btnY = dialogY + DIALOG_H - BTN_H - BTN_BOTTOM_PAD;
		int yesBtnX = dialogX + DIALOG_W / 2 - BTN_W - 5;

		if (mx >= yesBtnX && mx < yesBtnX + BTN_W && my >= btnY && my < btnY + BTN_H) {
			onConfirm.run();
			return true;
		}

		return mx >= dialogX && mx < dialogX + DIALOG_W && my >= dialogY && my < dialogY + DIALOG_H;
	}
}
