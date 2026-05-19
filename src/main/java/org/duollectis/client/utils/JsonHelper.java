package org.duollectis.client.utils;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonHelper {

	public final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
			.serializeNulls()
			.create();
}
