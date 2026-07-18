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

/**
 * An "external reference", i.e. a notable link for the component or
 * the SBOM itself.
 *
 * <p>External references must have a type and an URL. The
 * sepcification also supports optional comments and hashes which are
 * currently not supported. </p>
 *
 * <p>External references appear as children of {@link Component} or
 * {@code ExternalReferenceSet} elements.</p>
 */
public class ExternalReference {
    private String url;
    private String type;

    /**
     * Comparator for CycloneDX external references.
     *
     * <p>Sorts by type then by url.</p>
     *
     * @since CycloneDX Antlib 0.2
     */
    public static final Comparator<org.cyclonedx.model.ExternalReference> CycloneDxExternalReferenceComparator =
        Comparator.comparing(org.cyclonedx.model.ExternalReference::getType)
        .thenComparing(org.cyclonedx.model.ExternalReference::getUrl);

    /**
     * Set the URL (actually URI) of the external reference.
     *
     * <p>Required. This setter does not validate the value actually
     * follows the URI syntax.</p>
     *
     * @param url reference's URI
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Set the type of the external reference.
     *
     * <p>Required.</p>
     *
     * <p>Types defined by the specification or the enum names of <a
      href="https://github.com/CycloneDX/cyclonedx-core-java">CycloneDX
      Core (Java)</a>'s <a
      href="https://javadoc.io/static/org.cyclonedx/cyclonedx-core-java/13.0.0/org/cyclonedx/model/ExternalReference.Type.html">type
      enum</a> are accepted.</p>
     *
     * @param type reference's type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Maps the instance to a CycloneDX ExternalReference.
     *
     * @return CycloneDX version of this instance.
     */
    public org.cyclonedx.model.ExternalReference toCycloneDxExternalReference() {
        if (url == null) {
            throw new BuildException("external references must have an url");
        }
        if (type == null) {
            throw new BuildException("external references must have a type");
        }
        org.cyclonedx.model.ExternalReference.Type t = org.cyclonedx.model.ExternalReference.Type.fromString(type);
        if (t == null) {
            try {
                t = org.cyclonedx.model.ExternalReference.Type.valueOf(type);
            } catch (IllegalArgumentException ex) {
                throw new BuildException("external references type \"" + type + "\" is not supported");
            }
        }

        org.cyclonedx.model.ExternalReference r = new org.cyclonedx.model.ExternalReference();
        r.setUrl(url);
        r.setType(t);
        return r;
    }
}
