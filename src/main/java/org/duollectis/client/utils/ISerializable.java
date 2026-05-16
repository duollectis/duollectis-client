package org.duollectis.client.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface ISerializable<T> {
    JsonObject toTag();

    T fromTag(JsonObject tag);

}
