package org.duollectis.client.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class Utils {

    public String nameToTitle(String name) {
        return Arrays
                .stream(name.split("-"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}
