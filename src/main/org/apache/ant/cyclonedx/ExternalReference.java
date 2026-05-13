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
    private org.cyclonedx.model.ExternalReference.Type type;

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
     * <p>To make keep this easy to extend the <a
      href="https://javadoc.io/static/org.cyclonedx/cyclonedx-core-java/12.2.0/org/cyclonedx/model/ExternalReference.Type.html">type
      enum</a> of the <a
      href="https://github.com/CycloneDX/cyclonedx-core-java">CycloneDX
      Core (Java)</a> library is used directly. This also means you
      need to specify the type in uppercase rather than the lower case
      type defined by the standard.</p>
     */
    public void setType(org.cyclonedx.model.ExternalReference.Type type) {
        this.type = type;
    }

    org.cyclonedx.model.ExternalReference toCycloneDxExternalReference() {
        if (url == null) {
            throw new BuildException("external references must have an url");
        }
        if (type == null) {
            throw new BuildException("external references must have a type");
        }
        org.cyclonedx.model.ExternalReference r = new org.cyclonedx.model.ExternalReference();
        r.setUrl(url);
        r.setType(type);
        return r;
    }
}
