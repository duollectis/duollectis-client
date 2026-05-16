package org.duollectis.client.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;


public class TextUtil {

    public static MutableText createGradientText(TextSettings settings, Color... colors) {
        return createGradientText(settings, List.of(colors));
    }

    public static MutableText createGradientText(TextSettings settings, List<Color> colors) {
        MutableText text = Text.empty();
        String string = settings.text;
        List<Color> gradient = ColorUtils.createGradient(string.length(), colors);
        for (int i = 0; i < string.length(); i++) {
            settings.color = gradient.get(i);
            settings.text = "" + string.charAt(i);
            text.append(createText(settings));
        }
        return text;
    }

    public static MutableText createText(TextSettings settings) {
        MutableText newtext = Text.empty();
        newtext.append(Text.of(settings.text));
        newtext.setStyle(Style.EMPTY
                .withColor(settings.color.getPacked())
                .withBold(settings.bold)
                .withItalic(settings.italic)
                .withUnderline(settings.underline)
                .withStrikethrough(settings.strikehold)
                .withObfuscated(settings.obfuscated)
                .withInsertion(settings.insertion)
                .withParent(settings.parentStyle)
        );
        return newtext;
    }

    public static class TextSettings {

        public String text = "";
        public Color color = new Color(255, 255, 255);
        public boolean bold = false;
        public boolean italic = false;
        public boolean underline = false;
        public boolean strikehold = false;
        public boolean obfuscated = false;
        public String insertion = "";
        public Style parentStyle = Style.EMPTY;


        public TextSettings(String text) {
            this.text = text;
        }

        public TextSettings(String text, Color color, boolean bold) {
            this.color = color;
            this.text = text;
            this.bold = bold;
        }

        public TextSettings(String text, boolean bold) {
            this.text = text;
            this.bold = bold;
        }

        public TextSettings(String text, Color color, boolean bold, boolean italic, boolean underline, boolean strikehold, boolean obfuscated) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikehold = strikehold;
            this.obfuscated = obfuscated;
        }

    }
}
