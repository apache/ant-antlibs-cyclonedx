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

import org.apache.tools.ant.types.DataType;

/**
 * A container for a collection of {@link ExternalReference}s.
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements are
 * allowed.</p>
 */
public class ExternalReferenceSet extends DataType {
    private List<org.cyclonedx.model.ExternalReference> externalReferences = new ArrayList<>();

    /**
     * Adds an external reference to this set.
     *
     * @param ref reference to add
     */
    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    /**
     * Return the external references contained in this set.
     *
     * @return external references contained in this set
     */
    public Collection<org.cyclonedx.model.ExternalReference> getExternalReferences() {
        if (isReference()) {
            return getRef().getExternalReferences();
        }
        dieOnCircularReference();
        return externalReferences;
    }

    /**
     * Perform the check for circular references and return the
     * referenced ExternalReferenceSet.
     * @return <code>ExternalReferenceSet</code>.
     */
    protected ExternalReferenceSet getRef() {
        return getCheckedRef(ExternalReferenceSet.class);
    }
}
