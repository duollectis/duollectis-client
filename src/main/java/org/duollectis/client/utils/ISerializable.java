package org.duollectis.client.utils;

import com.google.gson.JsonObject;

public interface ISerializable {

	JsonObject toJson();

	void fromJson(JsonObject jsonObject);
}
