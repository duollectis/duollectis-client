package org.duollectis.client.utils;

import com.google.gson.JsonObject;

/**
 * Контракт сериализации объекта в JSON и обратно.
 * Реализуется классами, которые должны сохраняться на диск (модули, прокси и т.д.).
 */
public interface ISerializable {

	JsonObject toJson();

	void fromJson(JsonObject jsonObject);
}
