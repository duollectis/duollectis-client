package org.duollectis.client.api;

public interface ILoggable {

	void info(Object arg);

	void warning(Object arg);

	void error(Object arg);

	/**
	 * Выводит отладочное сообщение в чат, если включён флаг {@code debug}.
	 *
	 * @param arg объект для отображения
	 */
	void debugInfo(Object arg);
}
