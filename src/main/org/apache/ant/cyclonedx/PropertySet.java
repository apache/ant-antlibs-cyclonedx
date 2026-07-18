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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

import org.cyclonedx.model.Property;

/**
 * A container for a collection of {@link Property}s.
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements are
 * allowed.</p>
 *
 * @since CycloneDX Antlib 0.2
 */
public class PropertySet extends DataType {
    private List<Property> properties = new ArrayList<>();

    /**
     * Adds a property to the set.
     *
     * @param property set property
     */
    public void addConfiguredProperty(Property property) {
        checkChildrenAllowed();
        if (property.getName() == null) {
            throw new BuildException("properties must have a name");
        }
        properties.add(property);
    }

    /**
     * Return the external references contained in this set.
     *
     * @return external references contained in this set
     */
    public Collection<Property> getProperties() {
        if (isReference()) {
            return getRef().getProperties();
        }
        dieOnCircularReference();
        return properties;
    }

    /**
     * Perform the check for circular references and return the
     * referenced PropertySet.
     * @return <code>PropertySet</code>.
     */
    protected PropertySet getRef() {
        return getCheckedRef(PropertySet.class);
    }
}
