package org.duollectis.client.utils.color;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ColorUtils {

    public List<Color> createGradient(int length, List<Color> colors) {
        List<Color> gradient = new ArrayList<>();

        if (colors.isEmpty()) {
            for (int i = 0; i < length; i++) {
                gradient.add(new Color(255, 255, 255, 255));
            }

            return gradient;
        }

        if (colors.size() == 1) {
            for (int i = 0; i < length; i++) {
                gradient.add(colors.getFirst());
            }

            return gradient;
        }

        if (length <= 1) {
            gradient.add(colors.getFirst());
            return gradient;
        }

        int segmentLength = length / (colors.size() - 1);

        while (segmentLength * (colors.size() - 1) < length) {
            segmentLength++;
        }

        for (int i = 0; i < colors.size() - 1; i++) {
            Color from = colors.get(i);
            Color to = colors.get(i + 1);

            double r = from.r;
            double g = from.g;
            double b = from.b;
            double a = from.a;

            double rStep = (r - to.r) / (segmentLength - 1);
            double gStep = (g - to.g) / (segmentLength - 1);
            double bStep = (b - to.b) / (segmentLength - 1);
            double aStep = (a - to.a) / (segmentLength - 1);

            for (int j = 0; j < segmentLength; j++) {
                gradient.add(new Color(
                    (int) Math.round(r),
                    (int) Math.round(g),
                    (int) Math.round(b),
                    (int) Math.round(a)
                ));

                r -= rStep;
                g -= gStep;
                b -= bStep;
                a -= aStep;
            }
        }

        while (gradient.size() > length) {
            gradient.removeLast();
        }

        return gradient;
    }
}
