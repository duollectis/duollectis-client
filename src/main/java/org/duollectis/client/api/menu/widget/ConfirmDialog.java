package org.duollectis.client.api.menu.widget;

/**
 * Модальный диалог подтверждения действия.
 * Хранит текст вопроса и колбэк, вызываемый при нажатии «Да».
 */
public record ConfirmDialog(String message, Runnable onConfirm) {
}
