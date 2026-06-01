package org.duollectis.client.utils;

import static org.duollectis.client.utils.MC.translations;

/**
 * Контракт локализуемого объекта: предоставляет имя и описание через систему переводов.
 * Реализуется модулями и настройками, чтобы GUI мог отображать их локализованные названия.
 */
public interface ITranslatable {

	default String getName() {
		return translations().get(getTranslationPath() + ".name", getId());
	}

	default String getDescription() {
		return translations().get(getTranslationPath() + ".description", "");
	}

	String getId();

	String getTranslationPath();
}
