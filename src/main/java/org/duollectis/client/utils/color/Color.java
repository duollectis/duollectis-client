package org.duollectis.client.utils.color;

import com.google.gson.JsonObject;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.duollectis.client.utils.ISerializable;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Color implements ISerializable{

    private static final int MAX_CHANNEL = 255;
    private static final int MIN_CHANNEL = 0;
    private static final int FULL_ALPHA = 255;

    public static final Color CLEAR = new Color(0, 0, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color LIGHT_GRAY = new Color(192, 192, 192, 255);
    public static final Color GRAY = new Color(128, 128, 128, 255);
    public static final Color DARK_GRAY = new Color(64, 64, 64, 255);
    public static final Color BLACK = new Color(0, 0, 0, 255);
    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color PINK = new Color(255, 175, 175, 255);
    public static final Color ORANGE = new Color(255, 200, 0, 255);
    public static final Color YELLOW = new Color(255, 255, 0, 255);
    public static final Color GREEN = new Color(0, 255, 0, 255);
    public static final Color MAGENTA = new Color(255, 0, 255, 255);
    public static final Color CYAN = new Color(0, 255, 255, 255);
    public static final Color BLUE = new Color(0, 0, 255, 255);

    public int r;
    public int g;
    public int b;
    public int a;

    public Color() {
        this(MAX_CHANNEL, MAX_CHANNEL, MAX_CHANNEL, FULL_ALPHA);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, FULL_ALPHA);
    }

    public Color(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        clamp();
    }

    public Color(float r, float g, float b, float a) {
        this(
            (int) (r * MAX_CHANNEL),
            (int) (g * MAX_CHANNEL),
            (int) (b * MAX_CHANNEL),
            (int) (a * MAX_CHANNEL)
        );
    }

    public Color(int packed) {
        this(
            unpackR(packed),
            unpackG(packed),
            unpackB(packed),
            unpackA(packed)
        );
    }

    public Color(Color source) {
        this(source.r, source.g, source.b, source.a);
    }

    public Color(Formatting formatting) {
        this(
            formatting.isColor() ? unpackR(formatting.getColorValue()) : MAX_CHANNEL,
            formatting.isColor() ? unpackG(formatting.getColorValue()) : MAX_CHANNEL,
            formatting.isColor() ? unpackB(formatting.getColorValue()) : MAX_CHANNEL,
            FULL_ALPHA
        );
    }

    public Color(TextColor textColor) {
        this(textColor.getRgb());
    }

    public Color(Style style) {
        this(style.getColor() != null ? style.getColor().getRgb() : pack(MAX_CHANNEL, MAX_CHANNEL, MAX_CHANNEL, FULL_ALPHA));
    }

    public static int pack(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int unpackR(int packed) {
        return (packed >> 16) & 0xFF;
    }

    public static int unpackG(int packed) {
        return (packed >> 8) & 0xFF;
    }

    public static int unpackB(int packed) {
        return packed & 0xFF;
    }

    public static int unpackA(int packed) {
        return (packed >> 24) & 0xFF;
    }

    public static Color fromHsv(double hue, double saturation, double value) {
        if (saturation <= 0.0) {
            int channel = (int) (value * MAX_CHANNEL);
            return new Color(channel, channel, channel, FULL_ALPHA);
        }

        double normalizedHue = hue >= 360.0 ? 0.0 : hue;
        double sector = normalizedHue / 60.0;
        int sectorIndex = (int) sector;
        double fraction = sector - sectorIndex;

        double p = value * (1.0 - saturation);
        double q = value * (1.0 - saturation * fraction);
        double t = value * (1.0 - saturation * (1.0 - fraction));

        double r = value;
        double g = p;
        double b = q;

        switch (sectorIndex) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }

        return new Color(
            (int) (r * MAX_CHANNEL),
            (int) (g * MAX_CHANNEL),
            (int) (b * MAX_CHANNEL),
            FULL_ALPHA
        );
    }

    public Color set(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        clamp();
        return this;
    }

    public Color set(Color source) {
        return set(source.r, source.g, source.b, source.a);
    }

    public Color r(int r) {
        this.r = r;
        clamp();
        return this;
    }

    public Color g(int g) {
        this.g = g;
        clamp();
        return this;
    }

    public Color b(int b) {
        this.b = b;
        clamp();
        return this;
    }

    public Color a(int a) {
        this.a = a;
        clamp();
        return this;
    }

    public boolean parse(String text) {
        String[] parts = text.split(",");

        if (parts.length != 3 && parts.length != 4) {
            return false;
        }

        try {
            int parsedR = Integer.parseInt(parts[0]);
            int parsedG = Integer.parseInt(parts[1]);
            int parsedB = Integer.parseInt(parts[2]);
            int parsedA = parts.length == 4 ? Integer.parseInt(parts[3]) : this.a;

            this.r = parsedR;
            this.g = parsedG;
            this.b = parsedB;
            this.a = parsedA;

            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public int getPacked() {
        return pack(r, g, b, a);
    }

    public TextColor toTextColor() {
        return TextColor.fromRgb(getPacked());
    }

    public Style toStyle() {
        return Style.EMPTY.withColor(toTextColor());
    }

    public Style applyTo(Style style) {
        return style.withColor(toTextColor());
    }

    public Vector3f toVec3f() {
        return new Vector3f(r / 255.0f, g / 255.0f, b / 255.0f);
    }

    public Vector4f toVec4f() {
        return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    @Override
    public JsonObject toJson() {
        JsonObject tag = new JsonObject();
        tag.addProperty("r", r);
        tag.addProperty("g", g);
        tag.addProperty("b", b);
        tag.addProperty("a", a);
        return tag;
    }

    @Override
    public void fromJson(JsonObject jsonObject) {
        r = jsonObject.get("r").getAsInt();
        g = jsonObject.get("g").getAsInt();
        b = jsonObject.get("b").getAsInt();
        a = jsonObject.get("a").getAsInt();

        clamp();
    }

    @Override
    public String toString() {
        return r + " " + g + " " + b + " " + a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Color other = (Color) o;
        return r == other.r && g == other.g && b == other.b && a == other.a;
    }

    @Override
    public int hashCode() {
        int result = r;
        result = 31 * result + g;
        result = 31 * result + b;
        result = 31 * result + a;
        return result;
    }

    private void clamp() {
        r = Math.clamp(r, MIN_CHANNEL, MAX_CHANNEL);
        g = Math.clamp(g, MIN_CHANNEL, MAX_CHANNEL);
        b = Math.clamp(b, MIN_CHANNEL, MAX_CHANNEL);
        a = Math.clamp(a, MIN_CHANNEL, MAX_CHANNEL);
    }
}
