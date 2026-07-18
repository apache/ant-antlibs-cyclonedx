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

import java.util.Comparator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.resources.URLResource;

/**
 * A license element to be attached to a component.
 *
 * <p>Licenses are required to have an id or a name attribute. Due to
 * Ant's own usage of the {@code id} attribute the SBOM id of the
 * license is called {@code licenseId} here.</p>
 *
 * <p>The CycloneDX specification supports more information for a
 * license than this type currently exposes.</p>
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements or other
 * attributes are allowed.</p>
 */
public class License extends DataType {
    private String id;
    private String name;
    private String url;

    /**
     * Comparator for CycloneDX license.
     *
     * <p>Sorts by id (if present) and falls back to sorting by name
     * (if present).</p>
     *
     * @since CycloneDX Antlib 0.2
     */
    public static final Comparator<org.cyclonedx.model.License> CycloneDxLicenseComparator =
        Comparator.comparing(org.cyclonedx.model.License::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(org.cyclonedx.model.License::getName, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Sets the {@code id} of the license.
     *
     * <p>Must be a valid <a href="https://spdx.org/licenses/">SPDX</a>
     * identifier. This library doesn't enforce the SPDX identifier
     * but the CycloneDX Core library does.</p>
     *
     * @param id SPDX id of license
     */
    public void setLicenseId(String id) {
        checkAttributesAllowed();
        this.id = id;
    }

    /**
     * Sets the name of the license.
     *
     * @param name license name
     */
    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    /**
     * Sets the url of the license.
     *
     * <p>Even though this is a nested element of the license element,
     * at most one child is allowed.</p>
     *
     * @param url url of license
     */
    public void addConfiguredUrl(URLResource url) {
        checkChildrenAllowed();
        if (this.url != null) {
            throw new BuildException("only one URL is allowed in license");
        }
        this.url = url.getURL().toExternalForm();
    }

    /**
     * Maps the license to its CycloneDX counterpart.
     *
     * @return CycloneDX version of this instance
     */
    public org.cyclonedx.model.License toCycloneDxLicense() {
        if (isReference()) {
            return getRef().toCycloneDxLicense();
        }
        dieOnCircularReference();
        if (name == null && id == null) {
            throw new BuildException("license name or licenseId is required");
        }
        org.cyclonedx.model.License l = new org.cyclonedx.model.License();
        if (name != null) {
            l.setName(name);
        }
        if (id != null) {
            l.setId(id);
        }
        if (url != null) {
            l.setUrl(url);
        }
        return l;
    }

    /**
     * Perform the check for circular references and return the
     * referenced License.
     * @return <code>License</code>.
     */
    protected License getRef() {
        return getCheckedRef(License.class);
    }
}
