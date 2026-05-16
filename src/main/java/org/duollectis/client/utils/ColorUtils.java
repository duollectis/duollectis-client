package org.duollectis.client.utils;


import java.util.ArrayList;
import java.util.List;

public class ColorUtils {

    public static List<Color> createGradient(int length, List<Color> colors) {
        List<Color> gradient = new ArrayList<>();

        if (colors.isEmpty()) {
            for (int i = 0; i < length; i++)
                gradient.add(new Color(255, 255, 255, 255));
            return gradient;
        }

        if (colors.size() == 1) {
            for (int i = 0; i < length; i++)
                gradient.add(colors.getFirst());
            return gradient;
        }

        if (length <= 1) {
            gradient.add(colors.getFirst());
            return gradient;
        }

        int gradientLength = length / (colors.size() - 1);
        while (gradientLength * (colors.size() - 1) < (length))
            gradientLength++;

        for (int i = 0; i < colors.size() - 1; i++) {
            Color clr1 = colors.get(i);
            Color clr2 = colors.get(i + 1);

            double r1 = clr1.r;
            double g1 = clr1.g;
            double b1 = clr1.b;
            double a1 = clr1.a;

            double rStep = (r1 - clr2.r) / (gradientLength - 1);
            double gStep = (g1 - clr2.g) / (gradientLength - 1);
            double bStep = (b1 - clr2.b) / (gradientLength - 1);
            double aStep = (a1 - clr2.a) / (gradientLength - 1);

            for (int ii = 0; ii < gradientLength; ii++) {
                gradient.add(new Color(
                    (int) Math.round(r1),
                    (int) Math.round(g1),
                    (int) Math.round(b1),
                    (int) Math.round(a1)
                ));

                r1 -= rStep;
                g1 -= gStep;
                b1 -= bStep;
                a1 -= aStep;
            }
        }

        while (gradient.size() > length)
            gradient.removeLast();

        return gradient;
    }

	public static Color getColor(int color) {
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return new Color(r, g, b, a);
    }
}
