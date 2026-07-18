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

import java.util.AbstractMap;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.BiFunction;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;

final class DateUtils {

    private static final String ENV_SOURCE_DATE_EPOCH = "SOURCE_DATE_EPOCH";

    /**
     * Consults the magic properties {@link MagicNames.TSTAMP_NOW_ISO} and {@link MagicNames.TSTAMP_NOW} as well as the
     * environment variable {@code SOURCE_DATE_EPOCH} for predefined values of "now" and falls back to {@code new
     * Date()} if neiter is set.
     *
     * <p>{@code SOURCE_DATE_EPOCH} takes precendence over {@link MagicNames.TSTAMP_NOW_ISO} which in turn takes precendence over {@link MagicNames.TSTAMP_NOW}.</p>
     *
     * @param project Project instance to use when looking up the magic properties.
     * @return a tuple of "now" and a boolean flag that indicates whether {@code SOURCE_DATE_EPOCH} has been set.
     * @since CycloneDX Antlib 0.2
     */
    // stolen from Ant 1.10.18
    static Map.Entry<Date, Boolean> getNow(Project project) {
        final String epoch = System.getenv(ENV_SOURCE_DATE_EPOCH);
        if (epoch != null) {
            // Value of SOURCE_DATE_EPOCH will be an integer, representing seconds.
            try {
                Date d = new Date(Long.parseLong(epoch) * 1000L);
                project.log("Honouring environment variable " + ENV_SOURCE_DATE_EPOCH + " which has been set to " + epoch);
                return new AbstractMap.SimpleImmutableEntry(d, true);
            } catch(NumberFormatException e) {
                // ignore
                project.log("Ignoring invalid value '" + epoch + "' for " + ENV_SOURCE_DATE_EPOCH
                            + " environment variable", Project.MSG_DEBUG);
            }
        }
        return new AbstractMap.SimpleImmutableEntry(getNowAsDate(project), false);
    }

    private static Date getNowAsDate(Project p) {
        Optional<Date> now = getNowAsDate(
            p,
            MagicNames.TSTAMP_NOW_ISO,
            s -> Date.from(Instant.parse(s)),
            (k, v) -> "magic property " + k + " ignored as '" + v + "' is not in valid ISO pattern"
        );
        if (now.isPresent()) {
            return now.get();
        }

        now = getNowAsDate(
            p,
            MagicNames.TSTAMP_NOW,
            s -> new Date(1000 * Long.parseLong(s)),
            (k, v) -> "magic property " + k + " ignored as " + v + " is not a valid number"
        );
        return now.orElseGet(Date::new);
    }

    private static Optional<Date> getNowAsDate(Project p, String propertyName, Function<String, Date> map,
                                               BiFunction<String, String, String> log) {
        String property = p.getProperty(propertyName);
        if (property != null && !property.isEmpty()) {
            try {
                return Optional.ofNullable(map.apply(property));
            } catch (Exception e) {
                p.log(log.apply(propertyName, property));
            }
        }
        return Optional.empty();
    }
}
