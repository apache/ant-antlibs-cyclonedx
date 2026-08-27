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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.model.Composition.Aggregate;

/**
 * CycloneDX composition's aggregate type.
 *
 * <p>Specifies an aggregate type that describes how complete a
 * relationship of a composition is.</p>
 *
 * <p>Accepts the enum constants like {@code COMPLETE} as well as the
 * lowercase version {@code complete}. The values are directly
 * provided by CycloneDX Core's enum.</p>
 *
 * @since CycloneDX Antlib 0.2
 */
public class CompositionAggregate extends EnumeratedAttribute {

    @Override
    public String[] getValues() {
        return EnumUtils.valuesPlus(Aggregate.class, Aggregate::getAggregateName);
    }

    /**
     * Translates this instance to a {@link Aggregate}.
     *
     * @return translated aggregate
     * @throws BuildException if the value can not be translated.
     */
    public Aggregate getAggregate() {
        return EnumUtils.valueOf(Aggregate.class, getValue(), Aggregate::getAggregateName);
    }
}
