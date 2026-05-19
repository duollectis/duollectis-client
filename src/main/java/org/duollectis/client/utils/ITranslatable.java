package org.duollectis.client.utils;

import org.duollectis.client.api.i18n.ClientTranslations;

public interface ITranslatable {

	ClientTranslations tr = ClientTranslations.getInstance();

	default String getName() {
		return tr.translate(getTranslationPath() + ".name");
	}

	default String getDescription() {
		return tr.translate(getTranslationPath() + ".description");
	}

	String getTranslationPath();
}
