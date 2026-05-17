package org.apache.ant.cyclonedx;

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
     * Set the URL (actually URI) of the external reference.
     *
     * <p>Required. This setter does not validate the value actually
     * follows the URI syntax.</p>
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
      href="https://javadoc.io/static/org.cyclonedx/cyclonedx-core-java/12.2.0/org/cyclonedx/model/ExternalReference.Type.html">type
      enum</a> are accepted.</p>
     */
    public void setType(String type) {
        this.type = type;
    }

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
