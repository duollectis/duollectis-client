package org.duollectis.client.utils;

import org.duollectis.client.api.i18n.ClientTranslations;

/**
 * Контракт локализуемого объекта: предоставляет имя и описание через систему переводов.
 * Реализуется модулями и настройками, чтобы GUI мог отображать их локализованные названия.
 */
public interface ITranslatable {

	ClientTranslations tr = ClientTranslations.getInstance();

	default String getName() {
		return tr.get(getTranslationPath() + ".name", getId());
	}

	default String getDescription() {
		return tr.get(getTranslationPath() + ".description", "");
	}

	String getId();

	String getTranslationPath();
}
