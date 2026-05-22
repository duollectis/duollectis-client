package org.duollectis.client.utils;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Общий Gson-экземпляр с pretty-print и сериализацией null-значений для всего проекта. */
public final class JsonHelper {

	private JsonHelper() {}

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
			.serializeNulls()
			.create();
}
