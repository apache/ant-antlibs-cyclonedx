/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ant.cyclonedx;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

/**
 * Helpers for the <code>EnumeratedAttribute</code>s wrapping enums in this package.
 */
public class EnumUtils {

    public static <E extends Enum<E>> String[] valuesPlus(
        Class<E> clazz,
        Function<E, String> alternativeProvider) {
        return Arrays.stream(clazz.getEnumConstants())
            .flatMap(e -> Stream.of(e.name(), alternativeProvider.apply(e)))
            .toArray(String[]::new);
    }

    public static <E extends Enum<E>> E valueOf(
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
