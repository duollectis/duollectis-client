package org.duollectis.client.utils;

/**
 * Контракт условной видимости элемента (настройки, виджета).
 * Используется для динамического скрытия настроек в зависимости от состояния других настроек.
 */
public interface IVisible {
	boolean isVisible();
}
