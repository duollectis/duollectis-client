package org.duollectis.client.utils;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonHelper {

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
			.serializeNulls()
			.create();
}
