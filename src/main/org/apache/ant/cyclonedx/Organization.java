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
import java.util.List;

import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.model.OrganizationalEntity;

/**
 * Organization appears as "manufacturer" or "supplier" of components
 * or the SBOM itself.
 *
 * <p>The CycloneDX specification supports more information for an
 * organization than this type currently exposes.</p>
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements or other
 * attributes are allowed.</p>
 */
public class Organization extends DataType {
    private String name;
    private List<String> urls = new ArrayList<>();

    /**
     * Sets the name of the organization.
     *
     * @param name organization name
     */
    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    /**
     * Adds an url of the organization.
     *
     * @param url organization URI
     */
    public void addConfiguredUrl(URLResource url) {
        checkChildrenAllowed();
        urls.add(url.getURL().toExternalForm());
    }

    /**
     * Translates this organisation to its CycloneDX counterpart.
     *
     * @return the CycloneDX version of this instance.
     */
    public OrganizationalEntity toOrganizationalEntity() {
        if (isReference()) {
            return getRef().toOrganizationalEntity();
        }
        dieOnCircularReference();
        OrganizationalEntity oe = new OrganizationalEntity();
        if (name != null) {
            oe.setName(name);
        }
        if (!urls.isEmpty()) {
            oe.setUrls(urls);
        }
        return oe;
    }

    /**
     * Creates a new instance from the CycloneDX counterpart.
     *
     * @return the instance created
     */
    static Organization from(OrganizationalEntity oe) {
        Organization o = new Organization();
        o.setName(oe.getName());
        List<String> urls = oe.getUrls();
        if (urls != null) {
            o.urls.addAll(urls);
        }
        return o;
    }

    /**
     * Perform the check for circular references and return the
     * referenced Organization.
     * @return <code>Organization</code>.
     */
    protected Organization getRef() {
        return getCheckedRef(Organization.class);
    }
}
