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

import org.cyclonedx.model.Component.Type;

/**
 * CycloneDX component type.
 *
 * <p>Accepts the enum constants like {@code LIBRARY} as well as the
 * lowercase version {@code library}. The values are directly provided
 * by CycloneDX Core's enum.</p>
 */
public class ComponentType extends EnumeratedAttribute {

    @Override
    public String[] getValues() {
        return EnumUtils.valuesPlus(Type.class, Type::getTypeName);
    }

    /**
     * Translates this instance to a {@link Type}.
     *
     * @throws BuildException if the value can not be translated.
     * @return CycloneDX type of this instance
     */
    public Type getType() {
        return EnumUtils.valueOf(Type.class, getValue(), Type::getTypeName);
    }

    /**
     * Maps a CycloneDX type.
     *
     * @param type CycloneDX type.
     * @return translated type
     */
    public  static ComponentType from(Type type) {
        ComponentType t = new ComponentType();
        t.setValue(type.name());
        return t;
    }
}
