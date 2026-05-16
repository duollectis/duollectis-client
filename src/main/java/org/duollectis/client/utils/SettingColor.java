/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.duollectis.client.utils;

import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;

public class SettingColor extends Color {
    public boolean rainbow;

    public SettingColor(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    public SettingColor(int r, int g, int b, int a, boolean rainbow) {
        super(r, g, b, a);
        this.rainbow = rainbow;
    }


    @Override
    public JsonObject toTag() {
        JsonObject tag = super.toTag();
        tag.addProperty("rainbow", rainbow);
        return tag;
    }

    @Override
    public SettingColor fromTag(JsonObject tag) {
        super.fromTag(tag);
        rainbow = tag.get("rainbow").getAsBoolean();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        return rainbow == ((SettingColor) o).rainbow;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (rainbow ? 1 : 0);
        return result;
    }
}
