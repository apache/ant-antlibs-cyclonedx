package org.apache.ant.cyclonedx;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

class EnumUtils {
    static <E extends Enum<E>> String[] valuesPlus(
        Class<E> clazz,
        Function<E, String> alternativeProvider) {
        return Arrays.stream(clazz.getEnumConstants())
            .flatMap(e -> Stream.of(e.name(), alternativeProvider.apply(e)))
            .toArray(String[]::new);
    }

    static <E extends Enum<E>> E valueOf(
        Class<E> clazz,
        String value,
        Function<E, String> alternativeProvider) throws BuildException {
        if (value == null) {
            throw new NullPointerException("value must not be null");
        }
        E en = Arrays.stream(clazz.getEnumConstants())
            .filter(e -> value.equals(alternativeProvider.apply(e)))
            .findFirst()
            .orElse(null);
        if (en != null) {
            return en;
        }
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException ex) {
            throw new BuildException(value + " is not a supported " + clazz.getName());
        }
    }
}
